package com.atguigu.gmall.mq.controller;

import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.mq.config.DeadLetterConfig;
import com.atguigu.gmall.mq.config.DelayedMqConfig;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author 24657
 * @apiNote
 * @date 2023/8/14 23:11
 */
@RestController
@RequestMapping("/mq")
public class MqController {
    @Autowired
    private RabbitService rabbitService;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    /**
     * 消息发送
     */
    //http://localhost:8282/mq/sendDeadLetter
    @GetMapping("sendConfirm")
    public Result sendConfirm() {

        rabbitService.sendMessage("exchange.confirm", "routing.confirm", "来人了，开始接客吧！");
        return Result.ok();
    }
    //发送死信队列
    @GetMapping("sendDeadLetter")
    public Result sendDeadLetter(){
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        rabbitTemplate.convertAndSend(DeadLetterConfig.exchange_dead,DeadLetterConfig.routing_dead_1,"okkey");
        System.out.println("info already send");
        System.out.println(simpleDateFormat.format(new Date()));
        return Result.ok();
    }
    //发送延时消息
    @GetMapping("sendDelay")
    public Result sendDelay() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        System.out.println("发送消息的时间为："+simpleDateFormat.format(new Date()));
        rabbitService.sendDelayMsg(DelayedMqConfig.exchange_delay,DelayedMqConfig.routing_delay,"我是延时消息",10);
        return Result.ok();
    }
}
