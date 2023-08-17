package com.atguigu.gmall.cart.controller.api;

import com.atguigu.gmall.cart.controller.sercice.CartService;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.util.AuthContextHolder;
import com.atguigu.gmall.model.cart.CartInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author 24657
 * @apiNote
 * @date 2023/8/10 11:05
 */
@RestController
@RequestMapping("api/cart")
public class CartApiController {
    @Autowired
    private CartService cartService;
    /**
     * 根据用户Id 查询购物车列表---购物车结算生成订单需要
     *
     * @param userId
     * @return
     */
    @GetMapping("getCartCheckedList/{userId}")
    public  List<CartInfo> getCartCheckedList(@PathVariable String userId){
        List<CartInfo> cartInfoList=cartService.getCartCheckedList(userId);
        return cartInfoList;
    }
    /**
     * /api/cart/deleteCart/28
     * 删除购物项
     * @param skuId
     * @param request
     * @return
     */
    @DeleteMapping("/deleteCart/{skuId}")
    public  Result deleteCart(@PathVariable String skuId,HttpServletRequest request){
        String userId = AuthContextHolder.getUserId(request);
        if (StringUtils.isEmpty(userId)) {
            userId=AuthContextHolder.getUserTempId(request);
        }
        cartService.deleteCartById(skuId,userId);
        return Result.ok();
    }
    /*
     * 删除所选中的购物项
     * */
    @RequestMapping("/deleteChecked")
    public Result deleteChecked(HttpServletRequest request){
        //更改状态后需要将redis中的状态也进行相应的变更
        String userId = AuthContextHolder.getUserId(request);
        if (StringUtils.isEmpty(userId)) {
            userId=AuthContextHolder.getUserTempId(request);
        }
        cartService.deleteChecked(userId,request);
        return Result.ok();
    }

    //选中状态的变更
    @GetMapping("/checkCart/{skuId}/{isCheck}")
    public Result checkCart(@PathVariable String skuId,
                            @PathVariable Integer isCheck,
                            HttpServletRequest request){
        //更改状态后需要将redis中的状态也进行相应的变更
        String userId = AuthContextHolder.getUserId(request);
        if (StringUtils.isEmpty(userId)) {
            userId=AuthContextHolder.getUserTempId(request);
        }
        cartService.checkCart(userId,skuId,isCheck);
        return  Result.ok();
    }
    /**
     * 添加购物车
     * @param skuId
     * @param skuNum
     * @param request 获取用户id和用户临时id
     * @return
     */
    @RequestMapping("addToCart/{skuId}/{skuNum}")
    public Result addToCart(@PathVariable("skuId") Long skuId,
                            @PathVariable("skuNum") Integer skuNum,
                            HttpServletRequest request){
        //获取userid
        String userId = AuthContextHolder.getUserId(request);
        if (StringUtils.isEmpty(userId)) {
            userId = AuthContextHolder.getUserTempId(request);
        }
        cartService.addToCart(skuId,skuNum,userId);
        return  Result.ok();
    }

    /**
     * 查询购物车 并且合并购物车
     *
     * @param request
     * @return
     */
    @GetMapping("cartList")
    public Result cartList(HttpServletRequest request){
        //获取用户id
        String userId = AuthContextHolder.getUserId(request);

        //获取临时用户id

        String userTempId = AuthContextHolder.getUserTempId(request);

        List<CartInfo> cartInfoList = cartService.getCartList(userId, userTempId);
        return Result.ok(cartInfoList);
    }
}
