netflix-ribbon其实本身就是一个负载均衡器的系统，但是spring-cloud又集成包装了一层而已。

##1：相关接口定义
###1.1 接口LoadBalancerClient
LoadBalancerClient和netflix-ribbon中的IClient接口功能差不多。
```java
/**
 * Represents a client-side load balancer.
 *
 * 代表客户端层面的负载均衡器
 */
public interface LoadBalancerClient extends ServiceInstanceChooser {

	/**
	 * Executes request using a ServiceInstance from the LoadBalancer for the specified
	 * service.
     * 根据serviceId从负载均衡器中查找到服务实例（ServiceInstance）然后执行请求（request）
	 */
	<T> T execute(String serviceId, LoadBalancerRequest<T> request) throws IOException;

	/**
	 * Executes request using a ServiceInstance from the LoadBalancer for the specified
	 * service.
	 */
	<T> T execute(String serviceId, ServiceInstance serviceInstance,
			LoadBalancerRequest<T> request) throws IOException;

	/**
	 * Creates a proper URI with a real host and port for systems to utilize. Some systems
	 * use a URI with the logical service name as the host, such as
	 * http://myservice/path/to/service. This will replace the service name with the
	 * host:port from the ServiceInstance.
     * 
     * 创建一个带有真实host和port的URI以供系统使用。
     * 一些系统使用带有逻辑服务名称作为host的URI，比如：
     * http://myservice/path/to/service. 该方法将用ServiceInstance中的host:port来替换逻辑服务名称（myservice）
     * 
	 */
	URI reconstructURI(ServiceInstance instance, URI original);

}

```

顺带再看一下netflix-ribbon的IClient接口定义
```java
package com.netflix.client;
import com.netflix.client.config.IClientConfig;
/**
 * A client that can execute a single request. 
 * 
 */
public interface IClient<S extends ClientRequest, T extends IResponse> {

	/**
	 * Execute the request and return the response. It is expected that there is no retry and all exceptions
     * are thrown directly.
	 */
    public T execute(S request, IClientConfig requestConfig) throws Exception; 
}
```

###1.2: 接口ServiceInstanceChooser
该接口的方法定义在netflix-ribbon种也有定义，在ILoadBalancer接口中。  
```java
/**
 * Implemented by classes which use a load balancer to choose a server to send a request
 * to.
 * 由使用负载均衡器选择服务器以将请求发送到的类实现
 */
public interface ServiceInstanceChooser {

    /**
     * Chooses a ServiceInstance from the LoadBalancer for the specified service.
     * 根据给定的serviceId从负载均衡器中选择一个ServiceInstance
     */
    ServiceInstance choose(String serviceId);

}
```

###1.3：Requst的封装接口LoadBalancerRequest
类似于Netflix-ribbon中的ClientRequest接口。 
```java
/**
 * Simple interface used by LoadBalancerClient to apply metrics or pre and post actions
 * around load balancer requests.
 *
 * LoadBalancerClient使用的简单接口，用于围绕负载均衡器请求应用度量或者前后操作。
 */
public interface LoadBalancerRequest<T> {

    T apply(ServiceInstance instance) throws Exception;

}
```

###1.4: 服务信息封装接口ServiceInstance
该服务信息封装实例再netflix-ribbon中就是Server类。
```java
/**
 * Represents an instance of a service in a discovery system.
 * 表示服务发现系统中一个服务实例。
 */
public interface ServiceInstance {

	/**
	 * @return The unique instance ID as registered.
	 */
	default String getInstanceId() {
		return null;
	}

	/**
	 * @return The service ID as registered.
	 */
	String getServiceId();

	/**
	 * @return The hostname of the registered service instance.
	 */
	String getHost();

	/**
	 * @return The port of the registered service instance.
	 */
	int getPort();

	/**
	 * @return Whether the port of the registered service instance uses HTTPS.
	 */
	boolean isSecure();

	/**
	 * @return The service URI address.
	 */
	URI getUri();

	/**
	 * @return The key / value pair metadata associated with the service instance.
	 */
	Map<String, String> getMetadata();

	/**
	 * @return The scheme of the service instance.
	 */
	default String getScheme() {
		return null;
	}

}
```
总结：
根据这些接口定义，其实可以看出来大概的实现思路：
1. 执行execute(String serviceId, LoadBalancerRequest<T> request)
2. 根据serviceId执行choose(String serviceId)方法来获取到服务实例ServiceInstance
   - 选择服务怎么选择呢？用什么算法呢？这个就是ribbon自带的功能了（IRule接口，有round、random等）
