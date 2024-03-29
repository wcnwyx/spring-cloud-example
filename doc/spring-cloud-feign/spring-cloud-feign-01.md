spring集成Netflix-Feign是通过@FeignClient、@EnableFeignClients这两个注解来实现的。  
@FeignClient来标注一个接口，和Netflix-Feign中表示http api的接口是相同的。  
@EnableFeignClients注解是spring中很常用的开启某个功能的注解，内部通过@Import(FeignClientsRegistrar.class)来处理逻辑。  

##1: FeignClientsRegistrar处理了那些功能
```java
class FeignClientsRegistrar
		implements ImportBeanDefinitionRegistrar, ResourceLoaderAware, EnvironmentAware {
    private ResourceLoader resourceLoader;

    private Environment environment;

    //ResourceLoaderAware实现方法
    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    //EnvironmentAware实现方法
    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void registerBeanDefinitions(AnnotationMetadata metadata,
                                        BeanDefinitionRegistry registry) {
        //处理@EnableFeignClients注解里配置的defaultConfiguration配置类信息
        registerDefaultConfiguration(metadata, registry);
        registerFeignClients(metadata, registry);
    }

    //将@EnableFeignClients注解里配置的defaultConfiguration配置类信息
    //封装为BeanDefinition信息注册到spring的BeanDefinitionRegistry中
    //其中classType为FeignClientSpecification.class
    private void registerDefaultConfiguration(AnnotationMetadata metadata,
                                              BeanDefinitionRegistry registry) {
        Map<String, Object> defaultAttrs = metadata
                .getAnnotationAttributes(EnableFeignClients.class.getName(), true);

        if (defaultAttrs != null && defaultAttrs.containsKey("defaultConfiguration")) {
            String name;
            if (metadata.hasEnclosingClass()) {
                name = "default." + metadata.getEnclosingClassName();
            }
            else {
                name = "default." + metadata.getClassName();
            }
            registerClientConfiguration(registry, name,
                    defaultAttrs.get("defaultConfiguration"));
        }
    }

    //通过scanner来扫描标注有@FeignClient注解的接口做以下处理：
    //1. 解析注解@FeignClient的configuration属性并封装为BeanDefinition进行注册，和registerDefaultConfiguration处理大体一样
    //2. 解析接口，将接口封装为BeanDefinition来进行注册，classType为FeignClientFactoryBean.class（FeignClientFactoryBean看名字就知道是spring的FactoryBean，用于生成FeignClient）
    public void registerFeignClients(AnnotationMetadata metadata,
                                     BeanDefinitionRegistry registry) {
        ClassPathScanningCandidateComponentProvider scanner = getScanner();
        scanner.setResourceLoader(this.resourceLoader);

        Set<String> basePackages;

        Map<String, Object> attrs = metadata
                .getAnnotationAttributes(EnableFeignClients.class.getName());
        AnnotationTypeFilter annotationTypeFilter = new AnnotationTypeFilter(
                FeignClient.class);
        final Class<?>[] clients = attrs == null ? null
                : (Class<?>[]) attrs.get("clients");
        if (clients == null || clients.length == 0) {
            scanner.addIncludeFilter(annotationTypeFilter);
            basePackages = getBasePackages(metadata);
        }
        else {
            final Set<String> clientClasses = new HashSet<>();
            basePackages = new HashSet<>();
            for (Class<?> clazz : clients) {
                basePackages.add(ClassUtils.getPackageName(clazz));
                clientClasses.add(clazz.getCanonicalName());
            }
            AbstractClassTestingTypeFilter filter = new AbstractClassTestingTypeFilter() {
                @Override
                protected boolean match(ClassMetadata metadata) {
                    String cleaned = metadata.getClassName().replaceAll("\\$", ".");
                    return clientClasses.contains(cleaned);
                }
            };
            scanner.addIncludeFilter(
                    new AllTypeFilter(Arrays.asList(filter, annotationTypeFilter)));
        }

        for (String basePackage : basePackages) {
            Set<BeanDefinition> candidateComponents = scanner
                    .findCandidateComponents(basePackage);
            for (BeanDefinition candidateComponent : candidateComponents) {
                if (candidateComponent instanceof AnnotatedBeanDefinition) {
                    // verify annotated class is an interface
                    AnnotatedBeanDefinition beanDefinition = (AnnotatedBeanDefinition) candidateComponent;
                    AnnotationMetadata annotationMetadata = beanDefinition.getMetadata();
                    Assert.isTrue(annotationMetadata.isInterface(),
                            "@FeignClient can only be specified on an interface");

                    Map<String, Object> attributes = annotationMetadata
                            .getAnnotationAttributes(
                                    FeignClient.class.getCanonicalName());

                    String name = getClientName(attributes);
                    registerClientConfiguration(registry, name,
                            attributes.get("configuration"));

                    registerFeignClient(registry, annotationMetadata, attributes);
                }
            }
        }
    }

    //将Configuration封装为BeanDefinition进行注册
    private void registerClientConfiguration(BeanDefinitionRegistry registry, Object name,
                                             Object configuration) {
        BeanDefinitionBuilder builder = BeanDefinitionBuilder
                .genericBeanDefinition(FeignClientSpecification.class);
        builder.addConstructorArgValue(name);
        builder.addConstructorArgValue(configuration);
        registry.registerBeanDefinition(
                name + "." + FeignClientSpecification.class.getSimpleName(),
                builder.getBeanDefinition());
    }

    //将标注有@FeignClient注解的接口封装为BeanDefinition进行注册。
    private void registerFeignClient(BeanDefinitionRegistry registry,
                                     AnnotationMetadata annotationMetadata, Map<String, Object> attributes) {
        String className = annotationMetadata.getClassName();
        BeanDefinitionBuilder definition = BeanDefinitionBuilder
                .genericBeanDefinition(FeignClientFactoryBean.class);
        validate(attributes);
        definition.addPropertyValue("url", getUrl(attributes));
        definition.addPropertyValue("path", getPath(attributes));
        String name = getName(attributes);
        definition.addPropertyValue("name", name);
        String contextId = getContextId(attributes);
        definition.addPropertyValue("contextId", contextId);
        definition.addPropertyValue("type", className);
        definition.addPropertyValue("decode404", attributes.get("decode404"));
        definition.addPropertyValue("fallback", attributes.get("fallback"));
        definition.addPropertyValue("fallbackFactory", attributes.get("fallbackFactory"));
        definition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);

        String alias = contextId + "FeignClient";
        AbstractBeanDefinition beanDefinition = definition.getBeanDefinition();

        boolean primary = (Boolean) attributes.get("primary"); // has a default, won't be
        // null

        beanDefinition.setPrimary(primary);

        String qualifier = getQualifier(attributes);
        if (StringUtils.hasText(qualifier)) {
            alias = qualifier;
        }

        BeanDefinitionHolder holder = new BeanDefinitionHolder(beanDefinition, className,
                new String[] { alias });
        BeanDefinitionReaderUtils.registerBeanDefinition(holder, registry);
    }
}
```
总结：FeignClientsRegistrar
1. 将@EnableFeignClients注解里配置的defaultConfiguration配置类信息封装为BeanDefinition信息注册到spring的BeanDefinitionRegistry中，其中classType为FeignClientSpecification.class
2. 通过scanner来扫描标注有@FeignClient注解的接口做以下处理： 
   - 解析注解@FeignClient的configuration属性并封装为BeanDefinition进行注册
   - 将接口类封装为BeanDefinition来进行注册，className为接口自身的名字，classType为FeignClientFactoryBean.class（FeignClientFactoryBean看名字就知道是spring的FactoryBean，用于生成FeignClient）
