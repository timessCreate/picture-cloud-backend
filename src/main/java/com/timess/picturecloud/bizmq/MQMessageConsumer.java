package com.timess.picturecloud.bizmq;

import com.rabbitmq.client.Channel;
import com.timess.picturecloud.exception.ErrorCode;
import com.timess.picturecloud.exception.BusinessException;
import com.timess.picturecloud.manager.AiManager;
import com.timess.picturecloud.model.domain.Chart;
import com.timess.picturecloud.service.ChartService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;


/**
 * @author xing10
 */
@Component
@Slf4j
public class MQMessageConsumer {

    @Resource
    private ChartService chartService;

    @Resource
    private AiManager aiManager;

    /**
     * 接收消息的方法
     *
     * @param message      接收到的消息内容，是一个字符串类型
     * @param channel      消息所在的通道，可以通过该通道与 RabbitMQ 进行交互，例如手动确认消息、拒绝消息等
     * @param deliveryTag  消息的投递标签，用于唯一标识一条消息
     */
    // 使用@SneakyThrows注解简化异常处理
    @SneakyThrows
    // 使用@RabbitListener注解指定要监听的队列名称为"code_queue"，并设置消息的确认机制为手动确认
    @RabbitListener(queues = {MqConstant.QUEUE_NAME}, ackMode = "MANUAL")
    // @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag是一个方法参数注解,用于从消息头中获取投递标签(deliveryTag),
    // 在RabbitMQ中,每条消息都会被分配一个唯一的投递标签，用于标识该消息在通道中的投递状态和顺序。通过使用@Header(AmqpHeaders.DELIVERY_TAG)注解,可以从消息头中提取出该投递标签,并将其赋值给long deliveryTag参数。
    public void receiveMessage(String message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {

        log.info("receiveMessage message = {}", message);
        //其中的message 信息为 id
        if(StringUtils.isBlank(message)){
            //如果更新失败，拒绝当前消息，让消息重新进入队列
            channel.basicNack(deliveryTag, false, false);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "消息为空");
        }

        long chartId = Long.parseLong(message);
        //根据id查找图表
        Chart chart = chartService.getById(chartId);
        if(chart == null){
            //如果图表为空，拒绝信息并抛出异常
            channel.basicNack(deliveryTag, false, false);
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "数据中未查询到该图表");
        }

        //为了减少重复提交，优先将图表状态该为执行中
        Chart updateChart = new Chart();
        updateChart.setId(chart.getId());
        updateChart.setStatus("running");
        boolean b = chartService.updateById(updateChart);
        if(!b){
            //如果更新图表信息失败，拒绝消息并处理图表更新操作
            channel.basicNack(deliveryTag, false, false);
            handleChartUpdateError(updateChart.getId(), "更新图表执行中状态失败");
            return;
        }
        //调用AI接口
        // TODO:AI接口失效后更改
        String result = aiManager.invoke(BuildUserInput(chart));
        //对返回结果进行拆分，按照五个中括号进行拆分
        String [] splits = result.split("【【【【【");
        //对拆分部分进行校验
        if(splits.length < 3){
            handleChartUpdateError(chart.getId(), "AI生成错误");
        }
        //提取图表代码
        String genChart = splits[1].trim();
        //提取分析结论信息
        String genResult = splits[2].trim();
        //得到分析结果后，再次更新chart
        Chart updateChartResult = new Chart();
        updateChartResult.setId(chart.getId());
        updateChartResult.setGenChart(genChart);
        updateChartResult.setGenResult(genResult);
        updateChartResult.setStatus("succeed");
        //更新
        boolean updateResult = chartService.updateById(updateChartResult);
        if(! updateResult){
            handleChartUpdateError(chart.getId(), "更新图表状态为“succeed” 失败");
        }
        // 投递标签是一个数字标识,它在消息消费者接收到消息后用于向RabbitMQ确认消息的处理状态。通过将投递标签传递给channel.basicAck(deliveryTag, false)方法,可以告知RabbitMQ该消息已经成功处理,可以进行确认和从队列中删除。
        // 手动确认消息的接收，向RabbitMQ发送确认消息
        channel.basicAck(deliveryTag, false);
    }


    /**
     * 异常工具类
     */
    private void handleChartUpdateError(long charId, String execMessage){
        Chart updateChart = new Chart();
        updateChart.setId(charId);
        updateChart.setStatus("failed");
        updateChart.setExecMessage(execMessage);
        boolean updateResult = chartService.updateById(updateChart);
        if(!updateResult){
            log.error("更新图表状态为“failed” 失败" + charId  + "," + execMessage);
        }
    }

    private String BuildUserInput(Chart chart){
        String goal = chart.getGoal();
        String chartType = chart.getChartType();
        String csvData = chart.getChartData();

        // 构造用户输入
        StringBuilder userInput = new StringBuilder();
        userInput.append("分析需求：").append("\n");

        // 拼接分析目标
        String userGoal = goal;
        if (StringUtils.isNotBlank(chartType)) {
            userGoal += "，请使用" + chartType;
        }
        userInput.append(userGoal).append("\n");
        userInput.append("原始数据：").append("\n");
        userInput.append(csvData).append("\n");

        return userInput.toString();
    }
}

