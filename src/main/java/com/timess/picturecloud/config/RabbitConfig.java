package com.timess.picturecloud.config;

import com.timess.picturecloud.bizmq.MqConstant;
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author 33363
 */
@Configuration
public class RabbitConfig {
    @Bean
    public Queue cloudQueue() {
        // true表示持久化
        return new Queue(MqConstant.QUEUE_NAME, true);
    }

    @Bean
    public Exchange cloudExchange(){
        return new DirectExchange(MqConstant.EXCHANGE_NAME, true, false, null);
    }
    // 将队列绑定到交换机，并指定路由键
    @Bean
    public Binding bindingCloudQueueToExchange() {
        return BindingBuilder
                // 绑定队列
                .bind(cloudQueue())
                // 到交换机
                .to(cloudExchange())
                // 使用指定的路由键
                .with(MqConstant.ROUTING_KEY).noargs();
    }
}