3. 为什么要将配置信息和接口信息解析为BeanDefinition呢？因为后续要将这些信息当成一个spring的正常bean来生成使用，接口会被业务调用类自动注入来使用。

##2: FeignClientFactoryBean
上面的逻辑看下来，@FeignClient的接口被封装了BeanDefinition注册到了spring中，其classType就是FeignClientFactoryBean，一个工厂bean，用于生成FeignClient，被调用方注入使用。
下面看下源码看看FeignClient的生成逻辑。  
```java
class FeignClientFactoryBean
		implements FactoryBean<Object>, InitializingBean, ApplicationContextAware {
    //FactoryBean接口的实现方法
    @Override
    public Object getObject() throws Exception {
        return getTarget();
    }

    //FactoryBean接口的实现方法
    @Override
    public Class<?> getObjectType() {
        return this.type;
    }

    //FactoryBean接口的实现方法
    @Override
    public boolean isSingleton() {
        return true;
    }

    /**
     * @param <T> the target type of the Feign client
     * @return a {@link Feign} client created with the specified data and the context
     * information
     */
    <T> T getTarget() {
        //FeignContext是一个NamedContextFactory，就是将每个@FeignClient都生成一个ApplicationContext，并通过name作为key保存于map中
       //因为每个@FeignClient都可以配置自己私有的configuration，所以要进行隔离处理。这个后面再详细看，这里先有个概念
        FeignContext context = this.applicationContext.getBean(FeignContext.class);
        //从自己的ApplicationContext中获取Feign.Builder
        Feign.Builder builder = feign(context);

        //如果@FeignClient中没有配置url，则使用具有负载均衡功能的Feign Client
        if (!StringUtils.hasText(this.url)) {
            if (!this.name.startsWith("http")) {
                this.url = "http://" + this.name;
            }
            else {
                this.url = this.name;
            }
            this.url += cleanPath();
            return (T) loadBalance(builder, context,
                    new HardCodedTarget<>(this.type, this.name, this.url));
        }
        
        //如果@FeignClient中配置了url，则表示该请求直接使用配置的url进行处理，则使用不具有负载均衡功能的Feign Client
        if (StringUtils.hasText(this.url) && !this.url.startsWith("http")) {
            this.url = "http://" + this.url;
        }
        String url = this.url + cleanPath();
        Client client = getOptional(context, Client.class);
        if (client != null) {
            if (client instanceof LoadBalancerFeignClient) {
                // not load balancing because we have a url,
                // but ribbon is on the classpath, so unwrap
               //如果说获取到了client是具有负载均衡功能的，但是因为配置了固定的url，所以将包装的原始client（不具有负载均衡功能）获取使用
                client = ((LoadBalancerFeignClient) client).getDelegate();
            }
            builder.client(client);
        }
        Targeter targeter = get(context, Targeter.class);
        return (T) targeter.target(this, builder, context,
                new HardCodedTarget<>(this.type, this.name, url));
    }

    //从FeignContext中获取到自己私有的ApplicationContext，并从中获取相关bean
   protected Feign.Builder feign(FeignContext context) {
      FeignLoggerFactory loggerFactory = get(context, FeignLoggerFactory.class);
      Logger logger = loggerFactory.create(this.type);

      // @formatter:off
      Feign.Builder builder = get(context, Feign.Builder.class)
              // required values
              .logger(logger)
              .encoder(get(context, Encoder.class))
              .decoder(get(context, Decoder.class))
              .contract(get(context, Contract.class));
      // @formatter:on

      //通过配置文件再进行Feign.Builder的配置
      configureFeign(context, builder);

      return builder;
   }

   //构建一个具有负载均衡功能的Feign Client，使用的是LoadBalancerFeignClient，里面封装了ribbon的IClient，这个和netflix-feign中使用ribbon套路一样
   protected <T> T loadBalance(Feign.Builder builder, FeignContext context,
                               HardCodedTarget<T> target) {
      Client client = getOptional(context, Client.class);
      if (client != null) {
         builder.client(client);
         Targeter targeter = get(context, Targeter.class);
         return targeter.target(this, builder, context, target);
      }

      throw new IllegalStateException(
              "No Feign Client for loadBalancing defined. Did you forget to include spring-cloud-starter-netflix-ribbon?");
   }
}
```
FeignClient的生成逻辑并不复杂，
1. 根据@FeignClient是否配置了url来决定是否生成具有负载均衡功能的client，springcloud-feign不一定非要和ribbon来结合使用
2. 每个@FeignClient都对应了一个私有的ApplicationContext，自己的Feign.Builder所需要的各种组件都从自己私有的ApplicationContext中获取
下面我们再看下Feign.Builder及其需要的组件都是在哪里配置的以及配置的是什么


