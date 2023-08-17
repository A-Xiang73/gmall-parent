package com.atguigu.gmall.payment.controller;

import com.alipay.api.internal.util.AlipaySignature;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.enums.PaymentStatus;
import com.atguigu.gmall.model.enums.PaymentType;
import com.atguigu.gmall.model.payment.PaymentInfo;
import com.atguigu.gmall.payment.config.AlipayConfig;
import com.atguigu.gmall.payment.service.AlipayService;
import com.atguigu.gmall.payment.service.PaymentService;
import com.sun.org.apache.regexp.internal.RE;
import jdk.nashorn.internal.ir.Flags;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author 24657
 * @apiNote
 * @date 2023/8/15 23:07
 */
@Controller
@RequestMapping("/api/payment/alipay")
public class AlipayController {
    @Autowired
    private AlipayService alipayService;
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private RedisTemplate redisTemplate;
    // 发起退款！http://localhost:8205/api/payment/alipay/refund/20
    @RequestMapping("refund/{orderId}")
    @ResponseBody
    public Result refund(@PathVariable(value = "orderId")Long orderId){
        boolean flag=alipayService.refund(orderId);
        return Result.ok(flag);
    }

    /*
    *对于 PC 网站支付的交易，在用户支付完成之后，支付宝会根据 API 中商家传入的 notify_url，通过 POST 请求的形式将支付结果作为参数通知到商家系统。
    * 支付宝异步调用返回
    * 支付宝主动调用这个接口
    * */
    @PostMapping("/callback/notify")
    @ResponseBody
    @SneakyThrows
    public String callbackNotify(@RequestParam Map<String, String> paramsMap){
        //异步返回通知校验
        //将异步通知中收到的所有参数都存放到map中
        boolean signVerified = AlipaySignature.rsaCheckV1(paramsMap, AlipayConfig.alipay_public_key, AlipayConfig.charset, AlipayConfig.sign_type); //调用SDK验证签名
        /*
        * 商家需要验证该通知数据中的 out_trade_no 是否为商家系统中创建的订单号。
        判断 total_amount 是否确实为该订单的实际金额（即商家订单创建时的金额）。
        校验通知中的 seller_id（或者 seller_email) 是否为 out_trade_no 这笔单据的对应的操作方（有的时候，一个商家可能有多个 seller_id/seller_email）。
        验证 app_id 是否为该商家本身
        * */
        String out_trade_no = paramsMap.get("out_trade_no");
        BigDecimal total_amount = new BigDecimal(paramsMap.get("total_amount"));

        String app_id = paramsMap.get("app_id");
        //交易状态 只有交易通知状态为 TRADE_SUCCESS 或 TRADE_FINISHED 时，支付宝才会认定为买家付款成功。
        String trade_status = paramsMap.get("trade_status");
        String notify_id = paramsMap.get("notify_id");
        if(signVerified){
            //进行支付宝参数校验
            // TODO 验签成功后，按照支付结果异步通知中的描述，对支付结果中的业务内容进行二次校验，校验成功后在response中返回success并继续商户自身业务处理，校验失败返回failure
            PaymentInfo paymentInfo =paymentService.getPaymentInfo(out_trade_no, PaymentType.ALIPAY.name());
            //解决消息幂等性问题避免重复消费
            //  success 是判断支付异步通知是否发送成功
            if(paymentInfo!=null&& new BigDecimal("0.01").compareTo(total_amount)==0&&AlipayConfig.app_id.equals(app_id)){
                Boolean isExist = redisTemplate.opsForValue().setIfAbsent(notify_id, notify_id, 1424, TimeUnit.MINUTES);
                if(!isExist){
                    //已经消费过了
                    return "success";
                }
                //判断买家是否付款成功
                if("TRADE_SUCCESS".equals(trade_status)||"TRADE_FINISHED".equals(trade_status)){
                    //买付款成功
                    //支付成功进行个更新状态 扣减库存
                    //发送消息扣减库存呢
                    //消费成功 记录标示异步通知的唯一标示
                    //将所有更新操作封装到paySuccess方法中
                        //没有被消费过
                        paymentService.paySuccess(out_trade_no,PaymentType.ALIPAY.name(), paramsMap);
                }
                //  success 是判断支付异步通知是否发送成功
                return "success";
            }else {
                //校验失败 等待重新校验
                return "failure";
            }
        }else{
            // TODO 验签失败则记录异常日志，并在response中返回failure.
            return "failure";
        }

    }

    /*
    * 支付订单生成二维码
    * */
    @RequestMapping("submit/{orderId}")
    @ResponseBody
    public String submitOrder(@PathVariable Long orderId){
        String page = alipayService.createAlipay(orderId);
        return page;
    }
    /**
     * 支付宝同步回调
     * @return
     */
    @RequestMapping("callback/return")
    public String callBack(){
        return "redirect:"+ AlipayConfig.return_order_url;
    }
}
