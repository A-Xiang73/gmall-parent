package com.atguigu.gmall.common.config;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.pojo.GmallCorrelationData;
import com.rabbitmq.client.ConfirmCallback;
import com.rabbitmq.client.ReturnCallback;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author 24657
 * @apiNote
 * confirmcallback只确认消息是否正确的到达exchange中
 * returncallback消息没有正确的到达队列时触发回调 正确则不会执行
 *  * 1. 如果消息没有到exchange,则confirm回调,ack=false
 *  * 2. 如果消息到达exchange,则confirm回调,ack=true
 *  * 3. exchange到queue成功,则不回调return
 *  * 4. exchange到queue失败,则回调return
 * @date 2023/8/14 21:28
 */
@Component
@Slf4j
public class MQProducerAckConfig implements RabbitTemplate.ConfirmCallback, RabbitTemplate.ReturnCallback {
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private RedisTemplate redisTemplate;
    /*
    * @postConstruct 修饰一个非静态的void方法，在服务器加载Servlet的时候运行，并且只会被服务器执行一次
    * 在构造函数之后执行
    * @PostConstruct是Java自带的注解，在方法上加该注解会在项目启动的时候执行该方法，也可以理解为在spring容器初始化的时候执行该方法
    * */
    @PostConstruct
    public void  init(){
        rabbitTemplate.setConfirmCallback(this);//指定confirmcallback
        rabbitTemplate.setReturnCallback(this);
    }
    @Override
    public void confirm(CorrelationData correlationData, boolean ack, String cause) {
        //发送到交换机
        if(ack){
            log.info("消息发送成功:"+ JSON.toJSONString(correlationData));
        }else{
            log.info("消息发送失败：" + cause + " 数据：" + JSON.toJSONString(correlationData));
            retrySendMsg(correlationData);
        }
    }

    /*
    *  * returncallback消息没有正确的到达队列时触发回调 正确则不会执行
     *  * 1. 如果消息没有到exchange,则confirm回调,ack=false
     *  * 2. 如果消息到达exchange,则confirm回调,ack=true
     *  * 3. exchange到queue成功,则不回调return
     *  * 4. exchange到queue失败,则回调return
     * 相当于详细没有正确到达队列则会执行这个方法
    * */
    @Override
    public void returnedMessage(Message message, int replyCode, String replyText, String exchange, String routingKey) {
        // 反序列化对象输出
        System.out.println("消息主体: " + new String(message.getBody()));
        System.out.println("应答码: " + replyCode);
        System.out.println("描述：" + replyText);
        System.out.println("消息使用的交换器 exchange : " + exchange);
        System.out.println("消息使用的路由键 routing : " + routingKey);
        //获取消息id
        String correlationDataId  = (String) message.getMessageProperties().getHeaders().get("spring_returned_message_correlation");
        String strJson  = (String) this.redisTemplate.opsForValue().get(correlationDataId);
        //将json字符串转换成gmallCorrelationData对象
        GmallCorrelationData gmallCorrelationData = JSON.parseObject(strJson, GmallCorrelationData.class);
        //重新发送消息
        retrySendMsg(gmallCorrelationData);
    }

    //通过redis来实现重发机制
    public void retrySendMsg(CorrelationData correlationData) {
        GmallCorrelationData gmallCorrelationData = (GmallCorrelationData) correlationData;
        //获取重试次数
        int retryCount = gmallCorrelationData.getRetryCount();
        if (retryCount > 3) {
            log.error("重试次数已到，发送消息失败:" + JSON.toJSONString(gmallCorrelationData));
        } else {
            //重新发送
            retryCount++;
            gmallCorrelationData.setRetryCount(retryCount);
            System.out.println("重试次数：\t" + retryCount);
            if (gmallCorrelationData.isDelay()) {
                //是延时消息
                this.redisTemplate.opsForValue().set(gmallCorrelationData.getId(),JSON.toJSONString(gmallCorrelationData),10,TimeUnit.MINUTES);
                //重新发送
                this.rabbitTemplate.convertAndSend(gmallCorrelationData.getExchange(), gmallCorrelationData.getRoutingKey(), gmallCorrelationData.getMessage(), new MessagePostProcessor() {
                    @Override
                    public Message postProcessMessage(Message message) throws AmqpException {
                        message.getMessageProperties().setDelay(gmallCorrelationData.getDelayTime()*1000);
                        return message;
                    }
                }, gmallCorrelationData);
            }else {
                //不是延时消息
                //更新缓存中的数据
                this.redisTemplate.opsForValue().set(gmallCorrelationData.getId(), JSON.toJSONString(gmallCorrelationData), 10, TimeUnit.MINUTES);
                //重新发送
                this.rabbitTemplate.convertAndSend(gmallCorrelationData.getExchange(), gmallCorrelationData.getRoutingKey(), gmallCorrelationData.getMessage(), gmallCorrelationData);
            }
        }
    }
}