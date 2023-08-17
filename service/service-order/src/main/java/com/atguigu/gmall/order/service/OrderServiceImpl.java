package com.atguigu.gmall.order.service;

import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.common.util.HttpClientUtil;
import com.atguigu.gmall.model.cart.CartInfo;
import com.atguigu.gmall.model.enums.OrderStatus;
import com.atguigu.gmall.model.enums.ProcessStatus;
import com.atguigu.gmall.model.order.OrderDetail;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.order.mapper.OrderDetailMapper;
import com.atguigu.gmall.order.mapper.OrderInfoMapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.*;

/**
 * @author 24657
 * @apiNote
 * @date 2023/8/13 16:51
 */
@Service
public class OrderServiceImpl implements OrderService{
    @Autowired
    private OrderInfoMapper orderInfoMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private RabbitService rabbitService;
    //获取库存服务地址
    @Value("${ware.url}")
    private String WARE_URL;

    /*
    * 取消订单接口
    * */
    @Override
    public void execExpiredOrder(Long orderId) {
        // orderInfo
        updateOrderStatus(orderId, ProcessStatus.CLOSED);
    }

    @Override
    public void updateOrderStatus(Long orderId, ProcessStatus processStatus) {
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setId(orderId);
        orderInfo.setProcessStatus(processStatus.name());
        orderInfo.setOrderStatus(processStatus.getOrderStatus().name());
        orderInfoMapper.updateById(orderInfo);
    }

    @Override
    public OrderInfo getOrderInfo(Long orderId) {
        OrderInfo orderInfo = orderInfoMapper.selectById(orderId);
        QueryWrapper<OrderDetail> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("order_id", orderId);
        List<OrderDetail> orderDetailList = orderDetailMapper.selectList(queryWrapper);
        orderInfo.setOrderDetailList(orderDetailList);
        return orderInfo;
    }

    @Override
    public IPage<OrderInfo> getPage(Page<OrderInfo> pageParam, String userId) {
        IPage<OrderInfo> ipageInfo=orderInfoMapper.selectPageByUserId(pageParam,userId);
        return ipageInfo;
    }

    @Override
    public boolean checkStock(Long skuId, Integer skuNum) {
        /*远程调用http://localhost:9001/hasStock?skuId=10221&num=2*/
        //发起http请求 发起http请求的几种方式
        String result = HttpClientUtil.doGet(WARE_URL + "/hasStock?skuId=" + skuId + "&num=" + skuNum);
        return "1".equals(result);
    }

    /*
    * 获取流水号
    * */
    @Override
    public String getTradeNo(String userId) {
        String tradeNoKey = getTradeNoKey(userId);
        //定义一个流水号
        String tradeNo = UUID.randomUUID().toString().replace("-", "");
        //将流水号存储到redis中
        redisTemplate.opsForValue().set(tradeNoKey,tradeNo);
        return tradeNo;
    }
    public String getTradeNoKey(String userId){
        return RedisConst.USER_KEY_PREFIX + userId + RedisConst.USER_TRADE_CODE_SUFFIX;
    }

    @Override
    public boolean checkTradeCode(String userId, String tradeCodeNo) {
        String destTradeNo = redisTemplate.opsForValue().get(getTradeNoKey(userId));
        return tradeCodeNo.equals(destTradeNo);
    }

    @Override
    public void deleteTradeNo(String userId) {
        redisTemplate.delete(getTradeNoKey(userId));
    }

    @Override
    @Transactional
    public Long saveOrderInfo(OrderInfo orderInfo) {
       /*
        *保存前需要确认流水号 避免重复提交
        *       流水号在查询订单时生成，并将其存到redis中，保存订单前先从redis中查询判断若存在才可以保存订单，当保存订单后将流水号删除，可以避免用户重复提交的问题
        * 保存前需要重新查询价格
        * 保存前还要校验库存
        * 保存单据 orderinfo和orderdetail
        * 保存后需要将购物车中的数据删除
        * 重定向到支付页面
        * */
        orderInfo.sumTotalAmount();
        orderInfo.setOrderStatus(OrderStatus.UNPAID.name());
        //交易编号 支付宝支付的时候会使用
        String outTradeNo = "ATGUIGU" + System.currentTimeMillis() + "" + new Random().nextInt(1000);
        orderInfo.setOutTradeNo(outTradeNo);
        orderInfo.setCreateTime(new Date());
        //设置过期时间 定义为一天后
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE,1);
        orderInfo.setExpireTime(calendar.getTime());
        //设置订单流程状态
        orderInfo.setProcessStatus(ProcessStatus.UNPAID.name());
        //获取订单明细
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        //订单描述
        StringBuilder tradeBody = new StringBuilder();
        for (OrderDetail orderDetail : orderDetailList) {
            tradeBody.append(orderDetail.getSkuName()+" ");

        }
        if (tradeBody.toString().length()>100){
            orderInfo.setTradeBody(tradeBody.toString().substring(0,100));
        }else {
            orderInfo.setTradeBody(tradeBody.toString());
        }
        for (OrderDetail orderDetail : orderDetailList) {
            orderDetail.setOrderId(orderInfo.getId());
            orderDetailMapper.insert(orderDetail);

        }
        orderInfoMapper.insert(orderInfo);
        //发送延时消息 ，如果未定时支付则取消订单
        rabbitService.sendDelayMsg(MqConst.EXCHANGE_DIRECT_ORDER_CANCEL,MqConst.ROUTING_ORDER_CANCEL,orderInfo.getId(),MqConst.DELAY_TIME);
        return orderInfo.getId();
    }
}
