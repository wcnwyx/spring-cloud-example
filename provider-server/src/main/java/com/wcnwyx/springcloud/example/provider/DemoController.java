package com.wcnwyx.springcloud.example.provider;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DemoController {

    @Autowired
    private DiscoveryClient discoveryClient;

    @RequestMapping("/demo")
    public String demo(){
        for(String serviceId:discoveryClient.getServices()){
            System.out.println("serviceId:"+serviceId);
            System.out.println(discoveryClient.getInstances(serviceId));
        }
        return "This is Demo";
    }
}
