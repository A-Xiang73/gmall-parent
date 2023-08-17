package com.atguigu.gmall.common.pojo;

import lombok.Data;
import org.springframework.amqp.rabbit.connection.CorrelationData;

/**
 * @author 24657
 * @apiNote
 * @date 2023/8/14 23:45
 */
@Data
public class GmallCorrelationData extends CorrelationData {
    //  消息主体
    private Object message;
    //  交换机
    private String exchange;
    //  路由键
    private String routingKey;
    //  重试次数
    private int retryCount = 0;
    //  消息类型  是否是延迟消息
    private boolean isDelay = false;
    //  延迟时间
    private int delayTime = 10;
}
