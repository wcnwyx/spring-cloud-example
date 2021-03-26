package com.wcnwyx.springcloud.example.feign;

import com.wcnwyx.springcloud.example.api.Contributor;
import com.wcnwyx.springcloud.example.api.DemoFeignService;
import com.wcnwyx.springcloud.example.api.GithubFeignService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class FeignController {

    @Autowired
    private DemoFeignService demoFeignService;

    @Autowired
    private GithubFeignService githubFeignService;

    @RequestMapping("/demo")
    public String demo(){
        return demoFeignService.demoCall();
    }

    @RequestMapping("/github/{owner}/{repo}")
    public String github(@PathVariable("owner") String owner, @PathVariable("repo") String repo){
        List<Contributor> list = githubFeignService.contributors(owner, repo);
        StringBuilder sb = new StringBuilder();
        list.forEach((e)->sb.append("login:"+e.getLogin()).append("; num:"+e.getContributions()).append("<br>"));
        return sb.toString();
    }
}
