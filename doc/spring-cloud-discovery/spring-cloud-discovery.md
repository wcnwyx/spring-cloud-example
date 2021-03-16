spring cloud中的服务发现属于客户端模式，接口定义是DiscoveryClient。
spring cloud只是将各种服务注册中心的客户端进行了封装而已，自身没有什么特殊的业务。

##DiscoveryClient源码
```java
/**
 * Represents read operations commonly available to discovery services such as Netflix
 * Eureka or consul.io.
 * 表示发现服务通常可用的操作。比如eureka或者consul
 */
public interface DiscoveryClient extends Ordered {

	/**
	 * Default order of the discovery client.
	 */
	int DEFAULT_ORDER = 0;

	/**
	 * A human-readable description of the implementation, used in HealthIndicator.
	 * 一个易于理解的实现描述，用于健康指标里。
	 */
	String description();

	/**
	 * Gets all ServiceInstances associated with a particular serviceId.
	 * 
     * 获取与特定serviceId关联的所有ServiceInstance
	 */
	List<ServiceInstance> getInstances(String serviceId);

	/**
	 * @return All known service IDs.
     * 返回所有知道的serviceId
	 */
	List<String> getServices();

	/**
	 * Default implementation for getting order of discovery clients.
	 * @return order
	 */
	@Override
	default int getOrder() {
		return DEFAULT_ORDER;
	}

}
```


##EurekaDiscoveryClient
基于Eureka注册中心的服务发现实现类，就是把EurekaClient进行了封装调用而已  
```java
/**
 * A {@link DiscoveryClient} implementation for Eureka.
 *
 * Netflix-Eureka的 DiscoveryClient实现
 */
public class EurekaDiscoveryClient implements DiscoveryClient {

	/**
	 * Client description {@link String}.
	 */
	public static final String DESCRIPTION = "Spring Cloud Eureka Discovery Client";

	//Netflix自身提供的client类
	private final EurekaClient eurekaClient;

	private final EurekaClientConfig clientConfig;

	@Deprecated
	public EurekaDiscoveryClient(EurekaInstanceConfig config, EurekaClient eurekaClient) {
		this(eurekaClient, eurekaClient.getEurekaClientConfig());
	}

	public EurekaDiscoveryClient(EurekaClient eurekaClient,
			EurekaClientConfig clientConfig) {
		this.clientConfig = clientConfig;
		this.eurekaClient = eurekaClient;
	}

	@Override
	public String description() {
		return DESCRIPTION;
	}

	@Override
	public List<ServiceInstance> getInstances(String serviceId) {
	    //spring cloud是将serviceId当做eureka中的vipAddress（网络虚拟协议地址）来使用的
		List<InstanceInfo> infos = this.eurekaClient.getInstancesByVipAddress(serviceId,
				false);
		List<ServiceInstance> instances = new ArrayList<>();
		for (InstanceInfo info : infos) {
			instances.add(new EurekaServiceInstance(info));
		}
		return instances;
	}

	@Override
	public List<String> getServices() {
		Applications applications = this.eurekaClient.getApplications();
		if (applications == null) {
			return Collections.emptyList();
		}
		List<Application> registered = applications.getRegisteredApplications();
		List<String> names = new ArrayList<>();
		for (Application app : registered) {
			if (app.getInstances().isEmpty()) {
				continue;
			}
			names.add(app.getName().toLowerCase());

		}
		return names;
	}

	@Override
	public int getOrder() {
		return clientConfig instanceof Ordered ? ((Ordered) clientConfig).getOrder()
				: DiscoveryClient.DEFAULT_ORDER;
	}

	/**
	 * An Eureka-specific {@link ServiceInstance} implementation.
     * eureka特有的对ServiceInstance的实现，封装了netflix-eureka的InstanceInfo
	 */
	public static class EurekaServiceInstance implements ServiceInstance {
        
	    //Netflix-eureka中的对实例封装的结构
		private InstanceInfo instance;

		public EurekaServiceInstance(InstanceInfo instance) {
			Assert.notNull(instance, "Service instance required");
			this.instance = instance;
		}

		public InstanceInfo getInstanceInfo() {
			return instance;
		}

		@Override
		public String getInstanceId() {
			return this.instance.getId();
		}

		@Override
		public String getServiceId() {
			return this.instance.getAppName();
		}

		@Override
		public String getHost() {
			return this.instance.getHostName();
		}

		@Override
		public int getPort() {
			if (isSecure()) {
				return this.instance.getSecurePort();
			}
			return this.instance.getPort();
		}

		@Override
		public boolean isSecure() {
			// assume if secure is enabled, that is the default
			return this.instance.isPortEnabled(SECURE);
		}

		@Override
		public URI getUri() {
			return DefaultServiceInstance.getUri(this);
		}

		@Override
		public Map<String, String> getMetadata() {
			return this.instance.getMetadata();
		}

		@Override
		public String getScheme() {
			return getUri().getScheme();
		}

	}

}
```

