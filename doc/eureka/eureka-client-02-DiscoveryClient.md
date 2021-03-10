DiscoveryClient是eureka客户端的核心组件，其实现了接口EurekaClient，说明具有了EurekaClient的相关功能，但是不止这些功能，先看下类注释。  
```java
/**
 * The class that is instrumental for interactions with <tt>Eureka Server</tt>.
 * 用于和Eureka服务端进行交互的类。
 * 
 * <p>
 * <tt>Eureka Client</tt> is responsible for a) <em>Registering</em> the
 * instance with <tt>Eureka Server</tt> b) <em>Renewal</em>of the lease with
 * <tt>Eureka Server</tt> c) <em>Cancellation</em> of the lease from
 * <tt>Eureka Server</tt> during shutdown
 * <p>
 * d) <em>Querying</em> the list of services/instances registered with
 * <tt>Eureka Server</tt>
 * <p>
 *
 * EurekaClient 负责将instance信息注册到 EurekaServer、
 * 与服务端进行租约续期、
 * 关闭服务时取消租约、
 * 查询服务端注册的服务和实例信息。
 * 
 * <p>
 * <tt>Eureka Client</tt> needs a configured list of <tt>Eureka Server</tt>
 * {@link java.net.URL}s to talk to.These {@link java.net.URL}s are typically amazon elastic eips
 * which do not change. All of the functions defined above fail-over to other
 * {@link java.net.URL}s specified in the list in the case of failure.
 * </p>
 * EurekaClient需要配置一组EurekaServer的url以进行交互。
 * 如果发生故障，上面定义的所有方法将故障转移到列表中指定的其它url。
 *
 */
@Singleton
public class DiscoveryClient implements EurekaClient {
    
}
```
DiscoveryClient大致有以下功能点：
1. EurekaClient接口中定义的InstanceInfo信息获取的相关功能。
2. EurekaClient接口中定义的本地元数据的获取相关功能。
3. EurekaClient接口中定义的健康检查（healthcheck）相关功能。
4. 租约管理的功能（注册、续约、取消）

这个类代码一起看的话比较多，下面拆分几个部分分开了解。   

##租约管理相关功能
租约管理的相关功能用的都是都是eurekaTransport.registrationClient
```java
@Singleton
public class DiscoveryClient implements EurekaClient {
    //封装了网络通讯的操作
    private final EurekaTransport eurekaTransport;
    //用于定时发送续约心跳请求
    private final ThreadPoolExecutor heartbeatExecutor;
    //记录最后一次成功发送续约心跳的时间戳
    private volatile long lastSuccessfulHeartbeatTimestamp = -1;
    
    /**
     * Register with the eureka service by making the appropriate REST call.
     * 通过进行适当的REST调用来注册eureka服务
     */
    boolean register() throws Throwable {
        logger.info(PREFIX + "{}: registering service...", appPathIdentifier);
        EurekaHttpResponse<Void> httpResponse;
        try {
            httpResponse = eurekaTransport.registrationClient.register(instanceInfo);
        } catch (Exception e) {
            logger.warn(PREFIX + "{} - registration failed {}", appPathIdentifier, e.getMessage(), e);
            throw e;
        }
        if (logger.isInfoEnabled()) {
            logger.info(PREFIX + "{} - registration status: {}", appPathIdentifier, httpResponse.getStatusCode());
        }
        return httpResponse.getStatusCode() == Status.NO_CONTENT.getStatusCode();
    }

    /**
     * Renew with the eureka service by making the appropriate REST call
     * 续约。
     * 如果续约相应的responseCode为404，表示服务端没有该实例的租约，客户端就在进行register注册。
     */
    boolean renew() {
        EurekaHttpResponse<InstanceInfo> httpResponse;
        try {
            httpResponse = eurekaTransport.registrationClient.sendHeartBeat(instanceInfo.getAppName(), instanceInfo.getId(), instanceInfo, null);
            logger.debug(PREFIX + "{} - Heartbeat status: {}", appPathIdentifier, httpResponse.getStatusCode());
            if (httpResponse.getStatusCode() == Status.NOT_FOUND.getStatusCode()) {
                REREGISTER_COUNTER.increment();
                logger.info(PREFIX + "{} - Re-registering apps/{}", appPathIdentifier, instanceInfo.getAppName());
                long timestamp = instanceInfo.setIsDirtyWithTime();
                boolean success = register();
                if (success) {
                    instanceInfo.unsetIsDirty(timestamp);
                }
                return success;
            }
            return httpResponse.getStatusCode() == Status.OK.getStatusCode();
        } catch (Throwable e) {
            logger.error(PREFIX + "{} - was unable to send heartbeat!", appPathIdentifier, e);
            return false;
        }
    }

    /**
     * unregister w/ the eureka service.
     * 注销操作
     */
    void unregister() {
        // It can be null if shouldRegisterWithEureka == false
        if(eurekaTransport != null && eurekaTransport.registrationClient != null) {
            try {
                logger.info("Unregistering ...");
                EurekaHttpResponse<Void> httpResponse = eurekaTransport.registrationClient.cancel(instanceInfo.getAppName(), instanceInfo.getId());
                logger.info(PREFIX + "{} - deregister  status: {}", appPathIdentifier, httpResponse.getStatusCode());
            } catch (Exception e) {
                logger.error(PREFIX + "{} - de-registration failed{}", appPathIdentifier, e.getMessage(), e);
            }
        }
    }

    /**
     * The heartbeat task that renews the lease in the given intervals.
     * 在给定的时间间隔定期发送更新租约的心跳
     */
    private class HeartbeatThread implements Runnable {

        public void run() {
            if (renew()) {
                lastSuccessfulHeartbeatTimestamp = System.currentTimeMillis();
            }
        }
    }
}
```

