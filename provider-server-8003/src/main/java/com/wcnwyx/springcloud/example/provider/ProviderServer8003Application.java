package com.wcnwyx.springcloud.example.provider;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

@EnableEurekaClient
@SpringBootApplication(scanBasePackages = {"com.wcnwyx.springcloud.example.provider"})
public class ProviderServer8003Application {
    public static void main(String[] args) {
        SpringApplication.run(ProviderServer8003Application.class);
    }
}
