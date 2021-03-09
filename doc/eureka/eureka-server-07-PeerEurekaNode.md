在LeaseManager接口的子类PeerAwareInstanceRegistryImpl中，处理完自身的租约管理后，会通过PeerEurekaNodes将数据复制给相同region的eureka-server的对等节点。  
PeerEurekaNodes是一个PeerEurekaNode的集合管理类，一个PeerEurekaNode代表一个节点。    

##PeerEurekaNodes
```java
/**
 * Helper class to manage lifecycle of a collection of {@link PeerEurekaNode}s.
 *
 * 是一个帮助类，管理一个集合的PeerEurekaNode的声明周期。
 */
@Singleton
public class PeerEurekaNodes {

    private static final Logger logger = LoggerFactory.getLogger(PeerEurekaNodes.class);

    protected final PeerAwareInstanceRegistry registry;
    protected final EurekaServerConfig serverConfig;
    protected final EurekaClientConfig clientConfig;
    protected final ServerCodecs serverCodecs;
    private final ApplicationInfoManager applicationInfoManager;

    //存储PeerEurekaNode的数据集合
    private volatile List<PeerEurekaNode> peerEurekaNodes = Collections.emptyList();
    private volatile Set<String> peerEurekaNodeUrls = Collections.emptySet();

    //定时任务，用于定时查看新增或删除的节点信息
    private ScheduledExecutorService taskExecutor;

    @Inject
    public PeerEurekaNodes(
            PeerAwareInstanceRegistry registry,
            EurekaServerConfig serverConfig,
            EurekaClientConfig clientConfig,
            ServerCodecs serverCodecs,
            ApplicationInfoManager applicationInfoManager) {
        this.registry = registry;
        this.serverConfig = serverConfig;
        this.clientConfig = clientConfig;
        this.serverCodecs = serverCodecs;
        this.applicationInfoManager = applicationInfoManager;
    }

    public List<PeerEurekaNode> getPeerNodesView() {
        return Collections.unmodifiableList(peerEurekaNodes);
    }

    public List<PeerEurekaNode> getPeerEurekaNodes() {
        return peerEurekaNodes;
    }
    
    //启动方法，用于启动定时任务，更新PeerEurekaNode信息
    public void start() {
        taskExecutor = Executors.newSingleThreadScheduledExecutor(
                new ThreadFactory() {
                    @Override
                    public Thread newThread(Runnable r) {
                        Thread thread = new Thread(r, "Eureka-PeerNodesUpdater");
                        thread.setDaemon(true);
                        return thread;
                    }
                }
        );
        try {
            //启动时，直接进行一次更新操作
            updatePeerEurekaNodes(resolvePeerUrls());
            Runnable peersUpdateTask = new Runnable() {
                @Override
                public void run() {
                    try {
                        updatePeerEurekaNodes(resolvePeerUrls());
                    } catch (Throwable e) {
                        logger.error("Cannot update the replica Nodes", e);
                    }

                }
            };
            taskExecutor.scheduleWithFixedDelay(
                    peersUpdateTask,
                    serverConfig.getPeerEurekaNodesUpdateIntervalMs(),
                    serverConfig.getPeerEurekaNodesUpdateIntervalMs(),
                    TimeUnit.MILLISECONDS
            );
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        for (PeerEurekaNode node : peerEurekaNodes) {
            logger.info("Replica node URL:  {}", node.getServiceUrl());
        }
    }

    //关闭时逐个将各个node关闭
    public void shutdown() {
        taskExecutor.shutdown();
        List<PeerEurekaNode> toRemove = this.peerEurekaNodes;

        this.peerEurekaNodes = Collections.emptyList();
        this.peerEurekaNodeUrls = Collections.emptySet();

        for (PeerEurekaNode node : toRemove) {
            node.shutDown();
        }
    }

    /**
     * Resolve peer URLs.
     *
     * 解析节点的url(相同region的)
     */
    protected List<String> resolvePeerUrls() {
        InstanceInfo myInfo = applicationInfoManager.getInfo();
        //根据配置文件里自己的region获取所有的配置文件中相同region的availabilityZone
        //就是解析这三个配置：client.region/client.region.availability-zones/client.region.service-url
        //结果就是获取相同region的所有可用zone的url
        String zone = InstanceInfo.getZone(clientConfig.getAvailabilityZones(clientConfig.getRegion()), myInfo);
        List<String> replicaUrls = EndpointUtils
                .getDiscoveryServiceUrls(clientConfig, zone, new EndpointUtils.InstanceInfoBasedUrlRandomizer(myInfo));

        //根据isThisMyUrl排除掉自身的地址
        int idx = 0;
        while (idx < replicaUrls.size()) {
            if (isThisMyUrl(replicaUrls.get(idx))) {
                replicaUrls.remove(idx);
            } else {
                idx++;
            }
        }
        return replicaUrls;
    }

    /**
     * Given new set of replica URLs, destroy {@link PeerEurekaNode}s no longer available, and
     * create new ones.
     *
     * 更新PeerEurekaNode的数据（新增、删除）
     */
    protected void updatePeerEurekaNodes(List<String> newPeerUrls) {
        if (newPeerUrls.isEmpty()) {
            logger.warn("The replica size seems to be empty. Check the route 53 DNS Registry");
            return;
        }

        Set<String> toShutdown = new HashSet<>(peerEurekaNodeUrls);
        toShutdown.removeAll(newPeerUrls);
        Set<String> toAdd = new HashSet<>(newPeerUrls);
        toAdd.removeAll(peerEurekaNodeUrls);

        if (toShutdown.isEmpty() && toAdd.isEmpty()) { // No change
            return;
        }

        // Remove peers no long available
        List<PeerEurekaNode> newNodeList = new ArrayList<>(peerEurekaNodes);

        if (!toShutdown.isEmpty()) {
            logger.info("Removing no longer available peer nodes {}", toShutdown);
            int i = 0;
            while (i < newNodeList.size()) {
                PeerEurekaNode eurekaNode = newNodeList.get(i);
                if (toShutdown.contains(eurekaNode.getServiceUrl())) {
                    newNodeList.remove(i);
                    eurekaNode.shutDown();
                } else {
                    i++;
                }
            }
        }

        // Add new peers
        if (!toAdd.isEmpty()) {
            logger.info("Adding new peer nodes {}", toAdd);
            for (String peerUrl : toAdd) {
                newNodeList.add(createPeerEurekaNode(peerUrl));
            }
        }

        this.peerEurekaNodes = newNodeList;
        this.peerEurekaNodeUrls = new HashSet<>(newPeerUrls);
    }

    protected PeerEurekaNode createPeerEurekaNode(String peerEurekaNodeUrl) {
        HttpReplicationClient replicationClient = JerseyReplicationClient.createReplicationClient(serverConfig, serverCodecs, peerEurekaNodeUrl);
        String targetHost = hostFromUrl(peerEurekaNodeUrl);
        if (targetHost == null) {
            targetHost = "host";
        }
        return new PeerEurekaNode(registry, targetHost, peerEurekaNodeUrl, replicationClient, serverConfig);
    }


    /**
     * Checks if the given service url contains the current host which is trying
     * to replicate. Only after the EIP binding is done the host has a chance to
     * identify itself in the list of replica nodes and needs to take itself out
     * of replication traffic.
     *
     * 判断该url是否是自己的。
     */
    public boolean isThisMyUrl(String url) {
        final String myUrlConfigured = serverConfig.getMyUrl();
        if (myUrlConfigured != null) {
            return myUrlConfigured.equals(url);
        }
        return isInstanceURL(url, applicationInfoManager.getInfo());
    }
    
    /**
     * Checks if the given service url matches the supplied instance
     */
    public boolean isInstanceURL(String url, InstanceInfo instance) {
        String hostName = hostFromUrl(url);
        String myInfoComparator = instance.getHostName();
        if (clientConfig.getTransportConfig().applicationsResolverUseIp()) {
            myInfoComparator = instance.getIPAddr();
        }
        return hostName != null && hostName.equals(myInfoComparator);
    }

    public static String hostFromUrl(String url) {
        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            logger.warn("Cannot parse service URI {}", url, e);
            return null;
        }
        return uri.getHost();
    }
}
```

