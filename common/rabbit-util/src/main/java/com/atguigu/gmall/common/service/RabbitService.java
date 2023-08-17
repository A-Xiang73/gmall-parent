package com.atguigu.gmall.common.service;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.pojo.GmallCorrelationData;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author 24657
 * @apiNote
 * @date 2023/8/14 23:07
 */
@Service
public class RabbitService {
    @Autowired
    RabbitTemplate rabbitTemplate;
    @Autowired
    RedisTemplate redisTemplate;
    /**
     *  发送消息
     * @param exchange 交换机
     * @param routingKey 路由键
     */

    //通过redis来实现重发机制
    public Boolean sendMessage(String exchange,String routingKey, Object msg){
        GmallCorrelationData gmallCorrelationData = new GmallCorrelationData();
        //声明一个correlationId来标识信息的唯一性
        String correlationId  = UUID.randomUUID().toString().replace("-", "");
        gmallCorrelationData.setId(correlationId);
        gmallCorrelationData.setExchange(exchange);
        gmallCorrelationData.setRoutingKey(routingKey);
        gmallCorrelationData.setMessage(msg);
        //发送这个消息的时候将gmallCorrelationData对象放入缓存中
        redisTemplate.opsForValue().set(correlationId, JSON.toJSONString(gmallCorrelationData),30, TimeUnit.MINUTES);
        this.rabbitTemplate.convertAndSend(exchange,routingKey,msg,gmallCorrelationData);
        //默认返回true
        return true;
    }
    //封装发送延时消息
    public Boolean sendDelayMsg(String exchange,String routingKey, Object msg, int delayTime){
        GmallCorrelationData gmallCorrelationData = new GmallCorrelationData();
        //  声明一个correlationId的变量
        String correlationId = UUID.randomUUID().toString().replaceAll("-","");
        gmallCorrelationData.setId(correlationId);
        gmallCorrelationData.setExchange(exchange);
        gmallCorrelationData.setRoutingKey(routingKey);
        gmallCorrelationData.setMessage(msg);
        gmallCorrelationData.setDelayTime(delayTime);
        gmallCorrelationData.setDelay(true);
        //将数据封装到缓存
        redisTemplate.opsForValue().set(correlationId,JSON.toJSONString(gmallCorrelationData),30,TimeUnit.MINUTES);
        //发送延时消息
        this.rabbitTemplate.convertAndSend(exchange,routingKey,msg,new MessagePostProcessor() {
            @Override
            public Message postProcessMessage(Message message) throws AmqpException {
                //设置延时时间
                message.getMessageProperties().setDelay(delayTime*1000);
                return message;
            }
        },gmallCorrelationData);
        //默认返回
        return  true;
    }
}
