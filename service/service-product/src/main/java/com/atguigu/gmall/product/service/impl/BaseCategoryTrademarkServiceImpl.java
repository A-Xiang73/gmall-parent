package com.atguigu.gmall.product.service.impl;

import com.atguigu.gmall.model.product.BaseCategoryTrademark;
import com.atguigu.gmall.model.product.BaseTrademark;
import com.atguigu.gmall.model.product.CategoryTrademarkVo;
import com.atguigu.gmall.product.mapper.BaseCategoryTrademarkMapper;
import com.atguigu.gmall.product.mapper.BaseTrademarkMapper;
import com.atguigu.gmall.product.service.BaseCategoryTrademarkService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author 24657
 * @apiNote 分类和品牌中间表的实现类
 * @date 2023/7/27 11:21
 */
@Service
public class BaseCategoryTrademarkServiceImpl implements BaseCategoryTrademarkService {
    @Autowired
    private BaseCategoryTrademarkMapper baseCategoryTrademarkMapper;
    @Autowired
    private BaseTrademarkMapper baseTrademarkMapper;

    /*
     * 删除分类品牌关系
     * */
    @Override
    public void remove(Long category3Id, Long trademarkId) {
        QueryWrapper<BaseCategoryTrademark> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("category3_id",category3Id);
        queryWrapper.eq("trademark_id",trademarkId);
        baseCategoryTrademarkMapper.delete(queryWrapper);
    }

    /*
    * 保存分类品牌关系  一个分类保存多个品牌
    * */
    @Override
    public void save(CategoryTrademarkVo categoryTrademarkVo) {
        List<Long> trademarkIdList = categoryTrademarkVo.getTrademarkIdList();
        for (Long aLong : trademarkIdList) {
            BaseCategoryTrademark baseCategoryTrademark = new BaseCategoryTrademark();
            baseCategoryTrademark.setCategory3Id(categoryTrademarkVo.getCategory3Id());
            baseCategoryTrademark.setTrademarkId(aLong);
            baseCategoryTrademarkMapper.insert(baseCategoryTrademark);
        }
    }

    //根据category3Id获取品牌列表,先找到中间表根据中间表找到对应的品牌
    @Override
    public List<BaseTrademark> findTrademarkList(Long category3Id) {
        QueryWrapper<BaseCategoryTrademark> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("category3_id",category3Id);
        List<BaseCategoryTrademark> list = baseCategoryTrademarkMapper.selectList(queryWrapper);
        List<BaseTrademark> baseTrademarkList=new ArrayList<>();
        for (BaseCategoryTrademark baseCategoryTrademark : list) {
            Long trademarkId = baseCategoryTrademark.getTrademarkId();
            BaseTrademark baseTrademark = baseTrademarkMapper.selectById(trademarkId);
            baseTrademarkList.add(baseTrademark);
        }
        return baseTrademarkList;
    }

    /*
    * 获取可选品牌列表
    * */
    @Override
    public List<BaseTrademark> findCurrentTrademarkList(Long category3Id) {
        QueryWrapper<BaseCategoryTrademark> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("category3_id",category3Id);
        List<BaseCategoryTrademark> baseCategoryTrademarkList = baseCategoryTrademarkMapper.selectList(queryWrapper);
        //找到已经关联了品牌id进行过滤
        if (!CollectionUtils.isEmpty(baseCategoryTrademarkList)) {
            List<Long> trademarkIdList=new ArrayList<>();
            for (BaseCategoryTrademark baseCategoryTrademark : baseCategoryTrademarkList) {
                Long trademarkId = baseCategoryTrademark.getTrademarkId();
                trademarkIdList.add(trademarkId);
            }
            List<BaseTrademark> baseTrademarkList = baseTrademarkMapper.selectList(null).stream().filter(baseTrademark -> {
                return !trademarkIdList.contains(baseTrademark.getId());
            }).collect(Collectors.toList());
            return baseTrademarkList;
        }
        return baseTrademarkMapper.selectList(null);
    }
}
