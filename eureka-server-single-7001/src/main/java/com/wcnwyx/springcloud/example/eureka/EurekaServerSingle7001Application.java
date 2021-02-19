package com.wcnwyx.springcloud.example.eureka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@EnableEurekaServer
@SpringBootApplication
public class EurekaServerSingle7001Application {
    public static void main(String[] args) {
        SpringApplication.run(EurekaServerSingle7001Application.class, args);
    }
}
