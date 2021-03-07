LookupService接口的功能和InstanceRegistry接口的部分功能是类似的，都是获取Applications、Application、InstanceInfo的信息。  
这一篇主要是看LookupService接口全部实现方法和InstanceRegistry接口的获取Applications、Application、InstanceInfo部分功能。    
只看大概的几个就好，其它的都类似，只是通过不同的参数获取而已。  

##源码分析
```java
public abstract class AbstractInstanceRegistry implements InstanceRegistry {

    private final ConcurrentHashMap<String, Map<String, Lease<InstanceInfo>>> registry
            = new ConcurrentHashMap<String, Map<String, Lease<InstanceInfo>>>();
    
    protected Map<String, RemoteRegionRegistry> regionNameVSRemoteRegistry = new HashMap<String, RemoteRegionRegistry>();
    
    /**
     * Returns the given app that is in this instance only, falling back to other regions transparently only
     * if specified in this client configuration.
     * 获取给定appName的Application信息，可以支持从其它region获取
     */
    @Override
    public Application getApplication(String appName) {
        boolean disableTransparentFallback = serverConfig.disableTransparentFallbackToOtherRegion();
        return this.getApplication(appName, !disableTransparentFallback);
    }

    /**
     * Get all applications in this instance registry, falling back to other regions if allowed in the Eureka config.
     *
     * 获取所有该注册表里记录的Application信息，支持从其它region获取
     */
    public Applications getApplications() {
        boolean disableTransparentFallback = serverConfig.disableTransparentFallbackToOtherRegion();
        if (disableTransparentFallback) {
            return getApplicationsFromLocalRegionOnly();
        } else {
            return getApplicationsFromAllRemoteRegions();  // Behavior of falling back to remote region can be disabled.
        }
    }

    /**
     * 只从本region获取所有应用信息
     */
    @Override
    public Applications getApplicationsFromLocalRegionOnly() {
        return getApplicationsFromMultipleRegions(EMPTY_STR_ARRAY);
    }

    /**
     * 从所有一直的其他region节点获取所有应用信息
     */
    public Applications getApplicationsFromAllRemoteRegions() {
        return getApplicationsFromMultipleRegions(allKnownRemoteRegions);
    }


    /**
     * This method will return applications with instances from all passed remote regions as well as the current region.
     * Thus, this gives a union view of instances from multiple regions. <br/>
     * The application instances for which this union will be done can be restricted to the names returned by
     * {@link EurekaServerConfig#getRemoteRegionAppWhitelist(String)} for every region. In case, there is no whitelist
     * defined for a region, this method will also look for a global whitelist by passing <code>null</code> to the
     * method {@link EurekaServerConfig#getRemoteRegionAppWhitelist(String)} <br/>
     * If you are not selectively requesting for a remote region, use {@link #getApplicationsFromAllRemoteRegions()}
     * or {@link #getApplicationsFromLocalRegionOnly()}
     *
     */
    public Applications getApplicationsFromMultipleRegions(String[] remoteRegions) {

        boolean includeRemoteRegion = null != remoteRegions && remoteRegions.length != 0;

        logger.debug("Fetching applications registry with remote regions: {}, Regions argument {}",
                includeRemoteRegion, remoteRegions);

        if (includeRemoteRegion) {
            GET_ALL_WITH_REMOTE_REGIONS_CACHE_MISS.increment();
        } else {
            GET_ALL_CACHE_MISS.increment();
        }
        Applications apps = new Applications();
        apps.setVersion(1L);

        //循环本地注册表，将所有实例信息封装到Applications里
        for (Entry<String, Map<String, Lease<InstanceInfo>>> entry : registry.entrySet()) {
            Application app = null;

            if (entry.getValue() != null) {
                for (Entry<String, Lease<InstanceInfo>> stringLeaseEntry : entry.getValue().entrySet()) {
                    Lease<InstanceInfo> lease = stringLeaseEntry.getValue();
                    if (app == null) {
                        app = new Application(lease.getHolder().getAppName());
                    }
                    app.addInstance(decorateInstanceInfo(lease));
                }
            }
            if (app != null) {
                apps.addApplication(app);
            }
        }
        if (includeRemoteRegion) {
            //循环所有其他region节点，获取实例信息并封装到Applications里
            for (String remoteRegion : remoteRegions) {
                //RemoteRegionRegistry也是一个LookupService的实现类
                //其通过网络请求定时从其它region中同步实例数据保存于本地使用，这个后面再单独看
                RemoteRegionRegistry remoteRegistry = regionNameVSRemoteRegistry.get(remoteRegion);
                if (null != remoteRegistry) {
                    Applications remoteApps = remoteRegistry.getApplications();
                    for (Application application : remoteApps.getRegisteredApplications()) {
                        if (shouldFetchFromRemoteRegistry(application.getName(), remoteRegion)) {
                            logger.info("Application {}  fetched from the remote region {}",
                                    application.getName(), remoteRegion);

                            Application appInstanceTillNow = apps.getRegisteredApplications(application.getName());
                            if (appInstanceTillNow == null) {
                                appInstanceTillNow = new Application(application.getName());
                                apps.addApplication(appInstanceTillNow);
                            }
                            for (InstanceInfo instanceInfo : application.getInstances()) {
                                appInstanceTillNow.addInstance(instanceInfo);
                            }
                        } else {
                            logger.debug("Application {} not fetched from the remote region {} as there exists a "
                                            + "whitelist and this app is not in the whitelist.",
                                    application.getName(), remoteRegion);
                        }
                    }
                } else {
                    logger.warn("No remote registry available for the remote region {}", remoteRegion);
                }
            }
        }
        apps.setAppsHashCode(apps.getReconcileHashCode());
        return apps;
    }

    /**
     * Gets the {@link InstanceInfo} information.
     *
     * @param appName the application name for which the information is requested.
     * @param id the unique identifier of the instance.
     * @return the information about the instance.
     */
    @Override
    public InstanceInfo getInstanceByAppAndId(String appName, String id) {
        return this.getInstanceByAppAndId(appName, id, true);
    }

    /**
     * Gets the {@link InstanceInfo} information.
     *
     * @param appName the application name for which the information is requested.
     * @param id the unique identifier of the instance.
     * @param includeRemoteRegions true, if we need to include applications from remote regions
     *                             as indicated by the region {@link URL} by this property
     *                             {@link EurekaServerConfig#getRemoteRegionUrls()}, false otherwise
     * @return the information about the instance.
     */
    @Override
    public InstanceInfo getInstanceByAppAndId(String appName, String id, boolean includeRemoteRegions) {
        Map<String, Lease<InstanceInfo>> leaseMap = registry.get(appName);
        Lease<InstanceInfo> lease = null;
        if (leaseMap != null) {
            lease = leaseMap.get(id);
        }
        if (lease != null
                && (!isLeaseExpirationEnabled() || !lease.isExpired())) {
            return decorateInstanceInfo(lease);
        } else if (includeRemoteRegions) {
            for (RemoteRegionRegistry remoteRegistry : this.regionNameVSRemoteRegistry.values()) {
                Application application = remoteRegistry.getApplication(appName);
                if (application != null) {
                    return application.getByInstanceId(id);
                }
            }
        }
        return null;
    }


    /**
     * @deprecated Try {@link #getInstanceByAppAndId(String, String)} instead.
     *
     * Get all instances by ID, including automatically asking other regions if the ID is unknown.
     *
     * @see com.netflix.discovery.shared.LookupService#getInstancesById(String)
     */
    @Deprecated
    public List<InstanceInfo> getInstancesById(String id) {
        return this.getInstancesById(id, true);
    }

    /**
     * @deprecated Try {@link #getInstanceByAppAndId(String, String, boolean)} instead.
     *
     * Get the list of instances by its unique id.
     *
     * @param id the unique id of the instance
     * @param includeRemoteRegions true, if we need to include applications from remote regions
     *                             as indicated by the region {@link URL} by this property
     *                             {@link EurekaServerConfig#getRemoteRegionUrls()}, false otherwise
     * @return list of InstanceInfo objects.
     */
    @Deprecated
    public List<InstanceInfo> getInstancesById(String id, boolean includeRemoteRegions) {
        List<InstanceInfo> list = new ArrayList<InstanceInfo>();

        for (Iterator<Entry<String, Map<String, Lease<InstanceInfo>>>> iter =
                     registry.entrySet().iterator(); iter.hasNext(); ) {

            Map<String, Lease<InstanceInfo>> leaseMap = iter.next().getValue();
            if (leaseMap != null) {
                Lease<InstanceInfo> lease = leaseMap.get(id);

                if (lease == null || (isLeaseExpirationEnabled() && lease.isExpired())) {
                    continue;
                }

                if (list == Collections.EMPTY_LIST) {
                    list = new ArrayList<InstanceInfo>();
                }
                list.add(decorateInstanceInfo(lease));
            }
        }
        if (list.isEmpty() && includeRemoteRegions) {
            for (RemoteRegionRegistry remoteRegistry : this.regionNameVSRemoteRegistry.values()) {
                for (Application application : remoteRegistry.getApplications()
                        .getRegisteredApplications()) {
                    InstanceInfo instanceInfo = application.getByInstanceId(id);
                    if (instanceInfo != null) {
                        list.add(instanceInfo);
                        return list;
                    }
                }
            }
        }
        return list;
    }
}
```