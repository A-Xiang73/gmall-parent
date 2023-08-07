package com.atguigu.gmall.list.service;

/**
 * @author 24657
 * @apiNote
 * @date 2023/8/7 16:17
 */
public interface SearchService {
    /**
     * 上架商品列表
     * @param skuId
     */
    void upperGoods(Long skuId);

    /**
     * 下架商品列表
     * @param skuId
     */
    void lowerGoods(Long skuId);

    void incrHotScore(Long skuId);
}
