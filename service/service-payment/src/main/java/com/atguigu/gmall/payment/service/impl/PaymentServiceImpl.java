package com.atguigu.gmall.payment.service.impl;

import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.model.enums.PaymentStatus;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.payment.PaymentInfo;
import com.atguigu.gmall.payment.mapper.PaymentInfoMapper;
import com.atguigu.gmall.payment.service.PaymentService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;

/**
 * @author 24657
 * @apiNote
 * @date 2023/8/15 23:13
 */
@Service
public class PaymentServiceImpl implements PaymentService {
    @Autowired
    private PaymentInfoMapper paymentInfoMapper;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private RabbitService rabbitService;

    /*
    * 根据orderId获取支付订单
    * */
    @Override
    public PaymentInfo getPaymentInfo(Long orderId) {

        return null;
    }

    //二次校验后调用的方法
    @Override
    public void paySuccess(String out_trade_no, String paymentType, Map<String, String> paramsMap) {
        try {
            //更新支付状态信息
            PaymentInfo paymentInfo = getPaymentInfo(out_trade_no, paymentType);
            updatePaymentInfo(paymentInfo,PaymentStatus.PAID.name(), paramsMap);
            //发送mq消息更新order模块orderinfo的状态 支付成功后有paymentinfo和orderinfo的信息以及库存信息信息需要进行修改
            rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_WARE_STOCK,MqConst.ROUTING_WARE_STOCK,paymentInfo.getOrderId());
            //发送mq消息扣减库存
        } catch (Exception e) {
            e.printStackTrace();
            //出现异常删除redis中的数据
            redisTemplate.delete(paramsMap.get("notify_id"));
        }
    }

    /*
    * 更新支付状态
    * */
    @Override
    public void updatePaymentInfo(PaymentInfo paymentInfo, String paid,Map<String, String> paramsMap) {
        paymentInfo.setPaymentStatus(paid);
        paymentInfo.setCallbackTime(new Date());
        paymentInfo.setCallbackContent(paramsMap.toString());
        paymentInfo.setTradeNo(paramsMap.get("trade_no"));
        paymentInfoMapper.updateById(paymentInfo);
    }

    /*
     * 更新支付状态
     * */
    @Override
    public void updatePaymentInfo(String status, String outTradeNo, String paymentType) {
        PaymentInfo paymentInfo = getPaymentInfo(outTradeNo, paymentType);
        paymentInfo.setPaymentStatus(status);
        paymentInfoMapper.updateById(paymentInfo);
    }

    /*
    * 根据out_trade_no获取支付账单
    * */
    @Override
    public PaymentInfo getPaymentInfo(String outTradeNo, String paymentType) {
        QueryWrapper<PaymentInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("out_trade_no",outTradeNo);
        queryWrapper.eq("payment_type",paymentType);
        PaymentInfo paymentInfo = paymentInfoMapper.selectOne(queryWrapper);
        return paymentInfo;
    }

    @Override
    public void savePaymentInfo(OrderInfo orderInfo, String paymentType) {
        QueryWrapper<PaymentInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("order_id", orderInfo.getId());
        queryWrapper.eq("payment_type", paymentType);
        Integer count = paymentInfoMapper.selectCount(queryWrapper);
        if(count > 0) return;

        // 保存交易记录
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setUserId(orderInfo.getUserId());
        paymentInfo.setCreateTime(new Date());
        paymentInfo.setOrderId(orderInfo.getId());
        paymentInfo.setPaymentType(paymentType);
        paymentInfo.setOutTradeNo(orderInfo.getOutTradeNo());
        paymentInfo.setPaymentStatus(PaymentStatus.UNPAID.name());
        paymentInfo.setSubject(orderInfo.getTradeBody());
        //paymentInfo.setSubject("test");
        paymentInfo.setTotalAmount(orderInfo.getTotalAmount());

        paymentInfoMapper.insert(paymentInfo);
    }
}
