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
     *
     * <p>
     * The next server is picked on a round-robin fashion. By default, this
     * method just returns the servers that are currently with
     * {@link com.netflix.appinfo.InstanceInfo.InstanceStatus#UP} status.
     * This configuration can be controlled by overriding the
     * {@link com.netflix.discovery.EurekaClientConfig#shouldFilterOnlyUpInstances()}.
     *
     * Note that in some cases (Eureka emergency mode situation), the instances
     * that are returned may not be unreachable, it is solely up to the client
     * at that point to timeout quickly and retry the next server.
     * </p>
     *
     * @param virtualHostname
     *            the virtual host name that is associated to the servers.
     * @param secure
     *            indicates whether this is a HTTP or a HTTPS request - secure
     *            means HTTPS.
     * @return the {@link InstanceInfo} information which contains the public
     *         host name of the next server in line to process the request based
     *         on the round-robin algorithm.
     * @throws java.lang.RuntimeException if the virtualHostname does not exist
     */
    InstanceInfo getNextServerFromEureka(String virtualHostname, boolean secure);
}
```