package com.wcnwyx.springcloud.example.api;

import com.google.gson.Gson;
import feign.Response;
import feign.codec.Decoder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import java.io.Reader;
import java.lang.reflect.Type;
//此处不能添加@Configuration，因为是gitHub该FeignClient所私有的，如果加了注解，就会被spring自身容器加载
public class GitHubConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public Decoder feignDecoder() {
        return (Response response, Type type)->{
            System.out.println("i am custom Decoder!");
            if (response.body() == null)
                return null;
            Reader reader = response.body().asReader();
            try {
                Gson gson = new Gson();
                return gson.fromJson(reader, type);
            } catch (Exception e) {
                return null;
            } finally {
                reader.close();
            }
        };
    }
}
