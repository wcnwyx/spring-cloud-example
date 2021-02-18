package com.wcnwyx.springcloud.example.ribbon;

import com.netflix.loadbalancer.IRule;
import com.netflix.loadbalancer.RandomRule;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.netflix.hystrix.EnableHystrix;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@EnableEurekaClient
@EnableHystrix
@SpringBootApplication
public class ConsumerRibbon6001Application {
    public static void main(String[] args) {
        SpringApplication.run(ConsumerRibbon6001Application.class, args);
    }

    @Bean
    @LoadBalanced //该RestTemplate配置一个LoadBalancerClient，用于解析http://service-name/uri 中的service-name，并实现负载策略
    public RestTemplate restTemplate(){
        return new RestTemplate();
    }

    //默认使用的负载策略是轮询（Round），可以通过此方法自定义修改
    @Bean
    public IRule irule(){
        return new RandomRule();
    }
}
