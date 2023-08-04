package com.atguigu.gmall.item.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.item.service.ItemService;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.client.ProductFeignClient;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author 24657
 * @apiNote
 * @date 2023/7/31 19:54
 */
@SuppressWarnings("all")
@Service
public class ItemServiceImpl implements ItemService {
    /*
    * 远程调用feign
    * */
    @Autowired
    private ProductFeignClient productFeignClient;
    @Autowired
    private RedissonClient redissonClient;
    @Override
    public Map<String, Objects> getBySkuId(Long skuId) {
        Map result = new HashMap<>();
        //布隆过滤器防止穿透
//        RBloomFilter<Object> bloomFilter = redissonClient.getBloomFilter(RedisConst.SKU_BLOOM_FILTER);
//        //布隆过滤器中不存在
//        if (!bloomFilter.contains(skuId)) {
//            return result;
//        }
        //获取到的数据是skuinfo+SkuImageList
        SkuInfo skuInfo = productFeignClient.getAttrValueList(skuId);
        if (skuInfo != null) {
            //获取分类数据
            BaseCategoryView categoryView = productFeignClient.getCategoryView(skuInfo.getCategory3Id());
            result.put("categoryView",categoryView); //返回三级分类
            //获取销售属性+销售属性值 是否选中
            List<SpuSaleAttr> spuSaleAttrListCheckBySku = productFeignClient.getSpuSaleAttrListCheckBySku(skuId, skuInfo.getSpuId());
            result.put("spuSaleAttrList",spuSaleAttrListCheckBySku);//销售属性集合以及当前选中关系
            //查询销售属性值id与skuid组合层的map
            Map skuValueIdsMap = productFeignClient.getSkuValueIdsMap(skuInfo.getSpuId());
            //将map转换成页面需要的json对象
            String valueJson = JSONObject.toJSONString(skuValueIdsMap);
            result.put("valuesSkuJson",valueJson); //valuesSkuJson ==> 3127 | 3785  21
        }
        //获取价格
        BigDecimal price = productFeignClient.getSkuPrice(skuId);
        result.put("skuInfo",skuInfo);
        result.put("price",price);

        //获取海报数据
        List<SpuPoster> spuPosterList = productFeignClient.findSpuPosterBySpuId(skuInfo.getSpuId());
        result.put("spuPosterList",spuPosterList);
        //获取商品平台属性
        List<BaseAttrInfo> attrList = productFeignClient.getAttrList(skuId);
        //前端需要map形式封装到map里
        List<Map<String, Object>> skuAttrList = attrList.stream().map(baseAttrInfo -> {
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("attrName", baseAttrInfo.getAttrName());
            map.put("attrValue", baseAttrInfo.getAttrValueList().get(0).getValueName());
            return map;
        }).collect(Collectors.toList());
        result.put("skuAttrList",skuAttrList);
        return result;
    }
}