##应用、实例信息的获取相关
网络交互式都是使用的eurekaTransport.queryClient
```java
@Singleton
public class DiscoveryClient implements EurekaClient {

    //保存本从服务端获取到的Applications数据
    private final AtomicReference<Applications> localRegionApps = new AtomicReference<Applications>();
    //保存着从其它region获取下来的Applications数据
    private volatile Map<String, Applications> remoteRegionVsApps = new ConcurrentHashMap<>();

    //事件监听器集合，扩展使用，eureka client是作为一个事件产生方的，目前有的事件是缓存更新时间、InstanceInfo状态变更时间
    //像ribbon就需要用事件监听器来感知这些数据的变更
    private final CopyOnWriteArraySet<EurekaEventListener> eventListeners = new CopyOnWriteArraySet<>();

    //一个计数器，用于防止老的线程将注册表信息更新为老的版本
    private final AtomicLong fetchRegistryGeneration;
    
    //clientConfig.fetch-remote-regions-registry配置的远端region名称集合字符串，逗号分隔多个
    private final AtomicReference<String> remoteRegionsToFetch;
    //将remoteRegionsToFetch解析为regionName数组
    private final AtomicReference<String[]> remoteRegionsRef;
    //根据InstanceInfo获取region，如果不是部署在亚马逊云上的话，返回的都是null
    private final InstanceRegionChecker instanceRegionChecker;

    /**
     * Fetches the registry information.
     * 注册表信息的获取
     * 
     * 全量或者增量的从服务端获取Applications
     */
    private boolean fetchRegistry(boolean forceFullRegistryFetch) {
        Stopwatch tracer = FETCH_REGISTRY_TIMER.start();

        try {
            //如果增量获取被禁止，或者第一次启动，都进行全量获取
            //这里的逻辑和服务端从远端region获取的逻辑基本上一致
            Applications applications = getApplications();

            if (clientConfig.shouldDisableDelta()
                    || (!Strings.isNullOrEmpty(clientConfig.getRegistryRefreshSingleVipAddress()))
                    || forceFullRegistryFetch
                    || (applications == null)
                    || (applications.getRegisteredApplications().size() == 0)
                    || (applications.getVersion() == -1)) //Client application does not have latest library supporting delta
            {
                //全量获取
                getAndStoreFullRegistry();
            } else {
                //增量获取
                getAndUpdateDelta(applications);
            }
            //记录hash值
            applications.setAppsHashCode(applications.getReconcileHashCode());
            //日志记录一共获取了多少个实例信息
            logTotalInstances();
        } catch (Throwable e) {
            logger.error(PREFIX + "{} - was unable to refresh its cache! status = {}", appPathIdentifier, e.getMessage(), e);
            return false;
        } finally {
            if (tracer != null) {
                tracer.stop();
            }
        }

        // Notify about cache refresh before updating the instance remote status
        //广播一个CacheRefreshedEvent事件
        onCacheRefreshed();

        // Update remote status based on refreshed data held in the cache
        // 将从服务端获取下来的本服务的状态更新到本地缓存
        updateInstanceRemoteStatus();

        // registry was fetched successfully, so return true
        return true;
    }

    /**
     * Gets the full registry information from the eureka server and stores it locally.
     *
     * 从eureka服务端获取全量的注册表信息并保存到本地
     * 
     */
    private void getAndStoreFullRegistry() throws Throwable {
        long currentUpdateGeneration = fetchRegistryGeneration.get();

        logger.info("Getting all instance registry info from the eureka server");

        Applications apps = null;
        //发送请求时会将remoteRegion信息带过去，服务端就会判断为是否获取其它region的数据，这个服务端源码有体现
        EurekaHttpResponse<Applications> httpResponse = clientConfig.getRegistryRefreshSingleVipAddress() == null
                ? eurekaTransport.queryClient.getApplications(remoteRegionsRef.get())
                : eurekaTransport.queryClient.getVip(clientConfig.getRegistryRefreshSingleVipAddress(), remoteRegionsRef.get());
        if (httpResponse.getStatusCode() == Status.OK.getStatusCode()) {
            apps = httpResponse.getEntity();
        }
        logger.info("The response status is {}", httpResponse.getStatusCode());

        if (apps == null) {
            logger.error("The application is null for some reason. Not storing this information");
        } else if (fetchRegistryGeneration.compareAndSet(currentUpdateGeneration, currentUpdateGeneration + 1)) {
            //将结果集打乱后放到localRegionApps保存
            localRegionApps.set(this.filterAndShuffle(apps));
            logger.debug("Got full registry with apps hashcode {}", apps.getAppsHashCode());
        } else {
            logger.warn("Not updating applications as another thread is updating it already");
        }
    }

    /**
     * Get the delta registry information from the eureka server and update it locally.
     *
     * 从eureka服务端获取增联过的数据并更新到本地。
     */
    private void getAndUpdateDelta(Applications applications) throws Throwable {
        long currentUpdateGeneration = fetchRegistryGeneration.get();

        Applications delta = null;
        EurekaHttpResponse<Applications> httpResponse = eurekaTransport.queryClient.getDelta(remoteRegionsRef.get());
        if (httpResponse.getStatusCode() == Status.OK.getStatusCode()) {
            delta = httpResponse.getEntity();
        }

        if (delta == null) {
            logger.warn("The server does not allow the delta revision to be applied because it is not safe. "
                    + "Hence got the full registry.");
            //如果获取到的数据为空，进行一次全量获取。
            getAndStoreFullRegistry();
        } else if (fetchRegistryGeneration.compareAndSet(currentUpdateGeneration, currentUpdateGeneration + 1)) {
            logger.debug("Got delta update with apps hashcode {}", delta.getAppsHashCode());
            String reconcileHashCode = "";
            if (fetchRegistryUpdateLock.tryLock()) {
                try {
                    //将增量数据更新到本地Applications中（这些逻辑和RemoteRegionRegistry中的一样，这里就不看了）
                    updateDelta(delta);
                    reconcileHashCode = getReconcileHashCode(applications);
                } finally {
                    fetchRegistryUpdateLock.unlock();
                }
            } else {
                logger.warn("Cannot acquire update lock, aborting getAndUpdateDelta");
            }
            // There is a diff in number of instances for some reason
            if (!reconcileHashCode.equals(delta.getAppsHashCode()) || clientConfig.shouldLogDeltaDiff()) {
                //更新过增量数据后，本地的Applications计算的hash值和服务端的不一样，直接进行一次全量更新
                //这些逻辑和RemoteRegionRegistry中的一样，这里就不看了
                reconcileAndLogDifference(delta, reconcileHashCode);  // this makes a remoteCall
            }
        } else {
            logger.warn("Not updating application delta as another thread is updating it already");
            logger.debug("Ignoring delta update with apps hashcode {}, as another thread is updating it already", delta.getAppsHashCode());
        }
    }


    /**
     * Updates the delta information fetches from the eureka server into the
     * local cache.
     * 将从服务端获取到的增量数据更新到本地缓存中
     * 只是看下remoteRegionVsApps的使用，不同ActionType是如何更新到本地的和RemoteRegionRegistry中的逻辑一样
     */
    private void updateDelta(Applications delta) {
        int deltaCount = 0;
        for (Application app : delta.getRegisteredApplications()) {
            for (InstanceInfo instance : app.getInstances()) {
                Applications applications = getApplications();
                String instanceRegion = instanceRegionChecker.getInstanceRegion(instance);
                if (!instanceRegionChecker.isLocalRegion(instanceRegion)) {
                    Applications remoteApps = remoteRegionVsApps.get(instanceRegion);
                    if (null == remoteApps) {
                        remoteApps = new Applications();
                        //如果服务端获取到的InstanceInfo不是本客户端所在的region话，保存在remoteRegionVsApps缓存中
                        remoteRegionVsApps.put(instanceRegion, remoteApps);
                    }
                    applications = remoteApps;
                }

                ++deltaCount;
                if (ActionType.ADDED.equals(instance.getActionType())) {
                    Application existingApp = applications.getRegisteredApplications(instance.getAppName());
                    if (existingApp == null) {
                        applications.addApplication(app);
                    }
                    logger.debug("Added instance {} to the existing apps in region {}", instance.getId(), instanceRegion);
                    applications.getRegisteredApplications(instance.getAppName()).addInstance(instance);
                } else if (ActionType.MODIFIED.equals(instance.getActionType())) {
                    Application existingApp = applications.getRegisteredApplications(instance.getAppName());
                    if (existingApp == null) {
                        applications.addApplication(app);
                    }
                    logger.debug("Modified instance {} to the existing apps ", instance.getId());

                    applications.getRegisteredApplications(instance.getAppName()).addInstance(instance);

                } else if (ActionType.DELETED.equals(instance.getActionType())) {
                    Application existingApp = applications.getRegisteredApplications(instance.getAppName());
                    if (existingApp != null) {
                        logger.debug("Deleted instance {} to the existing apps ", instance.getId());
                        existingApp.removeInstance(instance);
                        /*
                         * We find all instance list from application(The status of instance status is not only the status is UP but also other status)
                         * if instance list is empty, we remove the application.
                         */
                        if (existingApp.getInstancesAsIsFromEureka().isEmpty()) {
                            applications.removeApplication(existingApp);
                        }
                    }
                }
            }
        }
        logger.debug("The total number of instances fetched by the delta processor : {}", deltaCount);

        getApplications().setVersion(delta.getVersion());
        getApplications().shuffleInstances(clientConfig.shouldFilterOnlyUpInstances());

        for (Applications applications : remoteRegionVsApps.values()) {
            applications.setVersion(delta.getVersion());
            applications.shuffleInstances(clientConfig.shouldFilterOnlyUpInstances());
        }
    }

    /**
     * The task that fetches the registry information at specified intervals.
     * 定时任务去获取服务端注册表信息更新到本地
     */
    class CacheRefreshThread implements Runnable {
        public void run() {
            refreshRegistry();
        }
    }

    @VisibleForTesting
    void refreshRegistry() {
        try {
            //配置文件中remoteRegions和内存中已加载的是否有差异
            boolean remoteRegionsModified = false;
            // This makes sure that a dynamic change to remote regions to fetch is honored.
            String latestRemoteRegions = clientConfig.fetchRegistryForRemoteRegions();
            if (null != latestRemoteRegions) {
                String currentRemoteRegions = remoteRegionsToFetch.get();
                //配置文件中的和当前内存中已加载好的不一样
                if (!latestRemoteRegions.equals(currentRemoteRegions)) {
                    // Both remoteRegionsToFetch and AzToRegionMapper.regionsToFetch need to be in sync
                    synchronized (instanceRegionChecker.getAzToRegionMapper()) {
                        if (remoteRegionsToFetch.compareAndSet(currentRemoteRegions, latestRemoteRegions)) {
                            //红心初始化remoteRegions相关参数
                            String[] remoteRegions = latestRemoteRegions.split(",");
                            remoteRegionsRef.set(remoteRegions);
                            instanceRegionChecker.getAzToRegionMapper().setRegionsToFetch(remoteRegions);
                            remoteRegionsModified = true;
                        } else {
                            logger.info("Remote regions to fetch modified concurrently," +
                                    " ignoring change from {} to {}", currentRemoteRegions, latestRemoteRegions);
                        }
                    }
                } else {
                    // Just refresh mapping to reflect any DNS/Property change
                    instanceRegionChecker.getAzToRegionMapper().refreshMapping();
                }
            }

            //调用fetchRegistry方法进行获取信息
            //remoteRegionsModified为true表示发现新增了remote region，就强制做一次全量获取
            boolean success = fetchRegistry(remoteRegionsModified);
            if (success) {
                //成功后更新相关参数
                registrySize = localRegionApps.get().size();
                lastSuccessfulRegistryFetchTimestamp = System.currentTimeMillis();
            }

        } catch (Throwable e) {
            logger.error("Cannot fetch registry from server", e);
        }
    }
    
    
    /**
     * Invoked every time the local registry cache is refreshed (whether changes have
     * been detected or not).
     * 
     * 本地注册表信息刷新后每次都调用该方法，不管是否有改变
     * 
     * Subclasses may override this method to implement custom behavior if needed.
     * 子类可以覆盖该方法去自定义行为
     */
    protected void onCacheRefreshed() {
        fireEvent(new CacheRefreshedEvent());
    }

    /**
     * Send the given event on the EventBus if one is available
     * 将事件发送到总线
     */
    protected void fireEvent(final EurekaEvent event) {
        for (EurekaEventListener listener : eventListeners) {
            try {
                listener.onEvent(event);
            } catch (Exception e) {
                logger.info("Event {} throw an exception for listener {}", event, listener, e.getMessage());
            }
        }
    }

    //fetchRegistry从服务端获取过注册表信息后，会根据服务端的数据更新本client所在的InstanceInfo的状态信息
    private synchronized void updateInstanceRemoteStatus() {
        // Determine this instance's status for this app and set to UNKNOWN if not found
        InstanceInfo.InstanceStatus currentRemoteInstanceStatus = null;
        if (instanceInfo.getAppName() != null) {
            Application app = getApplication(instanceInfo.getAppName());
            if (app != null) {
                InstanceInfo remoteInstanceInfo = app.getByInstanceId(instanceInfo.getId());
                if (remoteInstanceInfo != null) {
                    currentRemoteInstanceStatus = remoteInstanceInfo.getStatus();
                }
            }
        }
        if (currentRemoteInstanceStatus == null) {
            currentRemoteInstanceStatus = InstanceInfo.InstanceStatus.UNKNOWN;
        }

        // Notify if status changed
        if (lastRemoteInstanceStatus != currentRemoteInstanceStatus) {
            //状态变更后发送事件通知
            onRemoteStatusChanged(lastRemoteInstanceStatus, currentRemoteInstanceStatus);
            lastRemoteInstanceStatus = currentRemoteInstanceStatus;
        }
    }

    //发送状态变更事件
    protected void onRemoteStatusChanged(InstanceInfo.InstanceStatus oldStatus, InstanceInfo.InstanceStatus newStatus) {
        fireEvent(new StatusChangeEvent(oldStatus, newStatus));
    }

    //从本地Applications中根据appName获取Application
    public Application getApplication(String appName) {
        return getApplications().getRegisteredApplications(appName);
    }

    
    //获取Applications
    public Applications getApplications() {
        return localRegionApps.get();
    }
    
    //从指定region中获取Applications信息
    public Applications getApplicationsForARegion(@Nullable String region) {
        if (instanceRegionChecker.isLocalRegion(region)) {
            return localRegionApps.get();
        } else {
            return remoteRegionVsApps.get(region);
        }
    }

}

//检查InstanceInfo所在的region
public class InstanceRegionChecker {
    private static Logger logger = LoggerFactory.getLogger(InstanceRegionChecker.class);

    private final AzToRegionMapper azToRegionMapper;
    private final String localRegion;

    InstanceRegionChecker(AzToRegionMapper azToRegionMapper, String localRegion) {
        this.azToRegionMapper = azToRegionMapper;
        this.localRegion = localRegion;
    }

    @Nullable
    public String getInstanceRegion(InstanceInfo instanceInfo) {
        if (instanceInfo.getDataCenterInfo() == null || instanceInfo.getDataCenterInfo().getName() == null) {
            logger.warn("Cannot get region for instance id:{}, app:{} as dataCenterInfo is null. Returning local:{} by default",
                    instanceInfo.getId(), instanceInfo.getAppName(), localRegion);

            return localRegion;
        }
        if (DataCenterInfo.Name.Amazon.equals(instanceInfo.getDataCenterInfo().getName())) {
            AmazonInfo amazonInfo = (AmazonInfo) instanceInfo.getDataCenterInfo();
            Map<String, String> metadata = amazonInfo.getMetadata();
            String availabilityZone = metadata.get(AmazonInfo.MetaDataKey.availabilityZone.getName());
            if (null != availabilityZone) {
                return azToRegionMapper.getRegionForAvailabilityZone(availabilityZone);
            }
        }

        return null;
    }

    public boolean isLocalRegion(@Nullable String instanceRegion) {
        return null == instanceRegion || instanceRegion.equals(localRegion); // no region == local
    }

    public String getLocalRegion() {
        return localRegion;
    }
}

//默认的DataCenterInfo是在这里定义的
public abstract class AbstractInstanceConfig implements EurekaInstanceConfig {
    private DataCenterInfo info = new DataCenterInfo() {
            @Override
            public Name getName() {
                return Name.MyOwn;
            }
        };
}
```

