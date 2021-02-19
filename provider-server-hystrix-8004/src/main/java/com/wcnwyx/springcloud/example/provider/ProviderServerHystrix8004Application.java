package com.wcnwyx.springcloud.example.provider;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.netflix.hystrix.EnableHystrix;

@EnableEurekaClient
@EnableHystrix
@SpringBootApplication
public class ProviderServerHystrix8004Application {
    public static void main(String[] args) {
        SpringApplication.run(ProviderServerHystrix8004Application.class, args);
    }
}
