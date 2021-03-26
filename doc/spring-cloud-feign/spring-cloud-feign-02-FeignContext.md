在上一篇看到每个@FeignClient都会有自己的ApplicationContext，多个ApplicationContext被封装保存在了FeignContext中。  
为什么要这样做呢？因为每个@FeignClient可能使用自身单独的配置信息来进行初始化，所以要进行隔离。  
下面先看一个demo，定义两个@FeignClient,一个配置类，代码如下所示：  
```java
//通过ribbon来访问http://demo-server/demo地址，自身没有单独的配置类信息
@FeignClient(name = "demo-server")
public interface DemoFeignService {
    @RequestMapping(value = "/demo")
    String demoCall();
}

//通过url参数来固定访问地址，因为githug返回的是json，所以自定义了一个配置类，来自定义一个Decoder
@FeignClient(name = "github-server", url = "https://api.github.com", configuration = GitHubConfiguration.class)
public interface GithubFeignService {
    @RequestMapping(value = "/repos/{owner}/{repo}/contributors")
    List<Contributor> contributors(@PathVariable("owner") String owner, @PathVariable("repo") String repo);
}

//自定义了一个Decoder实现
//注意，这里不能添加@Configuration，如果这样加了，会被spring默认给加载处理的，那就不是github-service所私有了
public class GitHubConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public Decoder feignDecoder() {
        return (Response response, Type type)->{
            System.out.println("i am custom Decoder!");
            if (response.body() == null)
                return null;
            Reader reader = response.body().asReader();
            try {
                Gson gson = new Gson();
                return gson.fromJson(reader, type);
            } catch (Exception e) {
                return null;
            } finally {
                reader.close();
            }
        };
    }
}
```
1. demo-server的@FeignClient只是使用默认配置类FeignClientsConfiguration，那么解析到的Decoder就是SpringEncoder
2. github-server的@FeignClient因为自己配置了configuration，他就有一个自己私有的配置类，在创建Decoder的时候会优先使用自己私有的配置类创建。
3. @FeignClient中自定义的configuration类不能添加@Configuration，否则会被spring自身容器处理掉，那就不是单个FeignClient所私有的了。

##FeignContext、NamedContextFactory
FeignContext没有什么逻辑，只是继承了NamedContextFactory表示feign特有的而已。
```java
/**
 * A factory that creates instances of feign classes. It creates a Spring
 * ApplicationContext per client name, and extracts the beans that it needs from there.
 *
 * FeignClientSpecification就是在上一篇第一步解析@EnableFeignClient和@FeignClient的配置类信息时使用的
 */
public class FeignContext extends NamedContextFactory<FeignClientSpecification> {

	public FeignContext() {
		super(FeignClientsConfiguration.class, "feign", "feign.client.name");
	}

}
```


```java
/**
 * Creates a set of child contexts that allows a set of Specifications to define the beans
 * in each child context.
 *
 * Ported from spring-cloud-netflix FeignClientFactory and SpringClientFactory
 *
 */
// TODO: add javadoc
public abstract class NamedContextFactory<C extends NamedContextFactory.Specification>
		implements DisposableBean, ApplicationContextAware {

    private final String propertySourceName;

    private final String propertyName;

    //保存所有FeignClient对应的ApplicationContext
    private Map<String, AnnotationConfigApplicationContext> contexts = new ConcurrentHashMap<>();

    private Map<String, C> configurations = new ConcurrentHashMap<>();

    //Spring自身的ApplicationContext为他们的父节点
    private ApplicationContext parent;

    private Class<?> defaultConfigType;
    
    public NamedContextFactory(Class<?> defaultConfigType, String propertySourceName,
                               String propertyName) {
        this.defaultConfigType = defaultConfigType;
        this.propertySourceName = propertySourceName;
        this.propertyName = propertyName;
    }

    @Override
    public void setApplicationContext(ApplicationContext parent) throws BeansException {
        this.parent = parent;
    }

    public void setConfigurations(List<C> configurations) {
        for (C client : configurations) {
            this.configurations.put(client.getName(), client);
        }
    }

    protected AnnotationConfigApplicationContext getContext(String name) {
        if (!this.contexts.containsKey(name)) {
            //还没有该name的ApplicationContext时进行创建
            synchronized (this.contexts) {
                if (!this.contexts.containsKey(name)) {
                    this.contexts.put(name, createContext(name));
                }
            }
        }
        return this.contexts.get(name);
    }

    protected AnnotationConfigApplicationContext createContext(String name) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        if (this.configurations.containsKey(name)) {
            //优先注册自己私有的配置类，就像demo中的GitHubConfiguration就会在这里处理
            for (Class<?> configuration : this.configurations.get(name)
                    .getConfiguration()) {
                context.register(configuration);
            }
        }
        for (Map.Entry<String, C> entry : this.configurations.entrySet()) {
            //default.开头的是@EnableFeignClient中配置的配置类，默认全局所有client都使用
            if (entry.getKey().startsWith("default.")) {
                for (Class<?> configuration : entry.getValue().getConfiguration()) {
                    context.register(configuration);
                }
            }
        }
        context.register(PropertyPlaceholderAutoConfiguration.class,
                this.defaultConfigType);
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource(
                this.propertySourceName,
                Collections.<String, Object>singletonMap(this.propertyName, name)));
        if (this.parent != null) {
            // Uses Environment from parent as well as beans
            //设置父节点
            context.setParent(this.parent);
            // jdk11 issue
            // https://github.com/spring-cloud/spring-cloud-netflix/issues/3101
            context.setClassLoader(this.parent.getClassLoader());
        }
        context.setDisplayName(generateDisplayName(name));
        //刷新初始化
        context.refresh();
        return context;
    }
}
```