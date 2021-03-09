InstanceInfo、Application、Applications三个基础类。  

#InstanceInfo
```java
/**
 * The class that holds information required for registration with
 * <tt>Eureka Server</tt> and to be discovered by other components.
 *
 * 该类持有向EurekaServer注册的所有信息，并可以被其它组件发现。
 * 就是说服务注册到eureka server的信息封装类。
 */
public class InstanceInfo {
    private volatile String instanceId;
    private volatile String appName;
    private volatile String ipAddr;
    //虚拟互联网协议地址（Virtual Internet Protocol address）
    private volatile String vipAddress;
    //secure Virtual Internet Protocol address
    private volatile String secureVipAddress;
    private volatile String homePageUrl;
    private volatile String statusPageUrl;
    private volatile String healthCheckUrl;
    private volatile String hostName;
    private volatile InstanceStatus status = InstanceStatus.UP;
    private volatile InstanceStatus overriddenStatus = InstanceStatus.UNKNOWN;
    private volatile Long lastUpdatedTimestamp;
    private volatile Long lastDirtyTimestamp;
    private volatile ActionType actionType;
}
```

##Application
```java
/**
 * The application class holds the list of instances for a particular
 * application.
 *
 * Application保持了一个指定应用的一组实例信息。
 * 同一个应用的多个实例，被封装到了一个Application中
 *
 */
public class Application {
    private String name;
    private final Set<InstanceInfo> instances;
    private final AtomicReference<List<InstanceInfo>> shuffledInstances;
    private final Map<String, InstanceInfo> instancesMap;
    //用于记录是否被修改过
    private volatile boolean isDirty = false;

    public void addInstance(InstanceInfo i) {
        instancesMap.put(i.getId(), i);
        synchronized (instances) {
            instances.remove(i);
            instances.add(i);
            isDirty = true;
        }
    }

    private void removeInstance(InstanceInfo i, boolean markAsDirty) {
        instancesMap.remove(i.getId());
        synchronized (instances) {
            instances.remove(i);
            if (markAsDirty) {
                isDirty = true;
            }
        }
    }
}
```

##Applications
```java
/**
 * The class that wraps all the registry information returned by eureka server.
 * 封装了由eureka server返回的所有注册表信息。
 * 
 * <p>
 * Note that the registry information is fetched from eureka server as specified
 * in {@link EurekaClientConfig#getRegistryFetchIntervalSeconds()}. Once the
 * information is fetched it is shuffled and also filtered for instances with
 * {@link InstanceStatus#UP} status as specified by the configuration
 * {@link EurekaClientConfig#shouldFilterOnlyUpInstances()}.
 * </p>
 * 客户端根据配置周期定期的从eureka server获取注册表信息。
 * 可以根据配置来过滤只返回状态为UP的实例信息。
 */
public class Applications {
    //hash值，服务端会返回给客户端，客户端在增量更新时，将增量数据更新到本地后，也会计算一下hash值然后比较。如果不一致，客户端会再进行一次全量获取。
    private String appsHashCode;
    private Long versionDelta;

    private final AbstractQueue<Application> applications;
    private final Map<String, Application> appNameApplicationMap;
    private final Map<String, VipIndexSupport> virtualHostNameAppMap;
    private final Map<String, VipIndexSupport> secureVirtualHostNameAppMap;
}
```

总结：
InstanceInfo： 就是一个服务实例注册到eureka server的所有信息的封装类。
Application： 将一个应用的多组实例进行封装保存。
Applications： 将eureka server的整个注册表中的所有应用和所有实例进行了封装。