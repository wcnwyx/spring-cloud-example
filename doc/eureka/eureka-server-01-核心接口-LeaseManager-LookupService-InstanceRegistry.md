##LeaseManager接口（租约管理）
```java
/**
 * This class is responsible for creating/renewing and evicting a <em>lease</em>
 * for a particular instance.
 * 该class 用于对一个特定实例（instance）的租约（lease）进行创建、续约和驱逐
 * <p>
 * Leases determine what instances receive traffic. When there is no renewal
 * request from the client, the lease gets expired and the instances are evicted
 * out of {@link AbstractInstanceRegistry}. This is key to instances receiving traffic
 * or not.
 * <p>
 *  租约决定哪些实例接收流量。档客户端没有续约的请求，租约将过期并且实例将被驱逐。这是实例是否接收流量的关键。
 *  具体的逻辑都在AbstractInstanceRegistry这个实现类里。
 *  封装的泛型使用的是InstanceInfo
 */
public interface LeaseManager<T> {

    /**
     * Assign a new {@link Lease} to the passed in {@link T}.
     * 为传入的T分配一个新的租约（就是注册一个实例）
     * @param leaseDuration 租期
     * @param isReplication 是否是从eureka节点复制过来的
     */
    void register(T r, int leaseDuration, boolean isReplication);

    /**
     * Cancel the {@link Lease} associated w/ the passed in <code>appName</code>
     * and <code>id</code>.
     * 取消与传入的appName和id相关联的租约
     *
     * @param appName
     *            - unique id of the application.
     * @param id
     *            - unique id within appName.
     * @param isReplication
     *            - whether this is a replicated entry from another eureka node.
     * @return true, if the operation was successful, false otherwise.
     */
    boolean cancel(String appName, String id, boolean isReplication);

    /**
     * Renew the {@link Lease} associated w/ the passed in <code>appName</code>
     * and <code>id</code>.
     * 续订与传入的appName和id相关联的租约
     *
     * @param id
     *            - unique id within appName
     * @param isReplication
     *            - whether this is a replicated entry from another ds node
     * @return whether the operation of successful
     */
    boolean renew(String appName, String id, boolean isReplication);

    /**
     * Evict {@link T}s with expired {@link Lease}(s).
     * 驱逐租约已到期的实例T
     */
    void evict();
}
```

LeaseManager租约管理接口方法不多，每个方法也很好理解。  
1. register： 注册一个新的租约，客户端启动服务是调用注册接口，将自己的服务信息注册进来。  
2. cancel： 取消租约，客户端下线时调用取消接口。
3. renew: 续约，客户端在运行过程中，需要定时的发心跳来续约，防止租约过期被驱逐。  
4. evict： 驱逐，客户端在长时间没有续约的情况下，服务端会把该租约关联的实例给剔除。


##LookupService接口（服务查找）
```java
/**
 * Lookup service for finding active instances.
 * 
 * 用于查找活动的实例服务。
 */
public interface LookupService<T> {

    /**
     * Returns the corresponding {@link Application} object which is basically a
     * container of all registered <code>appName</code> {@link InstanceInfo}s.
     * 
     * 根据appName参数来查找已注册的Application
     */
    Application getApplication(String appName);

    /**
     * Returns the {@link Applications} object which is basically a container of
     * all currently registered {@link Application}s.
     *
     * 获取所有的已注册的Application，封装了一个Applications对象返回
     */
    Applications getApplications();

    /**
     * Returns the {@link List} of {@link InstanceInfo}s matching the the passed
     * in id. A single {@link InstanceInfo} can possibly be registered w/ more
     * than one {@link Application}s
     *
     * 根据参数id查找所有符合的InstanceInfo集合，一个InstanceInfo可能注册到多个Application
     */
    List<InstanceInfo> getInstancesById(String id);

    /**
     * Gets the next possible server to process the requests from the registry
     * information received from eureka.
     * 根据虚拟主机名获取下一个服务实例信息（InstanceInfo）
     * <p>
     * The next server is picked on a round-robin fashion. By default, this
     * method just returns the servers that are currently with
     * {@link com.netflix.appinfo.InstanceInfo.InstanceStatus#UP} status.
     * This configuration can be controlled by overriding the
     * {@link com.netflix.discovery.EurekaClientConfig#shouldFilterOnlyUpInstances()}.
     *
     * 以循环的方式获取下一个服务，该方法只返回服务当前状态是UP状态的。
     * 可以通过EurekaClientConfig#shouldFilterOnlyUpInstances()这个配置覆盖该设置。
     * 
     * Note that in some cases (Eureka emergency mode situation), the instances
     * that are returned may not be unreachable, it is solely up to the client
     * at that point to timeout quickly and retry the next server.
     * 注意在一些情况下（紧急模式情况），返回的服务实例可能是无法访问的，此时完全由客户端来决定是否快速超时再获取下一个服务。
     * </p>
     *
     * @param virtualHostname
     *            the virtual host name that is associated to the servers.
     * @param secure
     *            indicates whether this is a HTTP or a HTTPS request - secure
     *            means HTTPS.
     */
    InstanceInfo getNextServerFromEureka(String virtualHostname, boolean secure);
}
```
LookupService接口就是定义了一些查找应用、实例的一些方法。  
涉及到三个bean，大概了解下是做什么的：
1. InstanceInfo：一个服务进程实例的封装bean，包括appName、id、status、ip、port等。
2. Application： 相同应用的InstanceInfo集合。一个应用可以启动多个实例来提供服务，比如说一个用户服务，可以启动三个进程来做集群服务。
3. Applications： 封装了多个Application。


##InstanceRegistry接口（实例注册表）
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
InstanceRegistry接口大概分为一下几个部分的功能：  
1. 启动、关闭时的逻辑。
2. overriddenStatus相关操作，用于实现红黑部署。
3. 应用（Application）、实例（InstanceInfo）信息的相关获取操作，和LookupService接口的功能有点类似。
4. 响应缓存（ResponseCache）
5. 自我保护模式（SelfPreservationMode）的相关逻辑
6. 注册、取消操作的统计信息获取