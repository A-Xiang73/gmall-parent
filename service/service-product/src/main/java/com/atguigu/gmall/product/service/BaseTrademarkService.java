package com.atguigu.gmall.product.service;

import com.atguigu.gmall.model.product.BaseTrademark;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

public interface BaseTrademarkService {
    IPage<BaseTrademark> getPage(Page<BaseTrademark> pageParm);

    void removeById(Long id);

    void save(BaseTrademark baseTrademark);

    void update(BaseTrademark baseTrademark);

    BaseTrademark getById(String id);
}
