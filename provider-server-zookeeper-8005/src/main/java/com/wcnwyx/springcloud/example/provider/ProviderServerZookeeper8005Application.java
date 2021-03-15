package com.wcnwyx.springcloud.example.provider;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class ProviderServerZookeeper8005Application {
    public static void main(String[] args) {
        SpringApplication.run(ProviderServerZookeeper8005Application.class, args);
    }
}
