package com.wcnwyx.springcloud.example.ribbon;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
public class RibbonController {

    @Autowired
    private RestTemplate restTemplate;

    @RequestMapping("/demo")
    public String demo(){
        return restTemplate.getForObject("http://provider-server/demo", String.class);
    }
}
