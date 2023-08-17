package com.atguigu.gmall.cart.controller.sercice.impl;

import com.atguigu.gmall.cart.controller.sercice.CartService;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.util.DateUtil;
import com.atguigu.gmall.model.cart.CartInfo;
import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.product.client.ProductFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author 24657
 * @apiNote
 * @date 2023/8/10 11:06
 */
@Service
@SuppressWarnings("all")
public class CartServiceImpl implements CartService {

    @Autowired
    private ProductFeignClient productFeignClient;
    @Autowired
    private RedisTemplate redisTemplate;

    /*
    * 
    * 订单页面获取购物车列表
    * */
    
    @Override
    public List<CartInfo> getCartCheckedList(String userId) {
        BoundHashOperations boundHashOperations = redisTemplate.boundHashOps(getCartKey(userId));
        List<CartInfo> cartInfoList = boundHashOperations.values();
        cartInfoList = cartInfoList.stream().filter(cartInfo -> {
            //重新查询确认价格
            BigDecimal skuPrice = productFeignClient.getSkuPrice(cartInfo.getSkuId());
            cartInfo.setSkuPrice(skuPrice);
            return cartInfo.getIsChecked() == 1;
        }).collect(Collectors.toList());
        return cartInfoList;
    }

    /*
    * 删除当前数据
    * */
    @Override
    public void deleteCartById(String skuId, String userId) {
        BoundHashOperations boundHashOperations = redisTemplate.boundHashOps(getCartKey(userId));
        boundHashOperations.delete(skuId);
    }

    /*
    * 删除所选中的数据
    * */
    @Override
    public void deleteChecked(String userId,HttpServletRequest request) {
        BoundHashOperations boundHashOperations = redisTemplate.boundHashOps(getCartKey(userId));
        List<CartInfo> cartInfoList = boundHashOperations.values();
        for (CartInfo cartInfo : cartInfoList) {
            boundHashOperations.delete(cartInfo.getIsChecked()==1);
        }
    }

    /*
    * 更改选中商品状态
    * */
    @Override
    public void checkCart(String userId,String skuId, Integer isCheck) {
        BoundHashOperations boundHashOps = redisTemplate.boundHashOps(getCartKey(userId));
        CartInfo cartInfo = (CartInfo) boundHashOps.get(skuId);
        if (cartInfo != null) {
            cartInfo.setIsChecked(isCheck);
            //这里已经获得了boundHashOps 存储的是商品项 商品id：商品  userid对应的才是是boundhahops
            boundHashOps.put(skuId,cartInfo);
        }
    }

    /**
     * 查询购物车
     * @param request
     * @return
     */
//    @Override
//    public List<CartInfo> getCartList(String userId, String userTempId) {
//        List<CartInfo> cartInfoList=null;
//        //获取临时购物车的数据
//        if (!StringUtils.isEmpty(userTempId)) {
//            BoundHashOperations boundHashOperations = redisTemplate.boundHashOps(getCartKey(userTempId));
//            cartInfoList = boundHashOperations.values();
//        }
//        //获取用户购物车的数据
//        if (!StringUtils.isEmpty(userId)) {
//            BoundHashOperations boundHashOperations = redisTemplate.boundHashOps(getCartKey(userId));
//            cartInfoList=boundHashOperations.values();
//        }
//        if (!CollectionUtils.isEmpty(cartInfoList)) {
//            //对购物车内容进行排序
//            cartInfoList.sort((o1, o2) -> {
//                //时间大小比较
//                return DateUtil.truncatedCompareTo(o2.getUpdateTime(),o1.getUpdateTime(), Calendar.SECOND);
//            });
//        }
//        return cartInfoList;
//    }

