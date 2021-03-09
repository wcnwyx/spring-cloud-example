RemoteRegionRegistry主要作用是获取远端其它region里的节点数据，也是实现了LookupService接口。  
AbstractInstanceRegistry里关于LookupService接口的逻辑里涉及到的是否从远端region获取信息，就是从这个类里获取的。  

##源码
```java
/**
 * Handles all registry operations that needs to be done on a eureka service running in an other region.
 * 处理在其他region中运行的eureka服务上需要完成的所有注册表操作.
 * 
 * The primary operations include fetching registry information from remote region and fetching delta information
 * on a periodic basis.
 * 
 * 主要操作包括从远程区域获取注册表信息以及定期获取增量信息。
 *
 * TODO: a lot of the networking code in this class can be replaced by newer code in
 * {@link com.netflix.discovery.DiscoveryClient}
 *
 */
public class RemoteRegionRegistry implements LookupService<String> {
    private static final Logger logger = LoggerFactory.getLogger(RemoteRegionRegistry.class);

    private final ApacheHttpClient4 discoveryApacheClient;
    private final EurekaJerseyClient discoveryJerseyClient;
    private final com.netflix.servo.monitor.Timer fetchRegistryTimer;
    private final URL remoteRegionURL;

    private final ScheduledExecutorService scheduler;
    // monotonically increasing generation counter to ensure stale threads do not reset registry to an older version
    //一个计数器，防止老的线程将注册表信息设置为老版本
    private final AtomicLong fetchRegistryGeneration = new AtomicLong(0);
    private final Lock fetchRegistryUpdateLock = new ReentrantLock();

    //保存从远端region获取到的Application信息
    private final AtomicReference<Applications> applications = new AtomicReference<Applications>(new Applications());
    private final AtomicReference<Applications> applicationsDelta = new AtomicReference<Applications>(new Applications());
    
    private final EurekaServerConfig serverConfig;
    private volatile boolean readyForServingData;
    private final EurekaHttpClient eurekaHttpClient;

    @Inject
    public RemoteRegionRegistry(EurekaServerConfig serverConfig,
                                EurekaClientConfig clientConfig,
                                ServerCodecs serverCodecs,
                                String regionName,
                                URL remoteRegionURL) {
        //省略部分初始化代码

        try {
            //初始化是直接进行一次数据获取
            if (fetchRegistry()) {
                this.readyForServingData = true;
            } else {
                logger.warn("Failed to fetch remote registry. This means this eureka server is not ready for serving "
                        + "traffic.");
            }
        } catch (Throwable e) {
            logger.error("Problem fetching registry information :", e);
        }

        //后续启动定时任务定期去获取数据
        Runnable remoteRegionFetchTask = new Runnable() {
            @Override
            public void run() {
                try {
                    if (fetchRegistry()) {
                        readyForServingData = true;
                    } else {
                        logger.warn("Failed to fetch remote registry. This means this eureka server is not "
                                + "ready for serving traffic.");
                    }
                } catch (Throwable e) {
                    logger.error(
                            "Error getting from remote registry :", e);
                }
            }
        };

        ThreadPoolExecutor remoteRegionFetchExecutor = new ThreadPoolExecutor(
                1, serverConfig.getRemoteRegionFetchThreadPoolSize(), 0, TimeUnit.SECONDS, new SynchronousQueue<Runnable>());  // use direct handoff

        scheduler = Executors.newScheduledThreadPool(1,
                new ThreadFactoryBuilder()
                        .setNameFormat("Eureka-RemoteRegionCacheRefresher_" + regionName + "-%d")
                        .setDaemon(true)
                        .build());

        scheduler.schedule(
                new TimedSupervisorTask(
                        "RemoteRegionFetch_" + regionName,
                        scheduler,
                        remoteRegionFetchExecutor,
                        serverConfig.getRemoteRegionRegistryFetchInterval(),
                        TimeUnit.SECONDS,
                        5,  // exponential backoff bound
                        remoteRegionFetchTask
                ),
                serverConfig.getRemoteRegionRegistryFetchInterval(), TimeUnit.SECONDS);
    }


    /**
     * Fetch the registry information from the remote region.
     * 从远端Region获取注册表信息
     */
    private boolean fetchRegistry() {
        boolean success;
        Stopwatch tracer = fetchRegistryTimer.start();

        try {
            // If the delta is disabled or if it is the first time, get all applications
            //如果增量的方式被禁止了，或者第一次获取，进行一次全量获取，否则进行增量获取
            if (serverConfig.shouldDisableDeltaForRemoteRegions()
                    || (getApplications() == null)
                    || (getApplications().getRegisteredApplications().size() == 0)) {
                logger.info("Disable delta property : {}", serverConfig.shouldDisableDeltaForRemoteRegions());
                logger.info("Application is null : {}", getApplications() == null);
                logger.info("Registered Applications size is zero : {}", getApplications().getRegisteredApplications().isEmpty());
                success = storeFullRegistry();
            } else {
                success = fetchAndStoreDelta();
            }
            logTotalInstances();
        } catch (Throwable e) {
            logger.error("Unable to fetch registry information from the remote registry {}", this.remoteRegionURL, e);
            return false;
        } finally {
            if (tracer != null) {
                tracer.stop();
            }
        }
        return success;
    }

    //获取并保存增量信息
    private boolean fetchAndStoreDelta() throws Throwable {
        long currGeneration = fetchRegistryGeneration.get();
        //增量获取数据
        Applications delta = fetchRemoteRegistry(true);

        if (delta == null) {
            logger.error("The delta is null for some reason. Not storing this information");
        } else if (fetchRegistryGeneration.compareAndSet(currGeneration, currGeneration + 1)) {
            this.applicationsDelta.set(delta);
        } else {
            delta = null;  // set the delta to null so we don't use it
            logger.warn("Not updating delta as another thread is updating it already");
        }

        if (delta == null) {
            logger.warn("The server does not allow the delta revision to be applied because it is not "
                    + "safe. Hence got the full registry.");
            //上面的判断决定不允许应用此次的增量变更数据，直接进行全量获取
            return storeFullRegistry();
        } else {
            String reconcileHashCode = "";
            if (fetchRegistryUpdateLock.tryLock()) {
                try {
                    //将增量数据更新到本地缓存
                    updateDelta(delta);
                    //服务端再返回数据的时候会将Applications的hash值也发送过来，
                    //上一步将增量数据更新到本地后，本地再算一个hash值相比较
                    reconcileHashCode = getApplications().getReconcileHashCode();
                } finally {
                    fetchRegistryUpdateLock.unlock();
                }
            } else {
                logger.warn("Cannot acquire update lock, aborting updateDelta operation of fetchAndStoreDelta");
            }

            // There is a diff in number of instances for some reason
            // hash值对不上的话，直接进行一次全量获取
            if ((!reconcileHashCode.equals(delta.getAppsHashCode()))) {
                return reconcileAndLogDifference(delta, reconcileHashCode);
            }
        }

        return delta != null;
    }

    /**
     * Updates the delta information fetches from the eureka server into the
     * local cache.
     * 将从远端region获取的的增量数据更新到本地缓存
     */
    private void updateDelta(Applications delta) {
        int deltaCount = 0;
        for (Application app : delta.getRegisteredApplications()) {
            for (InstanceInfo instance : app.getInstances()) {
                ++deltaCount;
                if (ActionType.ADDED.equals(instance.getActionType())) {
                    //新增的操作，直接加到本地的Applications中
                    Application existingApp = getApplications()
                            .getRegisteredApplications(instance.getAppName());
                    if (existingApp == null) {
                        getApplications().addApplication(app);
                    }
                    logger.debug("Added instance {} to the existing apps ",
                            instance.getId());
                    getApplications().getRegisteredApplications(
                            instance.getAppName()).addInstance(instance);
                } else if (ActionType.MODIFIED.equals(instance.getActionType())) {
                    //修改的操作，直接用新的覆盖本地的数据
                    Application existingApp = getApplications()
                            .getRegisteredApplications(instance.getAppName());
                    if (existingApp == null) {
                        getApplications().addApplication(app);
                    }
                    logger.debug("Modified instance {} to the existing apps ",
                            instance.getId());

                    getApplications().getRegisteredApplications(
                            instance.getAppName()).addInstance(instance);

                } else if (ActionType.DELETED.equals(instance.getActionType())) {
                    //删除的数据，将本地也删除
                    Application existingApp = getApplications()
                            .getRegisteredApplications(instance.getAppName());
                    if (existingApp == null) {
                        getApplications().addApplication(app);
                    }
                    logger.debug("Deleted instance {} to the existing apps ",
                            instance.getId());
                    getApplications().getRegisteredApplications(
                            instance.getAppName()).removeInstance(instance);
                }
            }
        }
        logger.debug(
                "The total number of instances fetched by the delta processor : {}",
                deltaCount);

    }


    /**
     * Gets the full registry information from the eureka server and stores it
     * locally.
     * 从远端region获取圈梁的注册表信息，并保存到本地
     */
    public boolean storeFullRegistry() {
        long currentGeneration = fetchRegistryGeneration.get();
        Applications apps = fetchRemoteRegistry(false);
        if (apps == null) {
            logger.error("The application is null for some reason. Not storing this information");
        } else if (fetchRegistryGeneration.compareAndSet(currentGeneration, currentGeneration + 1)) {
            applications.set(apps);
            applicationsDelta.set(apps);
            logger.info("Successfully updated registry with the latest content");
            return true;
        } else {
            logger.warn("Not updating applications as another thread is updating it already");
        }
        return false;
    }

    /**
     * Fetch registry information from the remote region.
     * 增量或者全量的从远端region获取注册表信息
     */
    private Applications fetchRemoteRegistry(boolean delta) {
        logger.info("Getting instance registry info from the eureka server : {} , delta : {}", this.remoteRegionURL, delta);

        if (shouldUseExperimentalTransport()) {
            try {
                EurekaHttpResponse<Applications> httpResponse = delta ? eurekaHttpClient.getDelta() : eurekaHttpClient.getApplications();
                int httpStatus = httpResponse.getStatusCode();
                if (httpStatus >= 200 && httpStatus < 300) {
                    logger.debug("Got the data successfully : {}", httpStatus);
                    return httpResponse.getEntity();
                }
                logger.warn("Cannot get the data from {} : {}", this.remoteRegionURL, httpStatus);
            } catch (Throwable t) {
                logger.error("Can't get a response from {}", this.remoteRegionURL, t);
            }
        } else {
            ClientResponse response = null;
            try {
                String urlPath = delta ? "apps/delta" : "apps/";

                response = discoveryApacheClient.resource(this.remoteRegionURL + urlPath)
                        .accept(MediaType.APPLICATION_JSON_TYPE)
                        .get(ClientResponse.class);
                int httpStatus = response.getStatus();
                if (httpStatus >= 200 && httpStatus < 300) {
                    logger.debug("Got the data successfully : {}", httpStatus);
                    return response.getEntity(Applications.class);
                }
                logger.warn("Cannot get the data from {} : {}", this.remoteRegionURL, httpStatus);
            } catch (Throwable t) {
                logger.error("Can't get a response from {}", this.remoteRegionURL, t);
            } finally {
                closeResponse(response);
            }
        }
        return null;
    }

    /**
     * Reconciles the delta information fetched to see if the hashcodes match.
     * 获取一次增量数据后，远程和本地Applications的hash值对应不上，说明有偏差，直接进行全量获取。
     */
    private boolean reconcileAndLogDifference(Applications delta, String reconcileHashCode) throws Throwable {
        logger.warn("The Reconcile hashcodes do not match, client : {}, server : {}. Getting the full registry",
                reconcileHashCode, delta.getAppsHashCode());

        long currentGeneration = fetchRegistryGeneration.get();

        Applications apps = this.fetchRemoteRegistry(false);
        if (apps == null) {
            logger.error("The application is null for some reason. Not storing this information");
            return false;
        }

        if (fetchRegistryGeneration.compareAndSet(currentGeneration, currentGeneration + 1)) {
            applications.set(apps);
            applicationsDelta.set(apps);
            logger.warn("The Reconcile hashcodes after complete sync up, client : {}, server : {}.",
                    getApplications().getReconcileHashCode(),
                    delta.getAppsHashCode());
            return true;
        }else {
            logger.warn("Not setting the applications map as another thread has advanced the update generation");
            return true;  // still return true
        }
    }

    @Override
    public Applications getApplications() {
        return applications.get();
    }

    @Override
    public InstanceInfo getNextServerFromEureka(String arg0, boolean arg1) {
        return null;
    }

    @Override
    public Application getApplication(String appName) {
        return this.applications.get().getRegisteredApplications(appName);
    }

    @Override
    public List<InstanceInfo> getInstancesById(String id) {
        List<InstanceInfo> list = Collections.emptyList();

        for (Application app : applications.get().getRegisteredApplications()) {
            InstanceInfo info = app.getByInstanceId(id);
            if (info != null) {
                list.add(info);
                return list;
            }
        }
        return list;
    }

}
```

总结：用于从远端region增量或者全量的获取注册表数据，并缓存到本地。
1. 初次启动，直接获取全量数据。
2. 后续启用定时任务从远端region获取增量数据。
3. 增量数据根据不同的动作类型（ADDED、MODIFIED、DELETED）更新到本地的Applications中，然后计算本地Applications的hash值和远端的Applications的hash值进行对比。
4. 如果hash值对不上，说明增量更新后有问题，直接再进行一次全量更新。
注意：这个逻辑其实和DiscoveryClient的获取注册表信息的逻辑是一样的。