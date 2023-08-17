package com.atguigu.gmall.mq.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * @author 24657
 * @apiNote
 * @date 2023/8/15 18:53
 */
@Configuration
public class DelayedMqConfig {    public static final String exchange_delay = "exchange.delay";
    public static final String routing_delay = "routing.delay";
    public static final String queue_delay_1 = "queue.delay.1";

    @Bean
    public Queue delayQueue1(){
        return new Queue(queue_delay_1,true);
    }
    @Bean
    public CustomExchange delayExchange(){
        Map<String,Object> args=new HashMap<>();
        //这里的类型指的是你的交换机的类型
        args.put("x-delayed-type","direct");
        //这个类型指的是你用的那个插件的类型
        return new CustomExchange(exchange_delay,"x-delayed-message",true,false,args);
    }
    @Bean
    public Binding bindingDelayQueueToDelayExchange(){
        return BindingBuilder.bind(delayQueue1()).to(delayExchange()).with(routing_delay).noargs();
    }

}
