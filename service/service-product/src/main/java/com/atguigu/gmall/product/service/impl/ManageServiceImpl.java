package com.atguigu.gmall.product.service.impl;

import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.mapper.*;
import com.atguigu.gmall.product.service.ManageService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;

/**
 * @author 24657
 * @apiNote
 * @date 2023/7/25 16:25
 */
@Service
public class ManageServiceImpl implements ManageService {

    @Autowired
    private BaseCategory1Mapper baseCategory1Mapper;
    @Autowired
    private BaseCategory2Mapper baseCategory2Mapper;
    @Autowired
    private BaseCategory3Mapper baseCategory3Mapper;
    @Autowired
    private BaseAttrInfoMapper baseAttrInfoMapper;
    @Autowired
    private BaseAttrValueMapper baseAttrValueMapper;

    @Autowired
    private SpuInfoMapper spuInfoMapper;

//    根据category3Id查询分类列表
    @Override
    public IPage<SpuInfo> getSpuInfoPage(Page<SpuInfo> spuInfoPage, SpuInfo spuInfo) {
        QueryWrapper<SpuInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("category3_id",spuInfo.getCategory3Id());
        queryWrapper.orderByAsc("id");
        Page<SpuInfo> pages = spuInfoMapper.selectPage(spuInfoPage, queryWrapper);
        return pages;
    }

    /**
     * 根据属性id获取属性值
     * @param attrId
     * @return
     */
    @Override
    public BaseAttrInfo getAttrInfo(Long attrId) {
        LambdaQueryWrapper<BaseAttrInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(BaseAttrInfo::getId,attrId);
        BaseAttrInfo baseAttrInfo = baseAttrInfoMapper.selectOne(queryWrapper);
//        BaseAttrInfo baseAttrInfo = baseAttrInfoMapper.selectById(attrId);
        List<BaseAttrValue> list= getAttrValue(attrId);
        baseAttrInfo.setAttrValueList(list);
        return baseAttrInfo;
    }

    private List<BaseAttrValue> getAttrValue(Long attrId) {
        QueryWrapper<BaseAttrValue> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("attr_id",attrId);
        List<BaseAttrValue> list = baseAttrValueMapper.selectList(queryWrapper);
        return list;
    }

    /**
     * 保存-更新平台属性方法
     * @param baseAttrInfo
     */
    @Override
    public void saveAttrInfo(BaseAttrInfo baseAttrInfo) {
        //什么情况下是新增什么情况下是修改根据是否有id进行判断
        if (baseAttrInfo.getId() != null) {
            //有id为更新
            baseAttrInfoMapper.updateById(baseAttrInfo);
        }else{
            //没有id则添加
            baseAttrInfoMapper.insert(baseAttrInfo);
        }
        QueryWrapper<BaseAttrValue> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("attr_id",baseAttrInfo.getId());
        //删除
        baseAttrValueMapper.delete(queryWrapper);
        //新增
        List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
        if (!CollectionUtils.isEmpty(attrValueList)&&attrValueList.size()>0) {
            attrValueList.stream().forEach(attrValue->{
                attrValue.setAttrId(baseAttrInfo.getId());
                baseAttrValueMapper.insert(attrValue);
            });
        }
    }

    @Override
    public List<BaseAttrInfo> getAttrInfoList(Long category1Id, Long category2Id, Long category3Id) {
        List<BaseAttrInfo> list=baseAttrInfoMapper.getAttrInfoList(category1Id,category2Id,category3Id);
        return list;
    }

    @Override
    public List<BaseCategory1> getCategory1() {
        LambdaQueryWrapper<BaseCategory1> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByAsc();
//        QueryWrapper<BaseCategory1> queryWrapper = new QueryWrapper<>();
        List<BaseCategory1> list = baseCategory1Mapper.selectList(queryWrapper);
        return list;
    }

    @Override
    public List<BaseCategory2> getCategory2(Long category1Id) {
        LambdaQueryWrapper<BaseCategory2> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(BaseCategory2::getCategory1Id,category1Id);
        queryWrapper.orderByAsc();
        List<BaseCategory2> list = baseCategory2Mapper.selectList(queryWrapper);
        return list;
    }

    @Override
    public List<BaseCategory3> getCategory3(Long category2Id) {
        LambdaQueryWrapper<BaseCategory3> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(BaseCategory3::getCategory2Id,category2Id);
        queryWrapper.orderByAsc();
        List<BaseCategory3> list = baseCategory3Mapper.selectList(queryWrapper);
        return list;
    }


}
