##接口ClientHttpRequest、ClientHttpResponse、ClientHttpRequestFactory
RestTemplate最终的执行时通过创建ClientHttpRequest来进行执行的，所以先看下这三个接口。  
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


##RestTemplate的执行流程梳理

###执行方法-RestTemplate.doExecute()
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

###InterceptingHttpAccessor.getRequestFactory()
```java
public abstract class InterceptingHttpAccessor extends HttpAccessor {

    private final List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>();

    @Nullable
    private volatile ClientHttpRequestFactory interceptingRequestFactory;
    
    @Override
    public ClientHttpRequestFactory getRequestFactory() {
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

###HttpAccessor.createRequest()
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