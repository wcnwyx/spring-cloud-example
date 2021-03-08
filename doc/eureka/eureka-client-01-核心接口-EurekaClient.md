```java
/**
 * Define a simple interface over the current DiscoveryClient implementation.
 * 在当前的DiscoveryClient实现上定义一个简单的接口。
 *
 * EurekaClient API contracts are:
 *  - provide the ability to get InstanceInfo(s) (in various different ways)
 *  - 提供获取InstanceInfo的功能（以各种不同的方式）
 *  - provide the ability to get data about the local Client (known regions, own AZ etc)
 *  - 提供获取本地客户端数据的能力
 *  - provide the ability to register and access the healthcheck handler for the client
 *  - 提供注册和访问客户端的healthcheck处理程序的能力
 *
 */
@ImplementedBy(DiscoveryClient.class)
public interface EurekaClient extends LookupService {

    // ========================
    // getters for InstanceInfo
    // ========================

    // 从指定region获取Applications
    public Applications getApplicationsForARegion(@Nullable String region);

    // 从指定的serviceUrl获取Applications
    public Applications getApplications(String serviceUrl);

    //通过匹配vipAddress/secure来获取InstanceInfo集合
    public List<InstanceInfo> getInstancesByVipAddress(String vipAddress, boolean secure);

    //在指定的region里通过匹配vipAddress/secure来获取InstanceInfo集合
    public List<InstanceInfo> getInstancesByVipAddress(String vipAddress, boolean secure, @Nullable String region);

    //通过匹配vipAddress、appName、secure来获取InstanceInfo集合
    public List<InstanceInfo> getInstancesByVipAddressAndAppName(String vipAddress, String appName, boolean secure);

    
    
    // ==========================
    // getters for local metadata
    // ==========================

    //获取本客户端可以访问到的所有的regions
    public Set<String> getAllKnownRegions();

    //获取本实例自己在服务端可以看到的状态
    public InstanceInfo.InstanceStatus getInstanceRemoteStatus();


    
    // ===========================
    // healthcheck related methods
    // ===========================


    /**
     * Register {@link HealthCheckHandler} with the eureka client.
     *
     * Once registered, the eureka client will first make an onDemand update of the
     * registering instanceInfo by calling the newly registered healthcheck handler,
     * and subsequently invoke the {@link HealthCheckHandler} in intervals specified
     * by {@link EurekaClientConfig#getInstanceInfoReplicationIntervalSeconds()}.
     *
     * @param healthCheckHandler app specific healthcheck handler.
     */
    public void registerHealthCheck(HealthCheckHandler healthCheckHandler);

    /**
     * Register {@link EurekaEventListener} with the eureka client.
     *
     * Once registered, the eureka client will invoke {@link EurekaEventListener#onEvent} 
     * whenever there is a change in eureka client's internal state.  Use this instead of 
     * polling the client for changes.  
     * 
     * {@link EurekaEventListener#onEvent} is called from the context of an internal thread 
     * and must therefore return as quickly as possible without blocking.
     * 
     * @param eventListener
     */
    public void registerEventListener(EurekaEventListener eventListener);
    
    /**
     * Unregister a {@link EurekaEventListener} previous registered with {@link EurekaClient#registerEventListener}
     * or injected into the constructor of {@link DiscoveryClient}
     * 
     * @param eventListener
     * @return True if removed otherwise false if the listener was never registered.
     */
    public boolean unregisterEventListener(EurekaEventListener eventListener);
    
    /**
     * @return the current registered healthcheck handler
     */
    public HealthCheckHandler getHealthCheckHandler();

    // =============
    // other methods
    // =============

    //关闭，会发送一个注销的请求
    public void shutdown();
    
    //获取本客户端的配置信息
    public EurekaClientConfig getEurekaClientConfig();
    
    //获取本客户端的ApplicationInfoManager
    public ApplicationInfoManager getApplicationInfoManager();
}
```