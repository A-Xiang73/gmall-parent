package com.atguigu.gmall.item.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.item.service.ItemService;
import com.atguigu.gmall.product.client.ProductFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Objects;

/**
 * @author 24657
 * @apiNote 对外的接口名称添加Api方便识别
 * @date 2023/7/31 19:53
 */
@RestController
@RequestMapping("api/item")
@SuppressWarnings("all")
public class ItemApiController {
    @Autowired
    private ProductFeignClient productFeignClient;
    @Autowired
    private ItemService itemService;
    /**
     * 获取sku详情信息
     * @param skuId
     * @return
     */
    @GetMapping("{skuId}")
    public Result<Map> getItem(@PathVariable Long skuId){
        Map<String, Objects> result=itemService.getBySkuId(skuId);
        return Result.ok(result);
    }

}
