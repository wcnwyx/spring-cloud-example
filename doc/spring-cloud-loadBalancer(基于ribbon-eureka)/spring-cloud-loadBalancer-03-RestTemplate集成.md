RestTemplate通过@LoadBalance注解来开启的负载均衡功能，这一篇来梳理下RestTemplate是如何集成的负载均衡ribbon的。  
从第一篇可以知道负载均衡的调用时通过LoadBalancerClient.execute(String serviceId, LoadBalancerRequest<T> request)来完成调用的。  
下面看看RestTemplate和LoadBalancerClient如何关联上。  

##1： @LoadBalanced何时何地对RestTemplate做了什么？

###1.1： @LoadBalanced注解源码：
```java
/**
 * Annotation to mark a RestTemplate bean to be configured to use a LoadBalancerClient.
 * 注解标记一个RestTemplate，配置其使用一个LoadBalancerClient
 * 注意该注解被@Qualifier标注了
 */
@Target({ ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Qualifier
public @interface LoadBalanced {

}
```

###1.2： LoadBalancerAutoConfiguration配置类
```java
@Configuration
@ConditionalOnClass(RestTemplate.class)
@ConditionalOnBean(LoadBalancerClient.class)
@EnableConfigurationProperties(LoadBalancerRetryProperties.class)
public class LoadBalancerAutoConfiguration {

    //所有标注有@LoadBalanced注解的RestTemplate都将被注入到此List中
	@LoadBalanced
	@Autowired(required = false)
	private List<RestTemplate> restTemplates = Collections.emptyList();

    //SmartInitializingSingleton会在bean初始化完成后进行调用
	@Bean
	public SmartInitializingSingleton loadBalancedRestTemplateInitializerDeprecated(
			final ObjectProvider<List<RestTemplateCustomizer>> restTemplateCustomizers) {
		return () -> restTemplateCustomizers.ifAvailable(customizers -> {
            //将标注有@LoadBalanced的所有RestTemplate都经过RestTemplateCustomizer处理
			for (RestTemplate restTemplate : LoadBalancerAutoConfiguration.this.restTemplates) {
				for (RestTemplateCustomizer customizer : customizers) {
					customizer.customize(restTemplate);
				}
			}
		});
	}

	@Bean
	@ConditionalOnMissingBean
	public LoadBalancerRequestFactory loadBalancerRequestFactory(
			LoadBalancerClient loadBalancerClient) {
		//LoadBalancerRequestFactory就是用来创建LoadBalancerRequest的。
		//LoadBalancerRequestFactory中还放进去了一个LoadBalancerClient。
		//LoadBalancerRequest和LoadBalancerClient就是最开始看的核心部分喽。
		return new LoadBalancerRequestFactory(loadBalancerClient, this.transformers);
	}

	@Configuration
	@ConditionalOnMissingClass("org.springframework.retry.support.RetryTemplate")
	static class LoadBalancerInterceptorConfig {

		@Bean
		public LoadBalancerInterceptor ribbonInterceptor(
				LoadBalancerClient loadBalancerClient,
				LoadBalancerRequestFactory requestFactory) {
            //创建一个LoadBalancerInterceptor，用于设置到RestTemplate中，并传入了一个LoadBalancerRequestFactory
			return new LoadBalancerInterceptor(loadBalancerClient, requestFactory);
		}

		@Bean
		@ConditionalOnMissingBean
		public RestTemplateCustomizer restTemplateCustomizer(
				final LoadBalancerInterceptor loadBalancerInterceptor) {
			return restTemplate -> {
                //将LoadBalancerInterceptor设置到RestTemplate里
				List<ClientHttpRequestInterceptor> list = new ArrayList<>(
						restTemplate.getInterceptors());
				list.add(loadBalancerInterceptor);
				restTemplate.setInterceptors(list);
			};
		}

	}

}
```

