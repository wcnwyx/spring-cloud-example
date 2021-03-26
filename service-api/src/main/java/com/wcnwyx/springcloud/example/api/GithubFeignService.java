package com.wcnwyx.springcloud.example.api;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@FeignClient(name = "github-service", url = "https://api.github.com", configuration = GitHubConfiguration.class)
public interface GithubFeignService {
    @RequestMapping(value = "/repos/{owner}/{repo}/contributors")
    List<Contributor> contributors(@PathVariable("owner") String owner, @PathVariable("repo") String repo);
}
