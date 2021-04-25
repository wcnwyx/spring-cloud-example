package com.wcnwyx.springcloud.example.feign;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
//import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
//import org.springframework.cloud.netflix.hystrix.EnableHystrix;
import org.springframework.cloud.openfeign.EnableFeignClients;

//@EnableEurekaClient
//当feignClient不在该包目录及其子目录下是，通过EnableFeignClients的basePackages来指定目录
//feignClient里定义的fallback或者fallbackFactory也需要通过scanBasePackages来指定目录扫描
@EnableFeignClients(basePackages = {"com.wcnwyx.springcloud.example.api"})
//@EnableHystrix
@SpringBootApplication(scanBasePackages = {"com.wcnwyx.springcloud.example.api","com.wcnwyx.springcloud.example.feign"})
public class ConsumerFeign6002Application {
    public static void main(String[] args) {
        SpringApplication.run(ConsumerFeign6002Application.class, args);
    }
}