通过上面的代码可以看出在配置类LoadBalancerAutoConfiguration中做了一下操作：  
1. 给每个标注有@LoadBalanced注解的RestTemplate的interceptors集合中添加了一个LoadBalancerInterceptor。
2. LoadBalancerInterceptor中持有了一个LoadBalancerRequestFactory，  
3. LoadBalancerRequestFactory可以用来创建LoadBalancerRequest，  
4. LoadBalancerRequestFactory中又持有了一个LoadBalancerClient，
这样子其实在LoadBalancerInterceptor中就可以获取到LoadBalancerRequest和LoadBalancerClient，那么就可以调用LoadBalancerClient.execute(String serviceId, LoadBalancerRequest<T> request)方法了。  
到这里RestTemplate就和负载均衡LoadBalancerClient关联上了。  


##2： RestTemplate代码梳理

###2.1： 接口ClientHttpRequest、ClientHttpResponse、ClientHttpRequestFactory
RestTemplate最终的执行时是通过不同的ClientHttpRequestFactory来创建出ClientHttpRequest来进行执行的，所以先看下这三个接口。  

接口ClientHttpRequest，表示一个客户端方的Http请求
```java
/**
 * Represents a client-side HTTP request.
 * Created via an implementation of the {@link ClientHttpRequestFactory}.
 *
 * 表示一个客户端方的Http请求。
 * 通过ClientHttpRequestFactory的实现来创建。
 * 
 * <p>A {@code ClientHttpRequest} can be {@linkplain #execute() executed},
 * receiving a {@link ClientHttpResponse} which can be read from.
 *
 * ClientHttpRequest可以通过execute()方法来执行，并且返回一个ClientHttpResponse，可以从中读取数据。
 */
public interface ClientHttpRequest extends HttpRequest, HttpOutputMessage {

	ClientHttpResponse execute() throws IOException;

}
```

接口ClientHttpResponse，表示一个客户端方的HTTP响应
```java
/**
 * Represents a client-side HTTP response.
 * Obtained via an calling of the {@link ClientHttpRequest#execute()}.
 *
 * 表示一个客户端方的HTTP响应。
 * 通过调用ClientHttpRequest.execute()获取。
 *
 * <p>A {@code ClientHttpResponse} must be {@linkplain #close() closed},
 * typically in a {@code finally} block.
 *
 * ClientHttpResponse必须调用close方法进行关闭，通常在finally块中调用。
 */
public interface ClientHttpResponse extends HttpInputMessage, Closeable {

    /**
     * Return the HTTP status code as an {@link HttpStatus} enum value.
     */
    HttpStatus getStatusCode() throws IOException;

    /**
     * Return the HTTP status code (potentially non-standard and not
     * resolvable through the {@link HttpStatus} enum) as an integer.
     * 返回一个可能是不标准的，无法通过HttpStatus枚举的状态码
     */
    int getRawStatusCode() throws IOException;

    /**
     * Return the HTTP status text of the response.
     */
    String getStatusText() throws IOException;

    /**
     * Close this response, freeing any resources created.
     */
    @Override
    void close();

}
```

接口ClientHttpRequestFactory，用于生成ClientHttpRequest的工厂
```java
/**
 * Factory for {@link ClientHttpRequest} objects.
 * Requests are created by the {@link #createRequest(URI, HttpMethod)} method.
 *
 * ClientHttpRequest的工厂类。
 */
@FunctionalInterface
public interface ClientHttpRequestFactory {

    /**
     * Create a new {@link ClientHttpRequest} for the specified URI and HTTP method.
     * <p>The returned request can be written to, and then executed by calling
     * {@link ClientHttpRequest#execute()}.
     */
    ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) throws IOException;

}
```

