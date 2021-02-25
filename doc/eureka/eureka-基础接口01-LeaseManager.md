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
2. cancel： 取消租约，客户端下线是调用取消接口。
3. renew: 续约，客户端在运行过程中，需要定时的发心跳来续约，防止租约过期被驱逐。  
4. evict： 驱逐，客户端在长时间没有续约的情况下，服务端会把该租约关联的实例给剔除。