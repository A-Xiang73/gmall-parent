package com.atguigu.gmall.mq.receiver;

import com.atguigu.gmall.mq.config.DeadLetterConfig;
import com.atguigu.gmall.mq.config.DelayedMqConfig;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import org.checkerframework.checker.units.qual.C;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * @author 24657
 * @apiNote 消息确认接收
 * @date 2023/8/14 23:14
 */
@Component
public class ConfirmReceiver {
    @Autowired
    private RedisTemplate redisTemplate;


    /*
    * @SneakyThrows 相当于在外面加一层trycatch
    * */
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "queue.confirm",autoDelete = "true"),
            exchange = @Exchange(value = "exchange.confirm",autoDelete = "true"),
            key = {"routing.confirm"}
    ))
    public  void process(Message message, Channel channel){
        String mess = new String(message.getBody());
        System.out.println(mess);
        //手动对消息进行确认
        /*
        * 第一个参数一个可以代表消息的数据相当于id 第二个参数 是否进行批量确认
        * */
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }
    //处理死信消息
    @RabbitListener(queues = DeadLetterConfig.queue_dead_2)
    public  void recieve(String msg){
        System.out.println("msg:"+msg);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        System.out.println(simpleDateFormat.format(new Date())+"消息已接收");
    }
//    @RabbitListener(queues = DelayedMqConfig.queue_delay_1)
//    public  void cc(String msg){
//        System.out.println("msg:"+msg);
//        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//        System.out.println(simpleDateFormat.format(new Date())+"消息已接收");
//    }
    //处理延时消息
    @SneakyThrows
    @RabbitListener(queues = DelayedMqConfig.queue_delay_1)
    public void delayRecieve(String msg,Message message,Channel channel){
        //解决消息幂等性问题
        String tag ="delay:"+ message.getMessageProperties().getDeliveryTag();
        Boolean result = redisTemplate.opsForValue().setIfAbsent(tag, "0",10, TimeUnit.MINUTES);
        if (result) {
            //result=true 说明是第一次消费
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            System.out.println("msg:"+msg+"接收时间："+simpleDateFormat.format(new Date()));
            //1表示已经消费确认
            redisTemplate.opsForValue().set(tag,"1");
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
        }else {
            //说明不是第一次进 直接进行确认
            String value = (String) redisTemplate.opsForValue().get(tag);
            if ("0".equals(value)){
                //还未消费之前出现了异常 重新进行消费
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                System.out.println("msg:"+msg+"接收时间："+simpleDateFormat.format(new Date()));
                //1表示已经消费确认
                redisTemplate.opsForValue().set(tag,"1");
            }
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
        }
    }

    //解决幂等性问题
//    @RabbitListener(queues = DelayedMqConfig.queue_delay_1)
//    public void  processDelay(String msg)
//    {
//        String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
//        System.out.println("msg:"+msg+time);
//    }
}
