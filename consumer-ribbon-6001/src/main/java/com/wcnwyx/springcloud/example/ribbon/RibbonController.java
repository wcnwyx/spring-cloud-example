package com.wcnwyx.springcloud.example.ribbon;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@RestController
public class RibbonController {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private DiscoveryClient discoveryClient;

    @RequestMapping("/demo")
    @HystrixCommand(fallbackMethod = "demoFallback")//单个方法通过该注解来实现熔断
    public String demo(){
        return restTemplate.getForObject("http://provider-server/demo", String.class);
    }

    @RequestMapping("/discoveryList")
    public String discoveryList(){
        StringBuilder stringBuilder = new StringBuilder();
        for(String service:discoveryClient.getServices()){
            List<ServiceInstance> list = discoveryClient.getInstances(service);
            list.forEach(item->{stringBuilder.append(item.getServiceId()+"|"+item.getHost()+"|"+item.getPort()+"|"+item.getInstanceId()).append("\r\n");});
        }
        System.out.println(stringBuilder.toString());
        return stringBuilder.toString();
    }

    public String demoFallback(){
        return "this is fallback";
    }
}
