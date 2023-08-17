package com.atguigu.gmall.cart.client;

import com.atguigu.gmall.model.cart.CartInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
 * @author 24657
 * @apiNote
 * @date 2023/8/13 14:31
 */
@FeignClient(value = "service-cart",fallback = CartDegradeFeignClient.class )
public interface CartFeignClient {
    /**
     *结算页面需要调用获取购物车列表api
     */
    @GetMapping("/api/cart/getCartCheckedList/{userId}")
    public List<CartInfo> getCartCheckedList(@PathVariable String userId);
}
