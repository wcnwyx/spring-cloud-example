package com.wcnwyx.springcloud.example.eureka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@EnableEurekaServer //通过该注解开通EurekaServer服务
@SpringBootApplication
public class EurekaServer7002Application {
    public static void main(String[] args) {
        new SpringApplication(EurekaServer7002Application.class).run(args);
    }
}