##初始化及关闭的逻辑梳理
```java
@Singleton
public class DiscoveryClient implements EurekaClient {
    
    @Inject
    DiscoveryClient(ApplicationInfoManager applicationInfoManager, EurekaClientConfig config, AbstractDiscoveryClientOptionalArgs args,
                    Provider<BackupRegistry> backupRegistryProvider, EndpointRandomizer endpointRandomizer) {
        if (args != null) {
            this.healthCheckHandlerProvider = args.healthCheckHandlerProvider;
            this.healthCheckCallbackProvider = args.healthCheckCallbackProvider;
            this.eventListeners.addAll(args.getEventListeners());
            this.preRegistrationHandler = args.preRegistrationHandler;
        } else {
            this.healthCheckCallbackProvider = null;
            this.healthCheckHandlerProvider = null;
            this.preRegistrationHandler = null;
        }

        this.applicationInfoManager = applicationInfoManager;
        InstanceInfo myInfo = applicationInfoManager.getInfo();

        clientConfig = config;
        staticClientConfig = clientConfig;
        transportConfig = config.getTransportConfig();
        instanceInfo = myInfo;
        if (myInfo != null) {
            appPathIdentifier = instanceInfo.getAppName() + "/" + instanceInfo.getId();
        } else {
            logger.warn("Setting instanceInfo to a passed in null value");
        }

        this.backupRegistryProvider = backupRegistryProvider;
        this.endpointRandomizer = endpointRandomizer;
        this.urlRandomizer = new EndpointUtils.InstanceInfoBasedUrlRandomizer(instanceInfo);
        localRegionApps.set(new Applications());

        //初始化获取注册表的版本控制器
        fetchRegistryGeneration = new AtomicLong(0);

        //初始化remoteRegions相关数据
        remoteRegionsToFetch = new AtomicReference<String>(clientConfig.fetchRegistryForRemoteRegions());
        remoteRegionsRef = new AtomicReference<>(remoteRegionsToFetch.get() == null ? null : remoteRegionsToFetch.get().split(","));

        
        //如果需要获取注册表数据，就初始化registryStalenessMonitor，用于监控上一次成功获取注册表到现在有多久
        if (config.shouldFetchRegistry()) {
            this.registryStalenessMonitor = new ThresholdLevelsMetric(this, METRIC_REGISTRY_PREFIX + "lastUpdateSec_", new long[]{15L, 30L, 60L, 120L, 240L, 480L});
        } else {
            this.registryStalenessMonitor = ThresholdLevelsMetric.NO_OP_METRIC;
        }

        //如果需要将自己注册到eureka server，初始化heartbeatStalenessMonitor，用于监控上次心跳时间
        if (config.shouldRegisterWithEureka()) {
            this.heartbeatStalenessMonitor = new ThresholdLevelsMetric(this, METRIC_REGISTRATION_PREFIX + "lastHeartbeatSec_", new long[]{15L, 30L, 60L, 120L, 240L, 480L});
        } else {
            this.heartbeatStalenessMonitor = ThresholdLevelsMetric.NO_OP_METRIC;
        }

        logger.info("Initializing Eureka in region {}", clientConfig.getRegion());

        if (!config.shouldRegisterWithEureka() && !config.shouldFetchRegistry()) {
            logger.info("Client configured to neither register nor query for data.");
            //如果客户端配置的不去获取注册表信息也不讲自己注册到server，就不初始化心跳、缓存更新的定时任务
            scheduler = null;
            heartbeatExecutor = null;
            cacheRefreshExecutor = null;
            eurekaTransport = null;
            instanceRegionChecker = new InstanceRegionChecker(new PropertyBasedAzToRegionMapper(config), clientConfig.getRegion());

            // This is a bit of hack to allow for existing code using DiscoveryManager.getInstance()
            // to work with DI'd DiscoveryClient
            //提供了一种单例模式可以访问DiscovireyClient
            DiscoveryManager.getInstance().setDiscoveryClient(this);
            DiscoveryManager.getInstance().setEurekaClientConfig(config);

            initTimestampMs = System.currentTimeMillis();
            logger.info("Discovery Client initialized at timestamp {} with initial instances count: {}",
                    initTimestampMs, this.getApplications().size());

            return;  // no need to setup up an network tasks and we are done
        }

        try {
            // default size of 2 - 1 each for heartbeat and cacheRefresh
            //该线程池用于驱动心跳和缓存刷新的动作
            scheduler = Executors.newScheduledThreadPool(2,
                    new ThreadFactoryBuilder()
                            .setNameFormat("DiscoveryClient-%d")
                            .setDaemon(true)
                            .build());

            //心跳线程池
            heartbeatExecutor = new ThreadPoolExecutor(
                    1, clientConfig.getHeartbeatExecutorThreadPoolSize(), 0, TimeUnit.SECONDS,
                    new SynchronousQueue<Runnable>(),
                    new ThreadFactoryBuilder()
                            .setNameFormat("DiscoveryClient-HeartbeatExecutor-%d")
                            .setDaemon(true)
                            .build()
            );  // use direct handoff

            //缓存刷新线程池
            cacheRefreshExecutor = new ThreadPoolExecutor(
                    1, clientConfig.getCacheRefreshExecutorThreadPoolSize(), 0, TimeUnit.SECONDS,
                    new SynchronousQueue<Runnable>(),
                    new ThreadFactoryBuilder()
                            .setNameFormat("DiscoveryClient-CacheRefreshExecutor-%d")
                            .setDaemon(true)
                            .build()
            );  // use direct handoff

            //EurekaTransport封装了eureka的网络通讯操作
            eurekaTransport = new EurekaTransport();
            //初始化EurekaTransport
            scheduleServerEndpointTask(eurekaTransport, args);

            //AWS相关的类初始化
            AzToRegionMapper azToRegionMapper;
            if (clientConfig.shouldUseDnsForFetchingServiceUrls()) {
                azToRegionMapper = new DNSBasedAzToRegionMapper(clientConfig);
            } else {
                azToRegionMapper = new PropertyBasedAzToRegionMapper(clientConfig);
            }
            if (null != remoteRegionsToFetch.get()) {
                azToRegionMapper.setRegionsToFetch(remoteRegionsToFetch.get().split(","));
            }
            instanceRegionChecker = new InstanceRegionChecker(azToRegionMapper, clientConfig.getRegion());
        } catch (Throwable e) {
            throw new RuntimeException("Failed to initialize DiscoveryClient!", e);
        }

        if (clientConfig.shouldFetchRegistry() && !fetchRegistry(false)) {
            //如果说获取注册表失败，从一个后备方案获取，eureka自身实现的都是返回了null
            fetchRegistryFromBackup();
        }

        // call and execute the pre registration handler before all background tasks (inc registration) is started
        if (this.preRegistrationHandler != null) {
            //如果有自定义的扩展类，会在这一步执行
            this.preRegistrationHandler.beforeRegistration();
        }

        if (clientConfig.shouldRegisterWithEureka() && clientConfig.shouldEnforceRegistrationAtInit()) {
            try {
                //启动时直接进行注册操作
                if (!register() ) {
                    throw new IllegalStateException("Registration error at startup. Invalid server response.");
                }
            } catch (Throwable th) {
                logger.error("Registration error at startup: {}", th.getMessage());
                throw new IllegalStateException(th);
            }
        }

        // finally, init the schedule tasks (e.g. cluster resolvers, heartbeat, instanceInfo replicator, fetch
        //初始化定时任务
        initScheduledTasks();

        try {
            //Netflix-servo 监控
            Monitors.registerObject(this);
        } catch (Throwable e) {
            logger.warn("Cannot register timers", e);
        }

        // This is a bit of hack to allow for existing code using DiscoveryManager.getInstance()
        // to work with DI'd DiscoveryClient
        //提供了一种单例模式可以访问DiscovireyClient
        DiscoveryManager.getInstance().setDiscoveryClient(this);
        DiscoveryManager.getInstance().setEurekaClientConfig(config);

        initTimestampMs = System.currentTimeMillis();
        logger.info("Discovery Client initialized at timestamp {} with initial instances count: {}",
                initTimestampMs, this.getApplications().size());
    }


    /**
     * 初始化所有的定时任务
     */
    private void initScheduledTasks() {
        if (clientConfig.shouldFetchRegistry()) {
            //需要从服务端获取注册表信息，定时执行缓存刷新任务
            int registryFetchIntervalSeconds = clientConfig.getRegistryFetchIntervalSeconds();
            int expBackOffBound = clientConfig.getCacheRefreshExecutorExponentialBackOffBound();
            scheduler.schedule(
                    new TimedSupervisorTask(
                            "cacheRefresh",
                            scheduler,
                            cacheRefreshExecutor,
                            registryFetchIntervalSeconds,
                            TimeUnit.SECONDS,
                            expBackOffBound,
                            new CacheRefreshThread()
                    ),
                    registryFetchIntervalSeconds, TimeUnit.SECONDS);
        }

        if (clientConfig.shouldRegisterWithEureka()) {
            //需要将自己注册到服务端，定时执行心跳续约任务
            int renewalIntervalInSecs = instanceInfo.getLeaseInfo().getRenewalIntervalInSecs();
            int expBackOffBound = clientConfig.getHeartbeatExecutorExponentialBackOffBound();
            logger.info("Starting heartbeat executor: " + "renew interval is: {}", renewalIntervalInSecs);

            // Heartbeat timer
            scheduler.schedule(
                    new TimedSupervisorTask(
                            "heartbeat",
                            scheduler,
                            heartbeatExecutor,
                            renewalIntervalInSecs,
                            TimeUnit.SECONDS,
                            expBackOffBound,
                            new HeartbeatThread()
                    ),
                    renewalIntervalInSecs, TimeUnit.SECONDS);

            // InstanceInfo replicator
            //InstanceInfoReplicator的作用是定时查看自己的InstanceInfo信息，如果有变化，发送给服务端
            instanceInfoReplicator = new InstanceInfoReplicator(
                    this,
                    instanceInfo,
                    clientConfig.getInstanceInfoReplicationIntervalSeconds(),
                    2); // burstSize

            //定义一个StatusChangeListener，处理InstanceInfo.status变化的事件，和InstanceInfoReplicator联合使用
            statusChangeListener = new ApplicationInfoManager.StatusChangeListener() {
                @Override
                public String getId() {
                    return "statusChangeListener";
                }

                @Override
                public void notify(StatusChangeEvent statusChangeEvent) {
                    if (InstanceStatus.DOWN == statusChangeEvent.getStatus() ||
                            InstanceStatus.DOWN == statusChangeEvent.getPreviousStatus()) {
                        // log at warn level if DOWN was involved
                        logger.warn("Saw local status change event {}", statusChangeEvent);
                    } else {
                        logger.info("Saw local status change event {}", statusChangeEvent);
                    }
                    instanceInfoReplicator.onDemandUpdate();
                }
            };

            if (clientConfig.shouldOnDemandUpdateStatusChange()) {
                applicationInfoManager.registerStatusChangeListener(statusChangeListener);
            }

            instanceInfoReplicator.start(clientConfig.getInitialInstanceInfoReplicationIntervalSeconds());
        } else {
            logger.info("Not registering with Eureka server per configuration");
        }
    }


    /**
     * Shuts down Eureka Client. Also sends a deregistration request to the
     * eureka server.
     */
    @PreDestroy
    @Override
    public synchronized void shutdown() {
        if (isShutdown.compareAndSet(false, true)) {
            logger.info("Shutting down DiscoveryClient ...");

            //取消注册statusChangeListener
            if (statusChangeListener != null && applicationInfoManager != null) {
                applicationInfoManager.unregisterStatusChangeListener(statusChangeListener.getId());
            }

            //关闭所有的定时任务
            cancelScheduledTasks();

            // If APPINFO was registered
            if (applicationInfoManager != null
                    && clientConfig.shouldRegisterWithEureka()
                    && clientConfig.shouldUnregisterOnShutdown()) {
                //将状态设置为下线，然后从服务端注销掉
                applicationInfoManager.setInstanceStatus(InstanceStatus.DOWN);
                unregister();
            }

            //关闭eurekaTransport中的网络键连接
            if (eurekaTransport != null) {
                eurekaTransport.shutdown();
            }

            //netflix-servo 监控关闭
            heartbeatStalenessMonitor.shutdown();
            registryStalenessMonitor.shutdown();

            logger.info("Completed shut down of DiscoveryClient");
        }
    }
}
```

