package com.atguigu.gmall.cart.controller.sercice;

import com.atguigu.gmall.model.cart.CartInfo;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

public interface CartService {
    void addToCart(Long skuId, Integer skuNum, String userId);

    List<CartInfo> getCartList(String userId, String userTempId);

    void checkCart(String userId, String skuId, Integer isCheck);

    void deleteChecked(String userId,HttpServletRequest request);

    void deleteCartById(String skuId,String userId);

    List<CartInfo> getCartCheckedList(String userId);
}