##3: 配置类FeignClientsConfiguration、DefaultFeignLoadBalancedConfiguration、FeignAutoConfiguration
Netflix-Feign的使用是需要通过Feign.Builder这个构建器来构建出来Feign，然后通过java的动态代理最终为接口生成一个代理类的。  
Feign.Builder需要配置Encoder、Decoder、Contract、Client、Retryer这些属性，这些属性的配置就是在FeignClientsConfiguration里进行了配置。  
```java
@Configuration
public class FeignClientsConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public Decoder feignDecoder() {
		return new OptionalDecoder(
				new ResponseEntityDecoder(new SpringDecoder(this.messageConverters)));
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnMissingClass("org.springframework.data.domain.Pageable")
	public Encoder feignEncoder() {
		return new SpringEncoder(this.messageConverters);
	}

	@Bean
	@ConditionalOnClass(name = "org.springframework.data.domain.Pageable")
	@ConditionalOnMissingBean
	public Encoder feignEncoderPageable() {
		return new PageableSpringEncoder(new SpringEncoder(this.messageConverters));
	}

	@Bean
	@ConditionalOnMissingBean
	public Contract feignContract(ConversionService feignConversionService) {
		return new SpringMvcContract(this.parameterProcessors, feignConversionService);
	}

	@Bean
	@ConditionalOnMissingBean
	public Retryer feignRetryer() {
		return Retryer.NEVER_RETRY;
	}

	@Bean
	@Scope("prototype")
	@ConditionalOnMissingBean
	public Feign.Builder feignBuilder(Retryer retryer) {
		return Feign.builder().retryer(retryer);
	}

	//如何引用了Hystrix，那将会使用HystrixFeign.builder()
	@Configuration
	@ConditionalOnClass({ HystrixCommand.class, HystrixFeign.class })
	protected static class HystrixFeignConfiguration {

		@Bean
		@Scope("prototype")
		@ConditionalOnMissingBean
		@ConditionalOnProperty(name = "feign.hystrix.enabled")
		public Feign.Builder feignHystrixBuilder() {
			return HystrixFeign.builder();
		}

	}

}

@Configuration
class DefaultFeignLoadBalancedConfiguration {

   @Bean
   @ConditionalOnMissingBean
   public Client feignClient(CachingSpringLoadBalancerFactory cachingFactory,
                             SpringClientFactory clientFactory) {
       //LoadBalancerFeignClient只是对Client.default()进行了一个封装，使其具有ribbon的负载均衡功能
      //在生成Client的时候有看到过，如果@FeignClient中配置了url，如果获取到的Client是LoadBalancerFeignClient，就会将其拆封，使用期内部封装的Client.Default
      return new LoadBalancerFeignClient(new Client.Default(null, null), cachingFactory,
              clientFactory);
   }

}

@Configuration
@ConditionalOnClass(Feign.class)
@EnableConfigurationProperties({ FeignClientProperties.class,
        FeignHttpClientProperties.class })
public class FeignAutoConfiguration {

   //在最开始的时候，FeignClientsRegistrar解析处理了@EnableFeignClient和@FeignClient中的configuration信息会自动注入到这里
   @Autowired(required = false)
   private List<FeignClientSpecification> configurations = new ArrayList<>();

   @Bean
   public FeignContext feignContext() {
      //FeignContext初始化，使用所有Feign相关的configuration信息
      FeignContext context = new FeignContext();
      context.setConfigurations(this.configurations);
      return context;
   }
}
```
总结：
1. 主要作用就是来构建Feign.Builder，及其各种配件（Encoder、Decoder、Contract、Retryer等）
2. 默认使用的就是Netflix-Feign自带的Feign.Builder，如果配置了Hystrix，将使用HystrixFeign.builder
3. Contract使用了SpringMvcContract，因为Feign接口的注解都是使用的spring mvc的注解
4. Retryer默认使用的是Retryer.NEVER_RETRY
5. LoadBalancerFeignClient使用委托模式将真是的网络请求还是委托给了Client.Default来进行处理，自己只是实现了负债均衡的功能而已。