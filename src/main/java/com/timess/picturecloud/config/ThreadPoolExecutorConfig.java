package com.timess.picturecloud.config;

import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author 33363
 * 线程池配置类
 */
@Configuration
public class ThreadPoolExecutorConfig {

    /**
     * 生成一个线程池实例
     */
    @Bean
    public ThreadPoolExecutor threadPoolExecutor() {
        //创建线程工厂
        ThreadFactory threadFactory = new ThreadFactory() {
            //初始化线程数为:1
            private int count = 1;
            //当需要创建线程时，就调用该方法创建线程
            @Override
            public Thread newThread(@NotNull Runnable r) {
                Thread thread = new Thread(r);
                //线程命名，便于区分
                thread.setName("线程" + count);
                //线程数递增
                count++;
                return thread;
            }
        };
        //创建一个核心数为2， 最大数线程数为 4， 存活时间为100s, 自定义线程工厂，队列为阻塞队列， 拒绝策略默认为拒绝
       ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(2, 4, 100, TimeUnit.SECONDS,new ArrayBlockingQueue<>(100),threadFactory);
       return threadPoolExecutor;
    }

}
