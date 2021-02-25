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