###总结：
1. PeerEurekaNodes用来保存了PeerEurekaNode的一组集合，并管理他们的生命周期（创建和销毁）。
2. 定时根据配置文件新增或者销毁PeerEurekaNode。
3. 只是解析和自己相同region的节点。

##PeerEurekaNode
```java
/**
 * The <code>PeerEurekaNode</code> represents a peer node to which information
 * should be shared from this node.
 * 
 * PeerEurekaNode表示一个需要将本节点的信息共享的对等节点。
 * 
 * <p>
 * This class handles replicating all update operations like
 * <em>Register,Renew,Cancel,Expiration and Status Changes</em> to the eureka
 * node it represents.
 * <p>
 *
 * 该类处理所有的更新操作的复制，比如：Register,Renew,Cancel,Expiration and Status Changes
 *
 */
public class PeerEurekaNode {

    /**
     * A time to wait before continuing work if there is network level error.
     * 发生网络异常时，需要等待多久再继续工作
     */
    private static final long RETRY_SLEEP_TIME_MS = 100;

    /**
     * A time to wait before continuing work if there is congestion on the server side.
     * 如果服务端阻塞，需要等待多久再继续工作
     */
    private static final long SERVER_UNAVAILABLE_SLEEP_TIME_MS = 1000;

    /**
     * Maximum amount of time in ms to wait for new items prior to dispatching a batch of tasks.
     * 分派一批任务之前等待新项目的最长时间（以毫秒为单位）
     */
    private static final long MAX_BATCHING_DELAY_MS = 500;

    /**
     * Maximum batch size for batched requests.
     * 批处理请求的最大批处理大小
     */
    private static final int BATCH_SIZE = 250;

    private static final Logger logger = LoggerFactory.getLogger(PeerEurekaNode.class);

    public static final String BATCH_URL_PATH = "peerreplication/batch/";

    public static final String HEADER_REPLICATION = "x-netflix-discovery-replication";

    private final String serviceUrl;
    private final EurekaServerConfig config;
    private final long maxProcessingDelayMs;
    private final PeerAwareInstanceRegistry registry;
    private final String targetHost;
    private final HttpReplicationClient replicationClient;

    //批处理的任务分发器，处理具体的复制操作（发送网络请求）
    private final TaskDispatcher<String, ReplicationTask> batchingDispatcher;
    //非批处理的任务分发器，只用来处理AWS的业务
    private final TaskDispatcher<String, ReplicationTask> nonBatchingDispatcher;

    public PeerEurekaNode(PeerAwareInstanceRegistry registry, String targetHost, String serviceUrl, HttpReplicationClient replicationClient, EurekaServerConfig config) {
        this(registry, targetHost, serviceUrl, replicationClient, config, BATCH_SIZE, MAX_BATCHING_DELAY_MS, RETRY_SLEEP_TIME_MS, SERVER_UNAVAILABLE_SLEEP_TIME_MS);
    }

    /* For testing */ PeerEurekaNode(PeerAwareInstanceRegistry registry, String targetHost, String serviceUrl,
                                     HttpReplicationClient replicationClient, EurekaServerConfig config,
                                     int batchSize, long maxBatchingDelayMs,
                                     long retrySleepTimeMs, long serverUnavailableSleepTimeMs) {
        this.registry = registry;
        this.targetHost = targetHost;
        this.replicationClient = replicationClient;

        this.serviceUrl = serviceUrl;
        this.config = config;
        this.maxProcessingDelayMs = config.getMaxTimeForReplication();

        String batcherName = getBatcherName();
        ReplicationTaskProcessor taskProcessor = new ReplicationTaskProcessor(targetHost, replicationClient);
        this.batchingDispatcher = TaskDispatchers.createBatchingTaskDispatcher(
                batcherName,
                config.getMaxElementsInPeerReplicationPool(),
                batchSize,
                config.getMaxThreadsForPeerReplication(),
                maxBatchingDelayMs,
                serverUnavailableSleepTimeMs,
                retrySleepTimeMs,
                taskProcessor
        );
        this.nonBatchingDispatcher = TaskDispatchers.createNonBatchingTaskDispatcher(
                targetHost,
                config.getMaxElementsInStatusReplicationPool(),
                config.getMaxThreadsForStatusReplication(),
                maxBatchingDelayMs,
                serverUnavailableSleepTimeMs,
                retrySleepTimeMs,
                taskProcessor
        );
    }

    /**
     * Sends the registration information of {@link InstanceInfo} receiving by
     * this node to the peer node represented by this class.
     *
     * 
     */
    public void register(final InstanceInfo info) throws Exception {
        long expiryTime = System.currentTimeMillis() + getLeaseRenewalOf(info);
        batchingDispatcher.process(
                taskId("register", info),
                new InstanceReplicationTask(targetHost, Action.Register, info, null, true) {
                    public EurekaHttpResponse<Void> execute() {
                        return replicationClient.register(info);
                    }
                },
                expiryTime
        );
    }

    /**
     * Send the cancellation information of an instance to the node represented
     * by this class.
     *
     * @param appName
     *            the application name of the instance.
     * @param id
     *            the unique identifier of the instance.
     * @throws Exception
     */
    public void cancel(final String appName, final String id) throws Exception {
        long expiryTime = System.currentTimeMillis() + maxProcessingDelayMs;
        batchingDispatcher.process(
                taskId("cancel", appName, id),
                new InstanceReplicationTask(targetHost, Action.Cancel, appName, id) {
                    @Override
                    public EurekaHttpResponse<Void> execute() {
                        return replicationClient.cancel(appName, id);
                    }

                    @Override
                    public void handleFailure(int statusCode, Object responseEntity) throws Throwable {
                        super.handleFailure(statusCode, responseEntity);
                        if (statusCode == 404) {
                            logger.warn("{}: missing entry.", getTaskName());
                        }
                    }
                },
                expiryTime
        );
    }

    /**
     * Send the heartbeat information of an instance to the node represented by
     * this class. If the instance does not exist the node, the instance
     * registration information is sent again to the peer node.
     *
     * @param appName
     *            the application name of the instance.
     * @param id
     *            the unique identifier of the instance.
     * @param info
     *            the instance info {@link InstanceInfo} of the instance.
     * @param overriddenStatus
     *            the overridden status information if any of the instance.
     * @throws Throwable
     */
    public void heartbeat(final String appName, final String id,
                          final InstanceInfo info, final InstanceStatus overriddenStatus,
                          boolean primeConnection) throws Throwable {
        if (primeConnection) {
            // We do not care about the result for priming request.
            replicationClient.sendHeartBeat(appName, id, info, overriddenStatus);
            return;
        }
        ReplicationTask replicationTask = new InstanceReplicationTask(targetHost, Action.Heartbeat, info, overriddenStatus, false) {
            @Override
            public EurekaHttpResponse<InstanceInfo> execute() throws Throwable {
                return replicationClient.sendHeartBeat(appName, id, info, overriddenStatus);
            }

            @Override
            public void handleFailure(int statusCode, Object responseEntity) throws Throwable {
                super.handleFailure(statusCode, responseEntity);
                if (statusCode == 404) {
                    logger.warn("{}: missing entry.", getTaskName());
                    if (info != null) {
                        logger.warn("{}: cannot find instance id {} and hence replicating the instance with status {}",
                                getTaskName(), info.getId(), info.getStatus());
                        register(info);
                    }
                } else if (config.shouldSyncWhenTimestampDiffers()) {
                    InstanceInfo peerInstanceInfo = (InstanceInfo) responseEntity;
                    if (peerInstanceInfo != null) {
                        syncInstancesIfTimestampDiffers(appName, id, info, peerInstanceInfo);
                    }
                }
            }
        };
        long expiryTime = System.currentTimeMillis() + getLeaseRenewalOf(info);
        batchingDispatcher.process(taskId("heartbeat", info), replicationTask, expiryTime);
    }

    /**
     * Send the status information of of the ASG represented by the instance.
     *
     * <p>
     * ASG (Autoscaling group) names are available for instances in AWS and the
     * ASG information is used for determining if the instance should be
     * registered as {@link InstanceStatus#DOWN} or {@link InstanceStatus#UP}.
     *
     * @param asgName
     *            the asg name if any of this instance.
     * @param newStatus
     *            the new status of the ASG.
     */
    public void statusUpdate(final String asgName, final ASGStatus newStatus) {
        long expiryTime = System.currentTimeMillis() + maxProcessingDelayMs;
        nonBatchingDispatcher.process(
                asgName,
                new AsgReplicationTask(targetHost, Action.StatusUpdate, asgName, newStatus) {
                    public EurekaHttpResponse<?> execute() {
                        return replicationClient.statusUpdate(asgName, newStatus);
                    }
                },
                expiryTime
        );
    }

    /**
     *
     * Send the status update of the instance.
     *
     */
    public void statusUpdate(final String appName, final String id,
                             final InstanceStatus newStatus, final InstanceInfo info) {
        long expiryTime = System.currentTimeMillis() + maxProcessingDelayMs;
        batchingDispatcher.process(
                taskId("statusUpdate", appName, id),
                new InstanceReplicationTask(targetHost, Action.StatusUpdate, info, null, false) {
                    @Override
                    public EurekaHttpResponse<Void> execute() {
                        return replicationClient.statusUpdate(appName, id, newStatus, info);
                    }
                },
                expiryTime
        );
    }

    /**
     * Delete instance status override.
     *
     */
    public void deleteStatusOverride(final String appName, final String id, final InstanceInfo info) {
        long expiryTime = System.currentTimeMillis() + maxProcessingDelayMs;
        batchingDispatcher.process(
                taskId("deleteStatusOverride", appName, id),
                new InstanceReplicationTask(targetHost, Action.DeleteStatusOverride, info, null, false) {
                    @Override
                    public EurekaHttpResponse<Void> execute() {
                        return replicationClient.deleteStatusOverride(appName, id, info);
                    }
                },
                expiryTime);
    }

    /**
     * Get the service Url of the peer eureka node.
     */
    public String getServiceUrl() {
        return serviceUrl;
    }


    /**
     * Shuts down all resources used for peer replication.
     */
    public void shutDown() {
        batchingDispatcher.shutdown();
        nonBatchingDispatcher.shutdown();
    }

    /**
     * Synchronize {@link InstanceInfo} information if the timestamp between
     * this node and the peer eureka nodes vary.
     */
    private void syncInstancesIfTimestampDiffers(String appName, String id, InstanceInfo info, InstanceInfo infoFromPeer) {
        try {
            if (infoFromPeer != null) {
                logger.warn("Peer wants us to take the instance information from it, since the timestamp differs,"
                        + "Id : {} My Timestamp : {}, Peer's timestamp: {}", id, info.getLastDirtyTimestamp(), infoFromPeer.getLastDirtyTimestamp());

                if (infoFromPeer.getOverriddenStatus() != null && !InstanceStatus.UNKNOWN.equals(infoFromPeer.getOverriddenStatus())) {
                    logger.warn("Overridden Status info -id {}, mine {}, peer's {}", id, info.getOverriddenStatus(), infoFromPeer.getOverriddenStatus());
                    registry.storeOverriddenStatusIfRequired(appName, id, infoFromPeer.getOverriddenStatus());
                }
                registry.register(infoFromPeer, true);
            }
        } catch (Throwable e) {
            logger.warn("Exception when trying to set information from peer :", e);
        }
    }

    public String getBatcherName() {
        String batcherName;
        try {
            batcherName = new URL(serviceUrl).getHost();
        } catch (MalformedURLException e1) {
            batcherName = serviceUrl;
        }
        return "target_" + batcherName;
    }

    private static String taskId(String requestType, String appName, String id) {
        return requestType + '#' + appName + '/' + id;
    }

    private static String taskId(String requestType, InstanceInfo info) {
        return taskId(requestType, info.getAppName(), info.getId());
    }

    private static int getLeaseRenewalOf(InstanceInfo info) {
        return (info.getLeaseInfo() == null ? Lease.DEFAULT_DURATION_IN_SECS : info.getLeaseInfo().getRenewalIntervalInSecs()) * 1000;
    }
}
```