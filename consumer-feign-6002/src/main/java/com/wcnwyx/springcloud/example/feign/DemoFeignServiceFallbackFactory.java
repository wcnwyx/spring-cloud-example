package com.wcnwyx.springcloud.example.feign;

import feign.hystrix.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class DemoFeignServiceFallbackFactory implements FallbackFactory<DemoFeignService> {
    @Override
    public DemoFeignService create(Throwable throwable) {
        return new DemoFeignService() {
            @Override
            public String demoCall() {
                return "this is error msg. from FallbackFactory";
            }
        };
    }
}