3. 将choose的ServiceInstance传递给LoadBalancerRequest.apply(ServiceInstance instance)方法执行具体的网络调用并返回响应数据。

##2：LoadBalancerClient基于ribbon的实现类-RibbonLoadBalancerClient

###2.1： chose相关逻辑代码
其实就是调用netflix-ribbon的ILoadBalancer.chooseServer方法来选择服务。
```java
public class RibbonLoadBalancerClient implements LoadBalancerClient {
    
    @Override
    public ServiceInstance choose(String serviceId) {
        return choose(serviceId, null);
    }

    /**
     * New: Select a server using a 'key'.
     * @param serviceId of the service to choose an instance for
     * @param hint to specify the service instance
     * @return the selected {@link ServiceInstance}
     */
    public ServiceInstance choose(String serviceId, Object hint) {
        Server server = getServer(getLoadBalancer(serviceId), hint);
        if (server == null) {
            return null;
        }
        return new RibbonServer(serviceId, server, isSecure(server, serviceId),
                serverIntrospector(serviceId).getMetadata(server));
    }

    protected ILoadBalancer getLoadBalancer(String serviceId) {
        return this.clientFactory.getLoadBalancer(serviceId);
    }

    //Server是Netflix-ribbon中对于服务信息的封装类
    protected Server getServer(ILoadBalancer loadBalancer, Object hint) {
        if (loadBalancer == null) {
            return null;
        }
        // Use 'default' on a null hint, or just pass it on?
        return loadBalancer.chooseServer(hint != null ? hint : "default");
    }
}
```

###2.2： execute相关逻辑
```java
public class RibbonLoadBalancerClient implements LoadBalancerClient {
    
    @Override
    public <T> T execute(String serviceId, LoadBalancerRequest<T> request)
            throws IOException {
        return execute(serviceId, request, null);
    }

    /**
     * New: Execute a request by selecting server using a 'key'. The hint will have to be
     * the last parameter to not mess with the `execute(serviceId, ServiceInstance,
     * request)` method. This somewhat breaks the fluent coding style when using a lambda
     * to define the LoadBalancerRequest.
     * @param <T> returned request execution result type
     * @param serviceId id of the service to execute the request to
     * @param request to be executed
     * @param hint used to choose appropriate {@link Server} instance
     * @return request execution result
     * @throws IOException executing the request may result in an {@link IOException}
     */
    public <T> T execute(String serviceId, LoadBalancerRequest<T> request, Object hint)
            throws IOException {
        //通过netflix-ribbon的类获取到服务信息（Server）
        ILoadBalancer loadBalancer = getLoadBalancer(serviceId);
        Server server = getServer(loadBalancer, hint);
        if (server == null) {
            throw new IllegalStateException("No instances available for " + serviceId);
        }
        
        //将Server封装为Spring的RibbonServer
        RibbonServer ribbonServer = new RibbonServer(serviceId, server,
                isSecure(server, serviceId),
                serverIntrospector(serviceId).getMetadata(server));

        return execute(serviceId, ribbonServer, request);
    }

    @Override
    public <T> T execute(String serviceId, ServiceInstance serviceInstance,
                         LoadBalancerRequest<T> request) throws IOException {
        Server server = null;
        if (serviceInstance instanceof RibbonServer) {
            server = ((RibbonServer) serviceInstance).getServer();
        }
        if (server == null) {
            throw new IllegalStateException("No instances available for " + serviceId);
        }

        RibbonLoadBalancerContext context = this.clientFactory
                .getLoadBalancerContext(serviceId);
        RibbonStatsRecorder statsRecorder = new RibbonStatsRecorder(context, server);

        try {
            //具体的执行过程就是在LoadBalancerRequest里了
            T returnVal = request.apply(serviceInstance);
            statsRecorder.recordStats(returnVal);
            return returnVal;
        }
        // catch IOException and rethrow so RestTemplate behaves correctly
        catch (IOException ex) {
            statsRecorder.recordStats(ex);
            throw ex;
        }
        catch (Exception ex) {
            statsRecorder.recordStats(ex);
            ReflectionUtils.rethrowRuntimeException(ex);
        }
        return null;
    }
}
```