    /**
     * 合并购物车
     * @param request
     * @return
     */
    @Override
    public List<CartInfo> getCartList(String userId, String userTempId) {
        /*
        * 三种情况
        *   1.判断用户是否登录
        *       没登录，则不需要合并
        *       登录
        *           2.判断临时购物车是否为空
        *               （1）临时购物车为空，则不需要合并购物车
        *               （2）临时购物车不为空，则需要合并购物车
        *                   合并购物车，判断是否有相同的商品
        *                       若有相同的商品，进行更新数量，若无相同商品，将临时购物车中的商品复制到用户购物车中
        * */
        List<CartInfo> cartInfoList=null;
        if (StringUtils.isEmpty(userId)) {
            //用户没有登录
            String cartKey = getCartKey(userTempId);
            BoundHashOperations boundHashOperations = redisTemplate.boundHashOps(cartKey);
            cartInfoList = boundHashOperations.values();
            //对购物车进行排序
            cartInfoList.sort(new Comparator<CartInfo>() {
                @Override
                public int compare(CartInfo o1, CartInfo o2) {
                    return DateUtil.truncatedCompareTo(o2.getCreateTime(),o1.getCreateTime(),Calendar.SECOND);
                }
            });
            return cartInfoList;
        }
        //用户登录了
        List<CartInfo> tempList = redisTemplate.boundHashOps(getCartKey(userTempId)).values();
        //判断临时购物车是否为空
        if (CollectionUtils.isEmpty(tempList)) {
            //临时购物车为空
            cartInfoList = redisTemplate.boundHashOps(getCartKey(userId)).values();
            return cartInfoList;
        }
        //临时购物车不为空
        //合并购物车，判断是否有相同的商品
        BoundHashOperations boundHashOperations = redisTemplate.boundHashOps(getCartKey(userId));
        List<CartInfo> userList = boundHashOperations.values();
        for (CartInfo cartInfo : tempList) {
            //有相同的商品
            for (CartInfo info : userList) {
                if (cartInfo.getSkuId()==info.getSkuId()) {
                    //数量相加
                    info.setSkuNum(info.getSkuNum()+cartInfo.getSkuNum());
                    //更新实时价格
                    info.setSkuPrice(productFeignClient.getSkuPrice(info.getSkuId()));
                    info.setUpdateTime(new Date());
                    if(cartInfo.getIsChecked()==1){
                        info.setIsChecked(1);
                    }
                    boundHashOperations.put(info.getSkuId().toString(),info);
                }else{
                    //没有相同的商品，将临时购物车中的数据放到用户购物车中
                    cartInfo.setUserId(info.getUserId());
                    cartInfo.setCreateTime(new Date());
                    cartInfo.setUpdateTime(new Date());
                    cartInfo.setSkuPrice(productFeignClient.getSkuPrice(cartInfo.getSkuId()));
                    boundHashOperations.put(cartInfo.getSkuId().toString(),cartInfo);
                }
            }
        }
//        BoundHashOperations tempboundOps = redisTemplate.boundHashOps(getCartKey(userTempId));
//
//        for (CartInfo cartInfo : tempList) {
//            tempboundOps.delete(cartInfo.getSkuId().toString());
//        }
        //删除已经合并了的临时购物车
        redisTemplate.delete(getCartKey(userTempId));
        List<CartInfo> userCarts = redisTemplate.boundHashOps(getCartKey(userId)).values();
        userCarts.sort(new Comparator<CartInfo>() {
            @Override
            public int compare(CartInfo o1, CartInfo o2) {
                return DateUtil.truncatedCompareTo(o2.getUpdateTime(),o1.getUpdateTime(),Calendar.SECOND);
            }
        });
        return userCarts;
    }

    /**
     * 添加购物车
     * @param userId
     * @return
     */
    @Override
    public void addToCart(Long skuId, Integer skuNum, String userId) {
        /*
        * 添加购物车两种情况
        *   1.购物车中已经存在，只更改商品的数量
        *   2.购物车中不存在，新添加商品
        * 查询redis获取key判断是否已经存在该商品  存储的格式是hash
        * cartKey:hash(skuId:商品对象)
        *   若查询到，则说明购物车中已经存在
        *   否则，则没有，添加一个新的
        * */
        //定义存储key
        String cartKey = getCartKey(userId);
        BoundHashOperations<String,String, CartInfo> boundHashOperations = redisTemplate.boundHashOps(cartKey);
        CartInfo cartInfo=null;
        //判断redis中是否存在该商品
        if (boundHashOperations.hasKey(skuId.toString())) {
            //存在
            cartInfo = boundHashOperations.get(skuId.toString());
            cartInfo.setSkuNum(cartInfo.getSkuNum()+skuNum);
            //实时价格.
            cartInfo.setSkuPrice(productFeignClient.getSkuPrice(skuId));
            cartInfo.setIsChecked(1);
            cartInfo.setUpdateTime(new Date());
        }else{
            //不存在
            cartInfo=new CartInfo();
            SkuInfo skuInfo = productFeignClient.getAttrValueList(skuId);
            cartInfo.setUserId(userId);
            cartInfo.setSkuId(skuId);
            cartInfo.setCartPrice(skuInfo.getPrice());
            cartInfo.setSkuNum(skuNum);
            cartInfo.setImgUrl(skuInfo.getSkuDefaultImg());
            cartInfo.setSkuName(skuInfo.getSkuName());
            cartInfo.setSkuPrice(skuInfo.getPrice());
            cartInfo.setCreateTime(new Date());
            cartInfo.setUpdateTime(new Date());
        }
        boundHashOperations.put(skuId.toString(),cartInfo);

    }

    private  String getCartKey(String userId) {
        String  cartKey= RedisConst.USER_KEY_PREFIX+ userId + RedisConst.USER_CART_KEY_SUFFIX;
        return cartKey;
    }
}