###2.2： RestTemplate执行方法-doExecute()
```java
public class RestTemplate extends InterceptingHttpAccessor implements RestOperations {
    
    protected <T> T doExecute(URI url, @Nullable HttpMethod method, @Nullable RequestCallback requestCallback,
                              @Nullable ResponseExtractor<T> responseExtractor) throws RestClientException {

        ClientHttpResponse response = null;
        try {
            //创建ClientHttpRequest
            ClientHttpRequest request = createRequest(url, method);
            if (requestCallback != null) {
                requestCallback.doWithRequest(request);
            }
            //调用ClientHttpRequest.execute来执行实际的调用操作
            response = request.execute();
            handleResponse(url, method, response);
            return (responseExtractor != null ? responseExtractor.extractData(response) : null);
        }
        catch (IOException ex) {
            String resource = url.toString();
            String query = url.getRawQuery();
            resource = (query != null ? resource.substring(0, resource.indexOf('?')) : resource);
            throw new ResourceAccessException("I/O error on " + method.name() +
                    " request for \"" + resource + "\": " + ex.getMessage(), ex);
        }
        finally {
            if (response != null) {
                response.close();
            }
        }
    }
}
```

父类InterceptingHttpAccessor  
```java
public abstract class InterceptingHttpAccessor extends HttpAccessor {

    private final List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>();

    @Nullable
    private volatile ClientHttpRequestFactory interceptingRequestFactory;

    public List<ClientHttpRequestInterceptor> getInterceptors() {
        return this.interceptors;
    }
    
    @Override
    public ClientHttpRequestFactory getRequestFactory() {
        //interceptors被配置类加入了一个LoadBalancerInterceptor，所以不为空
        List<ClientHttpRequestInterceptor> interceptors = getInterceptors();
        if (!CollectionUtils.isEmpty(interceptors)) {
            ClientHttpRequestFactory factory = this.interceptingRequestFactory;
            if (factory == null) {
                factory = new InterceptingClientHttpRequestFactory(super.getRequestFactory(), interceptors);
                this.interceptingRequestFactory = factory;
            }
            return factory;
        }
        else {
            return super.getRequestFactory();
        }
    }
}
```

父类HttpAccessor  
```java
public abstract class HttpAccessor {
    /**
     * Create a new {@link ClientHttpRequest} via this template's {@link ClientHttpRequestFactory}.
     */
    protected ClientHttpRequest createRequest(URI url, HttpMethod method) throws IOException {
        ClientHttpRequest request = getRequestFactory().createRequest(url, method);
        if (logger.isDebugEnabled()) {
            logger.debug("HTTP " + method.name() + " " + url);
        }
        return request;
    }
}
```

通过TestTemplate的逻辑梳理，可以知道RestTemplate通过@LoadBalanced集成ribbon后，ClientHttpRequestFactory使用的实现类是InterceptingClientHttpRequestFactory  
ClientHttpRequest使用的实现类是InterceptingClientHttpRequest

