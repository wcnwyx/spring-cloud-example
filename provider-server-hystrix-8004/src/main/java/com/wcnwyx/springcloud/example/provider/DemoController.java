package com.wcnwyx.springcloud.example.provider;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Random;

@RestController
public class DemoController {

    @Value("${server.port}")
    private int port;

    @RequestMapping("/demo")
    @HystrixCommand(fallbackMethod = "demoFallback")
    public String demo(){
        int i = new Random().nextInt(2);
        if(i==0){
            return "This is Demo. port="+port;
        }else{
            throw new RuntimeException();
        }
    }

    public String demoFallback(){
        return "this is HystrixCommand fallback.";
    }
}
