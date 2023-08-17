package com.atguigu.gmall.order.receiver;

import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.model.enums.ProcessStatus;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.order.service.OrderService;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author 24657
 * @apiNote
 * @date 2023/8/15 21:08
 */
@Component
public class OrderReceiver {
    @Autowired
    private OrderService orderService;
    //监听消息
    @SneakyThrows
    @RabbitListener(queues = MqConst.QUEUE_ORDER_CANCEL)
    public void cancleOrder(Long orderId, Message message, Channel channel){
        try {
            if (orderId != null) {
              OrderInfo orderInfo=orderService.getOrderInfo(orderId);
                if (orderInfo != null&&"UNPAID".equals(orderInfo.getOrderStatus())&&"UNPAID".equals(orderInfo.getProcessStatus())) {
                    //该条件下关闭订单
                    orderService.execExpiredOrder(orderId);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_WARE_STOCK),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_WARE_STOCK),
            key = {MqConst.ROUTING_WARE_STOCK}
    ))
    public void upOrder(Long orderId ,Message message,Channel channel ){
        OrderInfo orderInfo = orderService.getOrderInfo(orderId);
        if (orderInfo != null&& ProcessStatus.UNPAID.equals(orderInfo.getOrderStatus())) {
            orderService.updateOrderStatus(orderId,ProcessStatus.PAID);
            //发送消息通知仓库扣减库存
        }
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }
}
