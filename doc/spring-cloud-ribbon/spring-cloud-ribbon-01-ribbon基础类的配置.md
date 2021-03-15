ribbon的使用需要有这几个基本的接口实现类（建议先看看ribbon自身的使用）：
1. IClientConfig: ribbon客户端配置
2. ILoadBalancer: 负载均衡器
3. IRule: 负载均衡器里使用的路由规则
4. IPing: 负载均衡器里检测服务列表是否存活的Ping
5. ServerList: 负载均衡器实现从动态愿获取服务列表的策略
6. ServerListUpdater： 负载均衡器动态服务器列表更新的策略

##ribbon基础类在springCloud中的配置：
下面代码是spring-cloud-netflix-ribbon-xxx.jar包里的配置类，定义了ribbon所需要的基本的类。  
```java
@Configuration
@EnableConfigurationProperties
@Import({ HttpClientConfiguration.class, OkHttpRibbonConfiguration.class,
		RestClientRibbonConfiguration.class, HttpClientRibbonConfiguration.class })
public class RibbonClientConfiguration {

	/**
	 * Ribbon client default connect timeout.
	 */
	public static final int DEFAULT_CONNECT_TIMEOUT = 1000;

	/**
	 * Ribbon client default read timeout.
	 */
	public static final int DEFAULT_READ_TIMEOUT = 1000;

	/**
	 * Ribbon client default Gzip Payload flag.
	 */
	public static final boolean DEFAULT_GZIP_PAYLOAD = true;

	@RibbonClientName
	private String name = "client";

	@Autowired
	private PropertiesFactory propertiesFactory;

	@Bean
	@ConditionalOnMissingBean
	public IClientConfig ribbonClientConfig() {
		DefaultClientConfigImpl config = new DefaultClientConfigImpl();
		config.loadProperties(this.name);
		config.set(CommonClientConfigKey.ConnectTimeout, DEFAULT_CONNECT_TIMEOUT);
		config.set(CommonClientConfigKey.ReadTimeout, DEFAULT_READ_TIMEOUT);
		config.set(CommonClientConfigKey.GZipPayload, DEFAULT_GZIP_PAYLOAD);
		return config;
	}

	@Bean
	@ConditionalOnMissingBean
	public IRule ribbonRule(IClientConfig config) {
		if (this.propertiesFactory.isSet(IRule.class, name)) {
			return this.propertiesFactory.get(IRule.class, config, name);
		}
		ZoneAvoidanceRule rule = new ZoneAvoidanceRule();
		rule.initWithNiwsConfig(config);
		return rule;
	}

	@Bean
	@ConditionalOnMissingBean
	public IPing ribbonPing(IClientConfig config) {
		if (this.propertiesFactory.isSet(IPing.class, name)) {
			return this.propertiesFactory.get(IPing.class, config, name);
		}
		return new DummyPing();
	}

	@Bean
	@ConditionalOnMissingBean
	@SuppressWarnings("unchecked")
	public ServerList<Server> ribbonServerList(IClientConfig config) {
		if (this.propertiesFactory.isSet(ServerList.class, name)) {
			return this.propertiesFactory.get(ServerList.class, config, name);
		}
		ConfigurationBasedServerList serverList = new ConfigurationBasedServerList();
		serverList.initWithNiwsConfig(config);
		return serverList;
	}

	@Bean
	@ConditionalOnMissingBean
	public ServerListUpdater ribbonServerListUpdater(IClientConfig config) {
		return new PollingServerListUpdater(config);
	}

	@Bean
	@ConditionalOnMissingBean
	public ILoadBalancer ribbonLoadBalancer(IClientConfig config,
			ServerList<Server> serverList, ServerListFilter<Server> serverListFilter,
			IRule rule, IPing ping, ServerListUpdater serverListUpdater) {
		if (this.propertiesFactory.isSet(ILoadBalancer.class, name)) {
			return this.propertiesFactory.get(ILoadBalancer.class, config, name);
		}
		return new ZoneAwareLoadBalancer<>(config, rule, ping, serverList,
				serverListFilter, serverListUpdater);
	}

	@Bean
	@ConditionalOnMissingBean
	@SuppressWarnings("unchecked")
	public ServerListFilter<Server> ribbonServerListFilter(IClientConfig config) {
		if (this.propertiesFactory.isSet(ServerListFilter.class, name)) {
			return this.propertiesFactory.get(ServerListFilter.class, config, name);
		}
		ZonePreferenceServerListFilter filter = new ZonePreferenceServerListFilter();
		filter.initWithNiwsConfig(config);
		return filter;
	}

	@Bean
	@ConditionalOnMissingBean
	public RibbonLoadBalancerContext ribbonLoadBalancerContext(ILoadBalancer loadBalancer,
			IClientConfig config, RetryHandler retryHandler) {
		return new RibbonLoadBalancerContext(loadBalancer, config, retryHandler);
	}

	@Bean
	@ConditionalOnMissingBean
	public RetryHandler retryHandler(IClientConfig config) {
		return new DefaultLoadBalancerRetryHandler(config);
	}

}
```


##ribbon与Eureka结合使用在springCloud中的配置类：
ribbon与eureka结合使用，是通过ribbon的DynamicServerListLoadBalancer的ServerList策略来实现的，  
使用的是DiscoveryEnabledNIWSServerList和EurekaNotificationServerListUpdater。  
但是springCloud中并未配置EurekaNotificationServerListUpdater，而是使用ribbon自带的PollingServerListUpdater  
下面代码是spring-cloud-netflix-eureka-client-xxx.jar里的定义：  
```java
@Configuration
public class EurekaRibbonClientConfiguration {

	private static final Log log = LogFactory
			.getLog(EurekaRibbonClientConfiguration.class);

	@Value("${ribbon.eureka.approximateZoneFromHostname:false}")
	private boolean approximateZoneFromHostname = false;

	@RibbonClientName
	private String serviceId = "client";

	@Autowired(required = false)
	private EurekaClientConfig clientConfig;

	@Autowired(required = false)
	private EurekaInstanceConfig eurekaConfig;


    //NIWSDiscoveryPing这个ping的逻辑只是检查了一下InstanceInfo的状态是否为UP
	@Bean
	@ConditionalOnMissingBean
	public IPing ribbonPing(IClientConfig config) {
		if (this.propertiesFactory.isSet(IPing.class, serviceId)) {
			return this.propertiesFactory.get(IPing.class, config, serviceId);
		}
		NIWSDiscoveryPing ping = new NIWSDiscoveryPing();
		ping.initWithNiwsConfig(config);
		return ping;
	}

    //这里定义了ServerList针对于eureka的实现类DiscoveryEnabledNIWSServerList，只是又封装了一层而已
	@Bean
	@ConditionalOnMissingBean
	public ServerList<?> ribbonServerList(IClientConfig config,
			Provider<EurekaClient> eurekaClientProvider) {
		if (this.propertiesFactory.isSet(ServerList.class, serviceId)) {
			return this.propertiesFactory.get(ServerList.class, config, serviceId);
		}
		DiscoveryEnabledNIWSServerList discoveryServerList = new DiscoveryEnabledNIWSServerList(
				config, eurekaClientProvider);
		DomainExtractingServerList serverList = new DomainExtractingServerList(
				discoveryServerList, config, this.approximateZoneFromHostname);
		return serverList;
	}

}
```
