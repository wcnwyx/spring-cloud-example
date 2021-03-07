InstanceRegistry接口定义了实例注册表的一些操作，其继承了LeaseManager接口和LookupService接口。

```java
public interface InstanceRegistry extends LeaseManager<InstanceInfo>, LookupService<String> {

    //注册表初始化完成后开始正常工作
    void openForTraffic(ApplicationInfoManager applicationInfoManager, int count);
    //服务关闭时调用，关闭一些定时器，释放一些资源
    void shutdown();

    //关于overriddenStatus的一组操作
    //简单来说就是可以使用单独的服务可以操控eureka serer来改变某个实例的状态，来实现红黑部署。
    @Deprecated
    void storeOverriddenStatusIfRequired(String id, InstanceStatus overriddenStatus);

    //存储overriddenStatus
    void storeOverriddenStatusIfRequired(String appName, String id, InstanceStatus overriddenStatus);

    //根据appName、id更新实例的状态，一起overriddenStatus
    boolean statusUpdate(String appName, String id, InstanceStatus newStatus,
                         String lastDirtyTimestamp, boolean isReplication);
    
    //删除overriddenStatus并将实例的状态更新为参数newStatus
    boolean deleteStatusOverride(String appName, String id, InstanceStatus newStatus,
                                 String lastDirtyTimestamp, boolean isReplication);
    //获取overriddenStatus的一个快照
    Map<String, InstanceStatus> overriddenInstanceStatusesSnapshot();


    //只从本地region获取Applications
    Applications getApplicationsFromLocalRegionOnly();

    //获取有序的Application集合
    List<Application> getSortedApplications();

    //根据appName获取Application信息，可以通过includeRemoteRegion参数来控制是否也从其它的region来获取
    Application getApplication(String appName, boolean includeRemoteRegion);

    //根据appName和id来获取实例信息
    InstanceInfo getInstanceByAppAndId(String appName, String id);

    //根据appName和id来获取实例信息，支持从其它region来获取
    InstanceInfo getInstanceByAppAndId(String appName, String id, boolean includeRemoteRegions);

    //清空注册表
    void clearRegistry();

    //初始化响应缓存
    void initializedResponseCache();

    //获取响应缓存
    ResponseCache getResponseCache();
    
    //获取上一分钟收到的续约请求的个数（用于自我保护模式的逻辑）
    long getNumOfRenewsInLastMin();

    //获取每分钟续约请求个数的最低阀值（用于自我保护模式的逻辑）
    int getNumOfRenewsPerMinThreshold();

    //是否低于续约阀值
    int isBelowRenewThresold();

    //获取最近注册的实例信息
    List<Pair<Long, String>> getLastNRegisteredInstances();

    //获取最近取消的实例信息
    List<Pair<Long, String>> getLastNCanceledInstances();

    //租约过期是否可用，定时器驱逐过期租约时会判断
    boolean isLeaseExpirationEnabled();

    //自我保护模式是否可用，就是配置文件是否开启了自我保护模式
    boolean isSelfPreservationModeEnabled();
}
```