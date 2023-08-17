package com.atguigu.gmall.payment.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.atguigu.gmall.model.enums.PaymentStatus;
import com.atguigu.gmall.model.enums.PaymentType;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.payment.PaymentInfo;
import com.atguigu.gmall.order.client.OrderFeignClient;
import com.atguigu.gmall.payment.config.AlipayConfig;
import com.atguigu.gmall.payment.service.AlipayService;
import com.atguigu.gmall.payment.service.PaymentService;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import sun.nio.cs.ext.GBK;

import java.math.BigDecimal;
import java.util.HashMap;

/**
 * @author 24657
 * @apiNote
 * @date 2023/8/15 23:09
 */
@Service
@SuppressWarnings("all")
public class AlipayServiceImpl implements AlipayService {

    @Autowired
    private AlipayClient alipayClient;

    @Autowired
    private OrderFeignClient orderFeignClient;

    @Autowired
    private PaymentService paymentService;

    /*
    * 退款支付接口
    * */
    @Override
    @SneakyThrows
    public boolean refund(Long orderId) {
        AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
        JSONObject bizContent = new JSONObject();
        OrderInfo orderInfo=orderFeignClient.getOrderInfo(orderId);
        //out_trade_no喝trade_no只需要选择一个传入就行
        bizContent.put("out_trade_no", orderInfo.getOutTradeNo());
        bizContent.put("refund_amount", new BigDecimal("0.01"));
        bizContent.put("out_request_no", "退款原因：不想要了");

        //// 返回参数选项，按需传入
        //JSONArray queryOptions = new JSONArray();
        //queryOptions.add("refund_detail_item_list");
        //bizContent.put("query_options", queryOptions);
        request.setBizContent(bizContent.toString());
        AlipayTradeRefundResponse response = alipayClient.execute(request);
        if(response.isSuccess()){
            /*
            *  退款成功判断说明：接口返回fund_change=Y为退款成功，fund_change=N或无此字段值返回时需通过退款查询接口进一步确认退款状态。详见退款成功判断指导。注意，接口中code=10000，仅代表本次退款请求成功，不代表退款成功。
            * */
            //只能表示调用是否成功 不能表示退款成功
            if ("Y".equals(response.getFundChange())){
                //退款成功 更新账单记录
                String status = PaymentStatus.CLOSED.name();
                paymentService.updatePaymentInfo(status,orderInfo.getOutTradeNo(),PaymentType.ALIPAY.name());
                return true;
            }
            System.out.println("调用成功");
            return false;
        } else {
            System.out.println("调用失败");
        }
        return false;
    }

    /*
    * 调用支付宝接口生成支付订单二维码
    * */
    @Override
    public String createAlipay(Long orderId) {
        OrderInfo orderInfo = orderFeignClient.getOrderInfo(orderId);
        if ("PAID".equals(orderInfo.getOrderStatus())||"CLOSED".equals(orderInfo.getOrderStatus())) {
            return "该订单已经完成或已经关闭!";
        }
        //保存账单记录
        paymentService.savePaymentInfo(orderInfo, PaymentType.ALIPAY.name());
        String form="";
        AlipayTradePagePayRequest alipayRequest =  new  AlipayTradePagePayRequest(); //创建API对应的request
        //  同步回调 http://api.gmall.com/api/payment/alipay/callback/return
        alipayRequest.setReturnUrl(AlipayConfig.return_payment_url);
        //  异步回调  内网穿透 地址
        alipayRequest.setNotifyUrl( AlipayConfig.notify_payment_url ); //在公共参数中设置回跳和通知地址
        //  封装业务参数
        HashMap<String, Object> map = new HashMap<>();
        //  第三方业务编号！
        map.put("out_trade_no",orderInfo.getOutTradeNo());
        map.put("product_code","FAST_INSTANT_TRADE_PAY");
        map.put("total_amount","0.01");
        map.put("subject",orderInfo.getTradeBody());
        //  设置二维码过期时间
        map.put("timeout_express","30m");
        alipayRequest.setBizContent(JSON.toJSONString(map));
        try  {
            form = alipayClient.pageExecute(alipayRequest).getBody();  //调用SDK生成表单
        }  catch  (AlipayApiException e) {
            e.printStackTrace();
        }
        return form;
    }

}
