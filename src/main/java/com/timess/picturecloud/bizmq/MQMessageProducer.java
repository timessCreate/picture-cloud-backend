package com.timess.picturecloud.bizmq;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author xing10
 */
@Component
public class MQMessageProducer {
    @Resource
    private RabbitTemplate rabbitTemplate;

    /**
     * 向rabbit发送消息的方法
     * @param message 消息内容
     */
    public void sendMessage(String message){
        //使用rabbitTemplate的方法将消息发送到指定的交换机和路由键
        rabbitTemplate.convertAndSend(MqConstant.EXCHANGE_NAME, MqConstant.ROUTING_KEY, message);
    }
}
