package com.wcnwyx.springcloud.example.api;

import org.springframework.stereotype.Component;

@Component
public class DemoFeignServiceFallback implements DemoFeignService{
    @Override
    public String demoCall() {
        return "this is error msg. from Fallback";
    }
}
