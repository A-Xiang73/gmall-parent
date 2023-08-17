package com.atguigu.gmall.mq.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * @author 24657
 * @apiNote
 * @date 2023/8/15 17:58
 */
//多个队列和交换机的关系无法用注解简单表示 需要配置注解类
@Configuration
public class DeadLetterConfig {
    public static final String exchange_dead = "exchange.dead";
    public static final String routing_dead_1 = "routing.dead.1";
    public static final String routing_dead_2 = "routing.dead.2";
    public static final String queue_dead_1 = "queue.dead.1";
    public static final String queue_dead_2 = "queue.dead.2";
    @Bean
    public DirectExchange deadExchange(){

        return new DirectExchange(exchange_dead,true,false,null);
    }
    @Bean
    public Queue queue1(){
        // 设置如果队列一 出现问题，则通过参数转到exchange_dead，routing_dead_2 上！
        Map<String, Object> map = new HashMap<>();
        //设置交换机
        map.put("x-dead-letter-exchange",exchange_dead);
        //设置路由  比如说交换机绑定了很多队列，该值决定发送到那个具体队列
        map.put("x-dead-letter-routing-key",routing_dead_2);
        //设置延迟时间
        map.put("x-message-ttl",10*1000);
        //exclusive是否排他的——允许其他使用
        return new Queue(queue_dead_1,true,false,false,map);
    }
    //将队列和交换机进行绑定
    @Bean
    public Binding bindingQueue1ToDeadExchange(){

        //通过路由将队列绑定到交换机上面
        return BindingBuilder.bind(queue1()).to(deadExchange()).with(routing_dead_1);
    }

    @Bean
    public Queue queue2(){

        return new Queue(queue_dead_2,true,false,false);
    }
    //绑定死信交换机和队列2
    @Bean
    public Binding bindingQueue2ToDeadExchange(){
        return BindingBuilder.bind(queue2()).to(deadExchange()).with(routing_dead_2);
    }
}
