package com.wcnwyx.springcloud.example.api;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;

@FeignClient(name = "provider-server", fallbackFactory = DemoFeignServiceFallbackFactory.class)
//@FeignClient(name = "provider-server", fallback= DemoFeignServiceFallback.class)
public interface DemoFeignService {

    @RequestMapping(value = "/demo")
    String demoCall();
}