###2.3： InterceptingClientHttpRequest的执行过程
InterceptingClientHttpRequest继承于AbstractBufferingClientHttpRequest再继承于AbstractClientHttpRequest，  
所以现从顶级父类AbstractClientHttpRequest看接口ClientHttpRequest.execute()方法的实现逻辑。   
```java
/**
 * Abstract base for {@link ClientHttpRequest} that makes sure that headers
 * and body are not written multiple times.
 *
 * ClientHttpRequst的抽象基础类，确保headers和body不被多次写入。
 */
public abstract class AbstractClientHttpRequest implements ClientHttpRequest {

	private final HttpHeaders headers = new HttpHeaders();

	private boolean executed = false;


	@Override
	public final ClientHttpResponse execute() throws IOException {
		assertNotExecuted();
		ClientHttpResponse result = executeInternal(this.headers);
		this.executed = true;
		return result;
	}

    /**
     * Assert that this request has not been {@linkplain #execute() executed} yet.
     * 确保execute不被多次执行
     */
    protected void assertNotExecuted() {
        Assert.state(!this.executed, "ClientHttpRequest already executed");
    }

	/**
	 * Abstract template method that writes the given headers and content to the HTTP request.
	 * 抽象模板方法，将给定的headers和content写入Http请求
	 */
	protected abstract ClientHttpResponse executeInternal(HttpHeaders headers) throws IOException;

}


/**
 * Base implementation of {@link ClientHttpRequest} that buffers output
 * in a byte array before sending it over the wire.
 *
 * ClientHttpRequest的基础实现，在发送output之前缓存到一个byte数组里。
 */
abstract class AbstractBufferingClientHttpRequest extends AbstractClientHttpRequest {

	private ByteArrayOutputStream bufferedOutput = new ByteArrayOutputStream(1024);


	@Override
	protected ClientHttpResponse executeInternal(HttpHeaders headers) throws IOException {
		byte[] bytes = this.bufferedOutput.toByteArray();
		if (headers.getContentLength() < 0) {
			headers.setContentLength(bytes.length);
		}
		ClientHttpResponse result = executeInternal(headers, bytes);
		this.bufferedOutput = new ByteArrayOutputStream(0);
		return result;
	}

	/**
	 * Abstract template method that writes the given headers and content to the HTTP request.
	 * 抽象模板方法，将给定的headers和content写入Http请求
	 */
	protected abstract ClientHttpResponse executeInternal(HttpHeaders headers, byte[] bufferedOutput)
			throws IOException;


}


/**
 * Wrapper for a {@link ClientHttpRequest} that has support for {@link ClientHttpRequestInterceptor ClientHttpRequest} that has support for {@link ClientHttpRequestInterceptors}.
 *
 * 支持ClientHttpRequestInterceptor的一个ClientHttpRequest的封装类
 */
class InterceptingClientHttpRequest extends AbstractBufferingClientHttpRequest {

	private final ClientHttpRequestFactory requestFactory;

	private final List<ClientHttpRequestInterceptor> interceptors;

	private HttpMethod method;

	private URI uri;


	protected InterceptingClientHttpRequest(ClientHttpRequestFactory requestFactory,
			List<ClientHttpRequestInterceptor> interceptors, URI uri, HttpMethod method) {

		this.requestFactory = requestFactory;
		this.interceptors = interceptors;
		this.method = method;
		this.uri = uri;
	}



	@Override
	protected final ClientHttpResponse executeInternal(HttpHeaders headers, byte[] bufferedOutput) throws IOException {
        //new出来一个执行器来执行
		InterceptingRequestExecution requestExecution = new InterceptingRequestExecution();
		return requestExecution.execute(this, bufferedOutput);
	}


	private class InterceptingRequestExecution implements ClientHttpRequestExecution {

		private final Iterator<ClientHttpRequestInterceptor> iterator;

		public InterceptingRequestExecution() {
			this.iterator = interceptors.iterator();
		}

		@Override
		public ClientHttpResponse execute(HttpRequest request, byte[] body) throws IOException {
			if (this.iterator.hasNext()) {
                //循环调用拦截器处理
                //这里就用到了ClientHttpRequestInterceptor，里面封装了LoadBalancerClient和LoadBalancerRequestFactory
                //就可以调用负载均衡器的执行方法了
				ClientHttpRequestInterceptor nextInterceptor = this.iterator.next();
				return nextInterceptor.intercept(request, body, this);
			}
			else {
				HttpMethod method = request.getMethod();
				Assert.state(method != null, "No standard HTTP method");
				ClientHttpRequest delegate = requestFactory.createRequest(request.getURI(), method);
				request.getHeaders().forEach((key, value) -> delegate.getHeaders().addAll(key, value));
				if (body.length > 0) {
					if (delegate instanceof StreamingHttpOutputMessage) {
						StreamingHttpOutputMessage streamingOutputMessage = (StreamingHttpOutputMessage) delegate;
						streamingOutputMessage.setBody(outputStream -> StreamUtils.copy(body, outputStream));
					}
					else {
						StreamUtils.copy(body, delegate.getBody());
					}
				}
				return delegate.execute();
			}
		}
	}

}
```

