package com.timess.picturecloud.config;

import lombok.Data;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 初始化redis
 * @author 33363
 */
@Configuration
@ConfigurationProperties(prefix = "spring.redis")
@Data
public class RedissonConfig {
    /**
     * redis库号
     */
    private Integer database;

    /**
     * 主机号
     */
    private String host;

    /**
     * 端口号
     */
    private String port;

    /**
     * redis密码
     */
    private String password;

    @Bean
    public RedissonClient getRedissonClient(){
        // 1. 创建配置对象
        Config config = new Config();
        // 2. 添加单机配置
        config.useSingleServer()
        // 3. 设置数据库
                .setDatabase(database)
        // 4. 设置redis地址
                .setAddress("redis://" + host + ":" + port)
        // 5.设置redis的密码
                .setPassword(password);
        //创建 Redisson 实例
        return Redisson.create(config);
    }

}
