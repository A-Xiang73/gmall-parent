package com.atguigu.gmall.payment.service;

/**
 * @author 24657
 * @apiNote
 * @date 2023/8/15 23:08
 */
public interface AlipayService {
    String createAlipay(Long orderId);

    boolean refund(Long orderId);
}
