package com.atguigu.gmall.order.service;

import com.atguigu.gmall.model.enums.ProcessStatus;
import com.atguigu.gmall.model.order.OrderInfo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

public interface OrderService {
    /**
     * 验证库存
     * @param skuId
     * @param skuNum
     * @return
     */
    boolean checkStock(Long skuId, Integer skuNum);
    /**
     * 生产流水号
     * @param userId
     * @return
     */
    String getTradeNo(String userId);
    /**
     * 比较流水号
     * @param userId 获取缓存中的流水号
     * @param tradeCodeNo   页面传递过来的流水号
     * @return
     */
    boolean checkTradeCode(String userId, String tradeCodeNo);
    /**
     * 删除流水号
     * @param userId
     */
    void deleteTradeNo(String userId);
    /*
    * 保存orderinfo订单信息
    * */
    Long saveOrderInfo(OrderInfo orderInfo);

    IPage<OrderInfo> getPage(Page<OrderInfo> pageParam, String userId);

    OrderInfo getOrderInfo(Long orderId);

    void execExpiredOrder(Long orderId);

    void updateOrderStatus(Long orderId, ProcessStatus paid);
}
