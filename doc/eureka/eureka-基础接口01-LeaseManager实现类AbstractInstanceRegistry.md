AbstractInstanceRegistry是实现了InstanceRegistry接口，看名字就知道是一个注册表类，存储着实例信息。   
InstanceRegistry接口的父接口就是LeaseManager接口，先看下LeaseManager接口的方法  

```java
/**
 * Handles all registry requests from eureka clients.
 * 处理所有的eureka客户端注册请求
 *
 * <p>
 * Primary operations that are performed are the
 * <em>Registers</em>, <em>Renewals</em>, <em>Cancels</em>, <em>Expirations</em>, and <em>Status Changes</em>. The
 * registry also stores only the delta operations
 * 主要测操作是注册（Registers）、续约（Renewals）、取消（Cancels）、过期（Expirations）和状态变更（Status Change）.
 * 注册表还仅存储增量操作
 * </p>
 *
 * @author Karthik Ranganathan
 *
 */
public abstract class AbstractInstanceRegistry implements InstanceRegistry {
    private static final Logger logger = LoggerFactory.getLogger(AbstractInstanceRegistry.class);

    private static final String[] EMPTY_STR_ARRAY = new String[0];
    
    //所有的实例都在这个map中进行存储，外层map的key为AppName，内层map的key为InstanceId
    private final ConcurrentHashMap<String, Map<String, Lease<InstanceInfo>>> registry
            = new ConcurrentHashMap<String, Map<String, Lease<InstanceInfo>>>();
    
    protected Map<String, RemoteRegionRegistry> regionNameVSRemoteRegistry = new HashMap<String, RemoteRegionRegistry>();
    protected final ConcurrentMap<String, InstanceStatus> overriddenInstanceStatusMap = CacheBuilder
            .newBuilder().initialCapacity(500)
            .expireAfterAccess(1, TimeUnit.HOURS)
            .<String, InstanceStatus>build().asMap();

    // CircularQueues here for debugging/statistics purposes only
    private final CircularQueue<Pair<Long, String>> recentRegisteredQueue;
    private final CircularQueue<Pair<Long, String>> recentCanceledQueue;
    private ConcurrentLinkedQueue<RecentlyChangedItem> recentlyChangedQueue = new ConcurrentLinkedQueue<RecentlyChangedItem>();

    private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private final Lock read = readWriteLock.readLock();
    private final Lock write = readWriteLock.writeLock();
    protected final Object lock = new Object();

    private Timer deltaRetentionTimer = new Timer("Eureka-DeltaRetentionTimer", true);
    private Timer evictionTimer = new Timer("Eureka-EvictionTimer", true);
    //记录上一分钟续约的请求数，用于判断是否进入自我保护模式
    private final MeasuredRate renewsLastMin;

    private final AtomicReference<EvictionTask> evictionTaskRef = new AtomicReference<EvictionTask>();

    protected String[] allKnownRemoteRegions = EMPTY_STR_ARRAY;
    //每分钟续约个数阀值，用于判断是否开启保护模式
    protected volatile int numberOfRenewsPerMinThreshold;
    protected volatile int expectedNumberOfClientsSendingRenews;

    protected final EurekaServerConfig serverConfig;
    protected final EurekaClientConfig clientConfig;
    protected final ServerCodecs serverCodecs;
    protected volatile ResponseCache responseCache;


    /**
     * Registers a new instance with a given duration.
     * 使用给定的租期来注册一个实例信息。
     * isReplication代表是不是其他的eureka节点发来的复制信息，还是客户端直接发来的注册信息
     */
    public void register(InstanceInfo registrant, int leaseDuration, boolean isReplication) {
        try {
            read.lock();
            
            //根据AppName参数从注册表里查找Map信息
            Map<String, Lease<InstanceInfo>> gMap = registry.get(registrant.getAppName());
            //注册监控计数器加1
            REGISTER.increment(isReplication);
            if (gMap == null) {
                //没有改AppName的数据直接new一个
                final ConcurrentHashMap<String, Lease<InstanceInfo>> gNewMap = new ConcurrentHashMap<String, Lease<InstanceInfo>>();
                gMap = registry.putIfAbsent(registrant.getAppName(), gNewMap);
                if (gMap == null) {
                    gMap = gNewMap;
                }
            }
            //根据instanceId从registry内层map中查找租约
            Lease<InstanceInfo> existingLease = gMap.get(registrant.getId());
            // Retain the last dirty timestamp without overwriting it, if there is already a lease
            //如果该instanceId已经在注册表中有了，保留老的实例lastDirtyTimestamp
            //应用在关闭时，cancel请求为正常发送，再次启动就会又来注册了，这是就会发现注册表中已经有了该实例
            if (existingLease != null && (existingLease.getHolder() != null)) {
                Long existingLastDirtyTimestamp = existingLease.getHolder().getLastDirtyTimestamp();
                Long registrationLastDirtyTimestamp = registrant.getLastDirtyTimestamp();
                logger.debug("Existing lease found (existing={}, provided={}", existingLastDirtyTimestamp, registrationLastDirtyTimestamp);

                // this is a > instead of a >= because if the timestamps are equal, we still take the remote transmitted
                // InstanceInfo instead of the server local copy.
                //如果老的实例的lastDirtyTimestamp比新注册的实例的此参数还靠后，那么就用老的实例，抛弃新注册的实例。
                //什么情况下会发生这种情况呢？比如客户端启动时，第一次注册请求出问题了，又发送了第二次，
                //然后服务端先成功处理了第二次的请求数据，然后又收到了第一次的请求数据。
                if (existingLastDirtyTimestamp > registrationLastDirtyTimestamp) {
                    logger.warn("There is an existing lease and the existing lease's dirty timestamp {} is greater" +
                            " than the one that is being registered {}", existingLastDirtyTimestamp, registrationLastDirtyTimestamp);
                    logger.warn("Using the existing instanceInfo instead of the new instanceInfo as the registrant");
                    registrant = existingLease.getHolder();
                }
            } else {
                // The lease does not exist and hence it is a new registration
                // 注册表中不存在该InstanceId的租约，所以是一个新的注册
                synchronized (lock) {
                    if (this.expectedNumberOfClientsSendingRenews > 0) {
                        // Since the client wants to register it, increase the number of clients sending renews
                        // 这个参数很重要，用于判断是否开启保护模式时使用。有一个新的客户端第一次注册，就会加一，可以理解为需要有多少个客户端会发续约请求。
                        this.expectedNumberOfClientsSendingRenews = this.expectedNumberOfClientsSendingRenews + 1;
                        //更新每分钟续约数的阀值，这个阀值也是用于计算是否开启保护模式的，这个先不细看，后面再看。
                        updateRenewsPerMinThreshold();
                    }
                }
                logger.debug("No previous lease information found; it is new registration");
            }
            
            //创建一个租约包装InstanceInfo，Lease表示一个租约，记录了注册时间、有效期等，后面再细看
            Lease<InstanceInfo> lease = new Lease<InstanceInfo>(registrant, leaseDuration);
            if (existingLease != null) {
                //如果instanceId对应的租约已经有了，使用老的租约的启动时间
                lease.setServiceUpTimestamp(existingLease.getServiceUpTimestamp());
            }
            gMap.put(registrant.getId(), lease);
            synchronized (recentRegisteredQueue) {
                //记录下统计数据
                recentRegisteredQueue.add(new Pair<Long, String>(
                        System.currentTimeMillis(),
                        registrant.getAppName() + "(" + registrant.getId() + ")"));
            }
            
            // This is where the initial state transfer of overridden status happens
            // 正常注册overriddenStatus是UNKNOWN，什么情况下会发生呢？
            if (!InstanceStatus.UNKNOWN.equals(registrant.getOverriddenStatus())) {
                logger.debug("Found overridden status {} for instance {}. Checking to see if needs to be add to the "
                        + "overrides", registrant.getOverriddenStatus(), registrant.getId());
                if (!overriddenInstanceStatusMap.containsKey(registrant.getId())) {
                    logger.info("Not found overridden id {} and hence adding it", registrant.getId());
                    overriddenInstanceStatusMap.put(registrant.getId(), registrant.getOverriddenStatus());
                }
            }
            InstanceStatus overriddenStatusFromMap = overriddenInstanceStatusMap.get(registrant.getId());
            if (overriddenStatusFromMap != null) {
                logger.info("Storing overridden status {} from map", overriddenStatusFromMap);
                registrant.setOverriddenStatus(overriddenStatusFromMap);
            }

            // Set the status based on the overridden status rules
            InstanceStatus overriddenInstanceStatus = getOverriddenInstanceStatus(registrant, existingLease, isReplication);
            registrant.setStatusWithoutDirty(overriddenInstanceStatus);

            // If the lease is registered with UP status, set lease service up timestamp
            // 如果租约注册的状态是UP，则设置租约的服务上线时间戳（serviceUpTimestamp）
            if (InstanceStatus.UP.equals(registrant.getStatus())) {
                lease.serviceUp();
            }
            
            //记录统计数据
            registrant.setActionType(ActionType.ADDED);
            recentlyChangedQueue.add(new RecentlyChangedItem(lease));
            registrant.setLastUpdatedTimestamp();
            
            //将该实例对应的缓存失效
            invalidateCache(registrant.getAppName(), registrant.getVIPAddress(), registrant.getSecureVipAddress());
            logger.info("Registered instance {}/{} with status {} (replication={})",
                    registrant.getAppName(), registrant.getId(), registrant.getStatus(), isReplication);
        } finally {
            read.unlock();
        }
    }

    /**
     * Cancels the registration of an instance.
     * 取消一个实例的注册。
     *
     * <p>
     * This is normally invoked by a client when it shuts down informing the
     * server to remove the instance from traffic.
     * </p>
     * 通常是在客户端关闭时调用，通知服务端移除该实例的流量。
     *
     */
    @Override
    public boolean cancel(String appName, String id, boolean isReplication) {
        return internalCancel(appName, id, isReplication);
    }

    /**
     * {@link #cancel(String, String, boolean)} method is overridden by {@link PeerAwareInstanceRegistry}, so each
     * cancel request is replicated to the peers. This is however not desired for expires which would be counted
     * in the remote peers as valid cancellations, so self preservation mode would not kick-in.
     */
    protected boolean internalCancel(String appName, String id, boolean isReplication) {
        try {
            read.lock();
            //cancel监控计数器加1
            CANCEL.increment(isReplication);
            
            //根据appName和id查找对应的实例信息
            Map<String, Lease<InstanceInfo>> gMap = registry.get(appName);
            Lease<InstanceInfo> leaseToCancel = null;
            if (gMap != null) {
                leaseToCancel = gMap.remove(id);
            }
            synchronized (recentCanceledQueue) {
                //记录统计数据
                recentCanceledQueue.add(new Pair<Long, String>(System.currentTimeMillis(), appName + "(" + id + ")"));
            }
            InstanceStatus instanceStatus = overriddenInstanceStatusMap.remove(id);
            if (instanceStatus != null) {
                logger.debug("Removed instance id {} from the overridden map which has value {}", id, instanceStatus.name());
            }
            if (leaseToCancel == null) {
                //没有找到对应的实例，记录统计信息打印日志
                CANCEL_NOT_FOUND.increment(isReplication);
                logger.warn("DS: Registry: cancel failed because Lease is not registered for: {}/{}", appName, id);
                return false;
            } else {
                //将租约取消掉，就是记录下租约里的驱逐时间戳evictionTimestamp
                leaseToCancel.cancel();
                InstanceInfo instanceInfo = leaseToCancel.getHolder();
                String vip = null;
                String svip = null;
                if (instanceInfo != null) {
                    //记录统计数据，记录最后一次更新时间
                    instanceInfo.setActionType(ActionType.DELETED);
                    recentlyChangedQueue.add(new RecentlyChangedItem(leaseToCancel));
                    instanceInfo.setLastUpdatedTimestamp();
                    vip = instanceInfo.getVIPAddress();
                    svip = instanceInfo.getSecureVipAddress();
                }
                
                //将缓存失效
                invalidateCache(appName, vip, svip);
                logger.info("Cancelled instance {}/{} (replication={})", appName, id, isReplication);
                return true;
            }
        } finally {
            read.unlock();
        }
    }


    /**
     * Marks the given instance of the given app name as renewed, and also marks whether it originated from
     * replication.
     * 续订与appName和id关联的实例的租约。也就是续约。
     */
    public boolean renew(String appName, String id, boolean isReplication) {
        //监控计数器加1
        RENEW.increment(isReplication);
        
        //找到相应的实例租约
        Map<String, Lease<InstanceInfo>> gMap = registry.get(appName);
        Lease<InstanceInfo> leaseToRenew = null;
        if (gMap != null) {
            leaseToRenew = gMap.get(id);
        }
        if (leaseToRenew == null) {
            //相应的实例租约未找到，监控计数器记录下
            RENEW_NOT_FOUND.increment(isReplication);
            logger.warn("DS: Registry: lease doesn't exist, registering resource: {} - {}", appName, id);
            return false;
        } else {
            InstanceInfo instanceInfo = leaseToRenew.getHolder();
            if (instanceInfo != null) {
                // touchASGCache(instanceInfo.getASGName());
                //???
                InstanceStatus overriddenInstanceStatus = this.getOverriddenInstanceStatus(
                        instanceInfo, leaseToRenew, isReplication);
                if (overriddenInstanceStatus == InstanceStatus.UNKNOWN) {
                    logger.info("Instance status UNKNOWN possibly due to deleted override for instance {}"
                            + "; re-register required", instanceInfo.getId());
                    RENEW_NOT_FOUND.increment(isReplication);
                    return false;
                }
                if (!instanceInfo.getStatus().equals(overriddenInstanceStatus)) {
                    logger.info(
                            "The instance status {} is different from overridden instance status {} for instance {}. "
                                    + "Hence setting the status to overridden status", instanceInfo.getStatus().name(),
                            instanceInfo.getOverriddenStatus().name(),
                            instanceInfo.getId());
                    instanceInfo.setStatusWithoutDirty(overriddenInstanceStatus);

                }
            }
            
            //上一分钟续约数加1
            renewsLastMin.increment();
            
            //lease的续约操作就是将上次更新时间戳lastUpdateTimestamp再加一个租期
            leaseToRenew.renew();
            return true;
        }
    }


    /**
     * Evicts everything in the instance registry that has expired, if expiry is enabled.
     * 如果启用了到期设置，驱逐所有实例注册表中到期的实例。
     */
    @Override
    public void evict() {
        evict(0l);
    }

    public void evict(long additionalLeaseMs) {
        logger.debug("Running the evict task");

        if (!isLeaseExpirationEnabled()) {
            //租约的到期不可用
            //如果开启了自我保护模式的配置，并且上一分钟续租的个数（renewsLastMin）小于每分钟续约的阀值（numberOfRenewsPerMinThreshold），就不进行驱逐操作。
            logger.debug("DS: lease expiration is currently disabled.");
            return;
        }

        // We collect first all expired items, to evict them in random order. For large eviction sets,
        // if we do not that, we might wipe out whole apps before self preservation kicks in. By randomizing it,
        // the impact should be evenly distributed across all applications.
        //首先收集所有的过期项，以随机的方式驱逐。
        //如果有大量的驱逐数据，如果不这么做，可能在进入自我保护模式之前就驱逐了所有apps
        //通过随机化处理，影响应该会分散在所有的应用中。
        List<Lease<InstanceInfo>> expiredLeases = new ArrayList<>();
        for (Entry<String, Map<String, Lease<InstanceInfo>>> groupEntry : registry.entrySet()) {
            Map<String, Lease<InstanceInfo>> leaseMap = groupEntry.getValue();
            if (leaseMap != null) {
                for (Entry<String, Lease<InstanceInfo>> leaseEntry : leaseMap.entrySet()) {
                    Lease<InstanceInfo> lease = leaseEntry.getValue();
                    if (lease.isExpired(additionalLeaseMs) && lease.getHolder() != null) {
                        expiredLeases.add(lease);
                    }
                }
            }
        }

        // To compensate for GC pauses or drifting local time, we need to use current registry size as a base for
        // triggering self-preservation. Without that we would wipe out full registry.
        //为了补偿GC的暂停或本地时间的漂移，我们需要使用当前注册表大小作为触发自我保存的基础
        // 否则将擦除注册表所有的数据
        int registrySize = (int) getLocalRegistrySize();
        //阀值=注册表总大小*配置的一个比例，默认是0.85，就是说一次驱逐最多能驱逐总数的25%数量
        int registrySizeThreshold = (int) (registrySize * serverConfig.getRenewalPercentThreshold());
        //计算驱逐上线个数
        int evictionLimit = registrySize - registrySizeThreshold;

        int toEvict = Math.min(expiredLeases.size(), evictionLimit);
        if (toEvict > 0) {
            logger.info("Evicting {} items (expired={}, evictionLimit={})", toEvict, expiredLeases.size(), evictionLimit);

            Random random = new Random(System.currentTimeMillis());
            for (int i = 0; i < toEvict; i++) {
                // Pick a random item (Knuth shuffle algorithm)
                int next = i + random.nextInt(expiredLeases.size() - i);
                Collections.swap(expiredLeases, i, next);
                Lease<InstanceInfo> lease = expiredLeases.get(i);

                String appName = lease.getHolder().getAppName();
                String id = lease.getHolder().getId();
                EXPIRED.increment();
                logger.warn("DS: Registry: expired lease for {}/{}", appName, id);
                internalCancel(appName, id, false);
            }
        }
    }
}
```