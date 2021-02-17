package com.wcnwyx.springcloud.example.feign;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.netflix.hystrix.EnableHystrix;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableEurekaClient
@EnableFeignClients(basePackages = {"com.wcnwyx.springcloud.example.api"})
@EnableHystrix
@SpringBootApplication(scanBasePackages = {"com.wcnwyx.springcloud.example.feign"})
public class ConsumerFeign6002Application {
    public static void main(String[] args) {
        SpringApplication.run(ConsumerFeign6002Application.class, args);
    }
}
