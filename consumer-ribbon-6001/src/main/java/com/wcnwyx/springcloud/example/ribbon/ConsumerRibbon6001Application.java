package com.wcnwyx.springcloud.example.ribbon;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

//@EnableEurekaClient
@SpringBootApplication(scanBasePackages = {"com.wcnwyx.springcloud.example.ribbon"})
@EnableDiscoveryClient
public class ConsumerRibbon6001Application {
    public static void main(String[] args) {
        SpringApplication.run(ConsumerRibbon6001Application.class);
    }

    @Bean
    @LoadBalanced
    public RestTemplate restTemplate(){
        return new RestTemplate();
    }
}
