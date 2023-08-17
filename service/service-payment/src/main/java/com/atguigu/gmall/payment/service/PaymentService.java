package com.atguigu.gmall.payment.service;

import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.payment.PaymentInfo;

import java.util.Map;

/**
 * @author 24657
 * @apiNote
 * @date 2023/8/15 23:12
 */
public interface PaymentService {
    /*
    * 根据out_trade_no获取支付账单
    * */
    PaymentInfo getPaymentInfo(String outTradeNo, String paymentType);

    void savePaymentInfo(OrderInfo orderInfo, String name);


    void updatePaymentInfo(PaymentInfo paymentInfo, String paid,Map<String, String> paramsMap);

    void paySuccess(String out_trade_no, String name, Map<String, String> paramsMap);

    PaymentInfo getPaymentInfo(Long orderId);

    void updatePaymentInfo(String status, String outTradeNo, String paymentType);
}