##ZookeeperDiscoveryClient
基于Zookeeper为注册中心的DiscoveryClient封装，也是用apache自身提供的类进行了封装  
```java
/**
 * Zookeeper version of {@link DiscoveryClient}. Capable of resolving aliases from
 * {@link ZookeeperDependencies} to service names in Zookeeper.
 *
 * Zookeeper的实现
 */
public class ZookeeperDiscoveryClient implements DiscoveryClient {

	private static final Log log = LogFactory.getLog(ZookeeperDiscoveryClient.class);

	private final ZookeeperDependencies zookeeperDependencies;

	//apache curator的实现类
	private final ServiceDiscovery<ZookeeperInstance> serviceDiscovery;

	private final ZookeeperDiscoveryProperties zookeeperDiscoveryProperties;

	public ZookeeperDiscoveryClient(ServiceDiscovery<ZookeeperInstance> serviceDiscovery,
			ZookeeperDependencies zookeeperDependencies,
			ZookeeperDiscoveryProperties zookeeperDiscoveryProperties) {
		this.serviceDiscovery = serviceDiscovery;
		this.zookeeperDependencies = zookeeperDependencies;
		this.zookeeperDiscoveryProperties = zookeeperDiscoveryProperties;
	}

	@Override
	public String description() {
		return "Spring Cloud Zookeeper Discovery Client";
	}

	private static org.springframework.cloud.client.ServiceInstance createServiceInstance(
			String serviceId, ServiceInstance<ZookeeperInstance> serviceInstance) {
		return new ZookeeperServiceInstance(serviceId, serviceInstance);
	}

	@Override
	public List<org.springframework.cloud.client.ServiceInstance> getInstances(
			final String serviceId) {
		try {
			if (getServiceDiscovery() == null) {
				return Collections.EMPTY_LIST;
			}
			//将serviceId转换为zk的路径
			String serviceIdToQuery = getServiceIdToQuery(serviceId);
			//ServiceInstance是apache curator对服务实例的封装体
			Collection<ServiceInstance<ZookeeperInstance>> zkInstances = getServiceDiscovery()
					.queryForInstances(serviceIdToQuery);
			List<org.springframework.cloud.client.ServiceInstance> instances = new ArrayList<>();
			for (ServiceInstance<ZookeeperInstance> instance : zkInstances) {
				instances.add(createServiceInstance(serviceIdToQuery, instance));
			}
			return instances;
		}
		catch (KeeperException.NoNodeException e) {
			if (log.isDebugEnabled()) {
				log.debug(
						"Error getting instances from zookeeper. Possibly, no service has registered.",
						e);
			}
			// this means that nothing has registered as a service yes
			return Collections.emptyList();
		}
		catch (Exception exception) {
			rethrowRuntimeException(exception);
		}
		return new ArrayList<>();
	}

	private ServiceDiscovery<ZookeeperInstance> getServiceDiscovery() {
		return this.serviceDiscovery;
	}

	private String getServiceIdToQuery(String serviceId) {
		if (this.zookeeperDependencies != null
				&& this.zookeeperDependencies.hasDependencies()) {
			String pathForAlias = this.zookeeperDependencies.getPathForAlias(serviceId);
			return pathForAlias.isEmpty() ? serviceId : pathForAlias;
		}
		return serviceId;
	}

	@Override
	public List<String> getServices() {
		List<String> services = null;
		if (getServiceDiscovery() == null) {
			log.warn(
					"Service Discovery is not yet ready - returning empty list of services");
			return Collections.emptyList();
		}
		try {
			Collection<String> names = getServiceDiscovery().queryForNames();
			if (names == null) {
				return Collections.emptyList();
			}
			services = new ArrayList<>(names);
		}
		catch (KeeperException.NoNodeException e) {
			if (log.isDebugEnabled()) {
				log.debug(
						"Error getting services from zookeeper. Possibly, no service has registered.",
						e);
			}
			// this means that nothing has registered as a service yes
			return Collections.emptyList();
		}
		catch (Exception e) {
			rethrowRuntimeException(e);
		}
		return services;
	}

	@Override
	public int getOrder() {
		return this.zookeeperDiscoveryProperties.getOrder();
	}

}
```

##初始化
EurekaDiscoveryClient 是在spring-cloud-netflix-eureka-client-xxx.jar中通过配置类EurekaClientAutoConfiguration进行初始化：
```
	@Bean
	public DiscoveryClient discoveryClient(EurekaClient client,
			EurekaClientConfig clientConfig) {
		return new EurekaDiscoveryClient(client, clientConfig);
	}
```

ZookeeperDiscoveryClient是在spring-cloud-zookeeper-discovery-xx.jar 中通过配置类ZookeeperDiscoveryAutoConfiguration进行初始化：
```
	@Bean
	@ConditionalOnMissingBean
	public ZookeeperDiscoveryClient zookeeperDiscoveryClient(
			ServiceDiscovery<ZookeeperInstance> serviceDiscovery,
			ZookeeperDiscoveryProperties zookeeperDiscoveryProperties) {
		return new ZookeeperDiscoveryClient(serviceDiscovery, this.zookeeperDependencies,
				zookeeperDiscoveryProperties);
	}
```