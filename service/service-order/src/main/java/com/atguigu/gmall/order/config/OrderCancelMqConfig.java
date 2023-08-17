package com.atguigu.gmall.order.config;

import com.atguigu.gmall.common.constant.MqConst;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.CustomExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * @author 24657
 * @apiNote
 * @date 2023/8/15 20:53
 */
@Configuration
public class OrderCancelMqConfig {
    @Bean
    public Queue cancelDelayQueue(){
        return new Queue(MqConst.QUEUE_ORDER_CANCEL,true);
    }
    @Bean
    public CustomExchange cancelDelayExchange(){
        Map<String,Object> args=new HashMap<>();
        args.put("x-delayed-type", "direct");
        return new CustomExchange(MqConst.EXCHANGE_DIRECT_ORDER_CANCEL,"x-delayed-message",true,false,args);
    }
    @Bean
    public Binding delayQueueToExchange(){
        //noargs()可以将其转换成bindind类型
        return BindingBuilder.bind(cancelDelayQueue()).to(cancelDelayExchange()).with(MqConst.ROUTING_ORDER_CANCEL).noargs();
    }
}