总结：
DiscoveryClient基本上包括了客户端所需要的全部功能，从整个生命周期的顺序来整理。
1. 启动服务
    - ClientConfig 客户端的配置信息
    - InstanceInfo 客户端自身的实例信息
    - localRegionsApps 服务端的注册信息保存在本地的数据结构
    - remoteRegions 其它regions的参数解析
    - 各种netflix-servo的监控数据
    - heartbeatExecutor 初始化心跳更新定时任务
    - cacheRefreshExecutor 初始化缓存刷新定时任务
    - EurekaTransport 网络通信的封装类初始化
    - instanceRegionChecker 获取一个InstanceInfo所在的region的帮助类
    - fetchRegistry() 从服务端进行一次全量的注册表信息获取
    - preRegistrationHandler 在启动定时任务之前，执行一个扩展类
    - register() 将自身信息注册到server端
    - StatusChangeListener 注册状态变化监听器
2. 运行期
    - HeartbeatExecutor-renew 心跳线程定时执行续约请求
    - CacheRefreshExecutor-refreshRegistry 刷新缓存定时任务定时增量或者全量的更新注册表信息
    - StatusChangeListener InstanceInfo信息有变化是，重新向server发起register请求
3. 关闭服务
    - 关闭定时任务（心跳、刷新缓存）
    - 自身InstanceInfo状态改为DOWN
    - 向server端发送注销请求
    - EurekaTransport关闭
    - netflix-servo监控关闭

更新注册表流程
1. 刚启动时

尽管通过配置文件配置了fetch-remote-regions-registry，如果不是部署在亚马逊云上的话，remote regions的注册表信息是可以拉去下来的，但是不会保存到remoteRegionVsApps中，根据指定region来获取Applications时，是获取不到数据的。
客户端只需要配置其它region的名字就好，不用配置具体的网络请求地址，将region名称发给server端即可。server端会和不同的region保持数据同步的。