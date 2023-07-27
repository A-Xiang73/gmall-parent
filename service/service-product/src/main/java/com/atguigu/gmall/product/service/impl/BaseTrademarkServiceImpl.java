package com.atguigu.gmall.product.service.impl;

import com.atguigu.gmall.model.product.BaseTrademark;
import com.atguigu.gmall.product.mapper.BaseTrademarkMapper;
import com.atguigu.gmall.product.service.BaseTrademarkService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author 24657
 * @apiNote
 * @date 2023/7/27 10:18
 */
@Service
public class BaseTrademarkServiceImpl implements BaseTrademarkService {

    @Autowired
    private BaseTrademarkMapper baseTrademarkMapper;

    /*
    * 根据id获取品牌信息
    * */
    @Override
    public BaseTrademark getById(String id) {
        BaseTrademark baseTrademark = baseTrademarkMapper.selectById(id);
        return baseTrademark;
    }

    /*
     * 修改BaseTrademark
     * */
    @Override
    public void update(BaseTrademark baseTrademark) {
        if (baseTrademark.getId() != null) {
            baseTrademarkMapper.updateById(baseTrademark);
        }
    }

    /*
    * 新增品牌
    * */

    @Override
    public void save(BaseTrademark baseTrademark) {
        if (baseTrademark!= null) {
            baseTrademarkMapper.insert(baseTrademark);
        }
    }

    /*
    * 根据id进行删除
    * */
    @Override
    public void removeById(Long id) {
            baseTrademarkMapper.deleteById(id);
    }

    /*
    * 获取品牌分页列表
    * */
    @Override
    public IPage<BaseTrademark> getPage(Page<BaseTrademark> pageParm) {
        QueryWrapper<BaseTrademark> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByAsc("id");
        IPage<BaseTrademark> pages = baseTrademarkMapper.selectPage(pageParm, queryWrapper);
        return pages;
    }
}
