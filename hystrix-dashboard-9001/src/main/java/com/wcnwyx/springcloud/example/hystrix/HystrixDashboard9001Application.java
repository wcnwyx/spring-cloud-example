package com.wcnwyx.springcloud.example.hystrix;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.hystrix.dashboard.EnableHystrixDashboard;

@EnableHystrixDashboard
@SpringBootApplication
public class HystrixDashboard9001Application {
    public static void main(String[] args) {
        SpringApplication.run(HystrixDashboard9001Application.class, args);
    }
}