###2.4 ClientHttpRequestInterceptor
ClientHttpRequest的拦截器
```java
/**
 * Intercepts client-side HTTP requests. Implementations of this interface can be
 * {@linkplain org.springframework.web.client.RestTemplate#setInterceptors registered}
 * with the {@link org.springframework.web.client.RestTemplate RestTemplate},
 * as to modify the outgoing {@link ClientHttpRequest} and/or the incoming
 * {@link ClientHttpResponse}.
 * 
 * 拦截客户端的HTTP请求（ClientHttpRequest）。
 * 该接口的实现可以通过RestTemplate.setInterceptors() 方法注册到restTemplate中，
 * 来修改传出的ClientHttpRequest或传入的ClientHttpResponse
 *
 * <p>The main entry point for interceptors is
 * {@link #intercept(HttpRequest, byte[], ClientHttpRequestExecution)}.
 *
 * 拦截器的主要入口是intercept方法
 */
@FunctionalInterface
public interface ClientHttpRequestInterceptor {

	/**
	 * Intercept the given request, and return a response. The given
	 * {@link ClientHttpRequestExecution} allows the interceptor to pass on the
	 * request and response to the next entity in the chain.
	 * 
	 * 来接给定的request，并返回一个response。
	 * 给定的ClientHttpRequestExecution允许拦截的请求和响应传递给链中的下一个实体。
	 * 
	 */
	ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
			throws IOException;

}
```

具有负载均衡功能的拦截器实现类
```java
public class LoadBalancerInterceptor implements ClientHttpRequestInterceptor {

	private LoadBalancerClient loadBalancer;

	private LoadBalancerRequestFactory requestFactory;

    //配置类LoadBalancerAutoConfiguration中初始化该类，并将LoadBalancerClient和LoadBalancerRequestFactory传入
	public LoadBalancerInterceptor(LoadBalancerClient loadBalancer,
			LoadBalancerRequestFactory requestFactory) {
		this.loadBalancer = loadBalancer;
		this.requestFactory = requestFactory;
	}

	public LoadBalancerInterceptor(LoadBalancerClient loadBalancer) {
		// for backwards compatibility
		this(loadBalancer, new LoadBalancerRequestFactory(loadBalancer));
	}

	@Override
	public ClientHttpResponse intercept(final HttpRequest request, final byte[] body,
			final ClientHttpRequestExecution execution) throws IOException {
		final URI originalUri = request.getURI();
		String serviceName = originalUri.getHost();
		Assert.state(serviceName != null,
				"Request URI does not contain a valid hostname: " + originalUri);
        //调用负载均衡器的方法
		return this.loadBalancer.execute(serviceName,
				this.requestFactory.createRequest(request, body, execution));
	}

}
```

##总结：
这里面有一些接口的名字比较像，所以看下来会有点乱，大概列一下这些接口及作用：
1. LoadBalancerClient：负载均衡器，提供了execute执行方法
2. LoadBalancerRequest： 负载均衡器封装的Request，在LoadBalancerClient.execute方法中使用，来具体执行请求操作。
3. ClientHttpRequest、ClientHttpResponse： spring对客户端层面http进行了请求和响应的封装。
    - InterceptingClientHttpRequest就是一个实现类，又有拦截器功能。
4. ClientHttpRequestInterceptor ：针对ClientHttpRequest的一个拦截器
    - LoadBalancerInterceptor 就是一个实现类，具有负载均衡功能。

RestTemplate集成负载均衡的大概步骤：
1. RestTemplate被@LoadBalanced注解来标注（具有@Qualifier特性）。
2. LoadBalancerAutoConfiguration 配置类将标注有@LoadBalanced注解的RestTemplate添加拦截器（LoadBalancerInterceptor）。
3. LoadBalancerInterceptor该拦截器中封装了LoadBalancerClient和LoadBalancerRequestFactory（生成LoadBalancerRequest）。
4. RestTemplate执行过程中触发拦截器，调用LoadBalancerClient.execute方法来实现负载均衡的调用。