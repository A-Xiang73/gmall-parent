package com.atguigu.gmall.cart.client;

import com.atguigu.gmall.model.cart.CartInfo;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;

import java.util.List;

/**
 * @author 24657
 * @apiNote
 * @date 2023/8/13 14:32
 */
@Component
public class CartDegradeFeignClient implements CartFeignClient{
    @Override
    public List<CartInfo> getCartCheckedList(String userId) {
        return null;
    }
}
