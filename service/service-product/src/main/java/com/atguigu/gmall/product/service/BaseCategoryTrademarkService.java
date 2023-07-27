package com.atguigu.gmall.product.service;

import com.atguigu.gmall.model.product.BaseTrademark;
import com.atguigu.gmall.model.product.CategoryTrademarkVo;

import java.util.List;

public interface BaseCategoryTrademarkService {
    List<BaseTrademark> findTrademarkList(Long category3Id);

    void save(CategoryTrademarkVo categoryTrademarkVo);

    void remove(Long category3Id, Long trademarkId);

    List<BaseTrademark> findCurrentTrademarkList(Long category3Id);
}
