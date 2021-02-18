package com.wcnwyx.springcloud.example.feign;

import com.wcnwyx.springcloud.example.api.DemoFeignService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FeignController {

    @Autowired
    private DemoFeignService demoFeignService;

    @RequestMapping("/demo")
    public String demo(){
        return demoFeignService.demoCall();
    }
}
