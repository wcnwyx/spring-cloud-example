package com.wcnwyx.springcloud.example.ribbon;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
public class RibbonController {

    @Autowired
    private RestTemplate restTemplate;

    @RequestMapping("/demo")
    @HystrixCommand(fallbackMethod = "demoFallback")//单个方法通过该注解来实现熔断
    public String demo(){
        return restTemplate.getForObject("http://provider-server/demo", String.class);
    }

    public String demoFallback(){
        return "this is fallback";
    }
}
