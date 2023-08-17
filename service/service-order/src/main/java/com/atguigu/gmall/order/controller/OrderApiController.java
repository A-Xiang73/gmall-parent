package com.atguigu.gmall.order.controller;

import com.atguigu.gmall.cart.client.CartFeignClient;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.handler.GlobalExceptionHandler;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.util.AuthContextHolder;
import com.atguigu.gmall.model.cart.CartInfo;
import com.atguigu.gmall.model.enums.OrderStatus;
import com.atguigu.gmall.model.order.OrderDetail;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.user.UserAddress;
import com.atguigu.gmall.order.service.OrderService;
import com.atguigu.gmall.product.client.ProductFeignClient;
import com.atguigu.gmall.user.client.UserFeignClient;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * @author 24657
 * @apiNote
 * @date 2023/8/13 14:43
 */
@RestController
@RequestMapping("api/order")
@SuppressWarnings("all")
public class OrderApiController {
    @Autowired
    private UserFeignClient userFeignClient;
    @Autowired
    private CartFeignClient cartFeignClient;
    @Autowired
    private  ProductFeignClient productFeignClient;
    @Autowired
    private OrderService orderService;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;
    /**
     * 内部调用获取订单
     * @param orderId
     * @return
     */
    @GetMapping("inner/getOrderInfo/{orderId}")
    public OrderInfo getOrderInfo(@PathVariable(value = "orderId") Long orderId){
        return orderService.getOrderInfo(orderId);
    }

    /*
    *返回我的订单页面
    * */
    @ApiOperation("我的订单")
    @GetMapping("auth/{page}/{limit}")
    public Result<IPage<OrderInfo>> index(        @ApiParam(name = "page", value = "当前页码", required = true)
                                                      @PathVariable Long page,

                                                  @ApiParam(name = "limit", value = "每页记录数", required = true)
                                                      @PathVariable Long limit,
                                                  HttpServletRequest request){
        String userId = AuthContextHolder.getUserId(request);
        Page<OrderInfo> pageParam = new Page<>(page, limit);
        IPage<OrderInfo> pageModel=orderService.getPage(pageParam,userId);
        return Result.ok(pageModel);
    }

    /**
     * 提交订单
     * @param orderInfo
     * @param request
     * @return
     */
    @PostMapping("auth/submitOrder")
    public Result submitOrder(@RequestBody OrderInfo orderInfo, HttpServletRequest request){
        String userId = AuthContextHolder.getUserId(request);
        orderInfo.setUserId(Long.valueOf(userId));
        //获取流水号
        String tradeNo = request.getParameter("tradeNo");
        //验证通过，保存订单
        boolean isExist = orderService.checkTradeCode(userId, tradeNo);
        if (!isExist) {
            // 比较失败！
            return Result.fail().message("不能重复提交订单！");
        }
        //验证库存 重新最后一次查询价格 看价格是否有变动
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        List<String> errorList = new ArrayList<>();
        List<CompletableFuture> futureList=new ArrayList<>();
        for (OrderDetail orderDetail : orderDetailList) {
            /*
            * 查询库存和查询价格是互不影响的 可以写实现异步编排
            * */
            //查询库存
            CompletableFuture<Void> stockCompletableFuture = CompletableFuture.runAsync(() -> {
                boolean isStock = orderService.checkStock(orderDetail.getSkuId(), orderDetail.getSkuNum());
                if (!isStock) {
                    //没有库存
                    errorList.add(orderDetail.getSkuName() + "库存不足，下单失败");
                }
            }, threadPoolExecutor);
            futureList.add(stockCompletableFuture);
            //重新检查一次价格
            //对查询价格进行异步编排
            CompletableFuture<Void> skuPriceCompletableFuture = CompletableFuture.runAsync(() -> {
                BigDecimal skuPrice = productFeignClient.getSkuPrice(orderDetail.getSkuId());
                if (orderDetail.getOrderPrice().compareTo(skuPrice)!=0) {
                    //价格不相等，表示发生了价格波动 重新查询购物车列表 更新到redis
                    String  cartKey= RedisConst.USER_KEY_PREFIX+ userId + RedisConst.USER_CART_KEY_SUFFIX;
                    BoundHashOperations boundHashOperations = redisTemplate.boundHashOps(cartKey);
                    List<CartInfo> cartCheckedList = cartFeignClient.getCartCheckedList(userId);
                    for (CartInfo cartInfo : cartCheckedList) {
                        BigDecimal newPrice = productFeignClient.getSkuPrice(cartInfo.getSkuId());
                        cartInfo.setSkuPrice(newPrice);
                        //更新redis
                        boundHashOperations.put(cartInfo.getSkuId(),cartInfo);
                    }
                    errorList.add(orderDetail.getSkuName()+"价格发生了变动，下单失败");
                }
            },threadPoolExecutor);
            futureList.add(skuPriceCompletableFuture);
        }
        //合并线程
        CompletableFuture.allOf(
                futureList.toArray(new CompletableFuture[futureList.size()])
        ).join();
        if (!CollectionUtils.isEmpty(errorList)) {
            return Result.fail().message(StringUtils.join(errorList,","));
        }
        Long orderId=orderService.saveOrderInfo(orderInfo);
        orderService.deleteTradeNo(userId);
        return Result.ok(orderId);
    }

    /**
     * 确认订单
     * @param request
     * @return
     */
    @GetMapping("/auth/trade")
    public Result<Map<String, Object>> trade(HttpServletRequest request){

        String userId = AuthContextHolder.getUserId(request);
        Map<String,Object> map=new HashMap<>();
        List<UserAddress> userAddressList = userFeignClient.findUserAddressListByUserId(userId);
        map.put("userAddressList",userAddressList);
        //选中商品列表
        /*
        * 获取的是选中商品列表 返回需要的是订单详情列表
        * 订单信息cartInfo和订单详情的关系是一对多
        * */
        List<CartInfo> cartCheckedList = cartFeignClient.getCartCheckedList(userId);
        if (CollectionUtils.isEmpty(cartCheckedList)) {
            return Result.fail();
        }
        //返回订单详情列表返回给前端
        //计算商品总件数
        AtomicReference<Integer> totalNum= new AtomicReference<>(0);
        List<OrderDetail> orderDetailList = cartCheckedList.stream().map(
                cartInfo -> {
                    OrderDetail orderDetail = new OrderDetail();
                    orderDetail.setSkuId(cartInfo.getSkuId());
                    orderDetail.setImgUrl(cartInfo.getImgUrl());
                    orderDetail.setSkuName(cartInfo.getSkuName());
                    //orderPrice为下单时的价格 这里不用更新查询 最后生成订单的时候会重新查询一次
                    orderDetail.setOrderPrice(cartInfo.getSkuPrice());
                    orderDetail.setSkuNum(cartInfo.getSkuNum());
                    totalNum.updateAndGet(v -> v + cartInfo.getSkuNum());
                    return orderDetail;
                }
        ).collect(Collectors.toList());
        map.put("detailArrayList",orderDetailList);
        //计算总金额
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOrderDetailList(orderDetailList);
        orderInfo.sumTotalAmount();
        BigDecimal totalAmount = orderInfo.getTotalAmount();
        map.put("totalAmount",totalAmount);
        //获取流水号
        String tradeNo = orderService.getTradeNo(userId);
        map.put("tradeNo",tradeNo);
        return Result.ok(map);
    }

}
