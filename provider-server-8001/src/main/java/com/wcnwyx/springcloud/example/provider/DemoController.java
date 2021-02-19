package com.wcnwyx.springcloud.example.provider;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DemoController {

    @Value("${server.port}")
    private int port;

    @RequestMapping("/demo")
    public String demo(){
        return "This is Demo. port="+port;
    }
}
