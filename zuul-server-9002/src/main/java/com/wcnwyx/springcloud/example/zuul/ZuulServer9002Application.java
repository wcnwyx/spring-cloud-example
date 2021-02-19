package com.wcnwyx.springcloud.example.zuul;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;

@SpringBootApplication
@EnableEurekaClient
@EnableZuulProxy
public class ZuulServer9002Application {
    public static void main(String[] args) {
        SpringApplication.run(ZuulServer9002Application.class, args);
    }
}
