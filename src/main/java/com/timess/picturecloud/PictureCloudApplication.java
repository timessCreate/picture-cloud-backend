package com.timess.picturecloud;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@MapperScan("com.timess.picturecloud.mapper")
@EnableAspectJAutoProxy(exposeProxy = true)
public class PictureCloudApplication {
    public static void main(String[] args) {
        SpringApplication.run(PictureCloudApplication.class, args);
    }

}
