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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Supplier;
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
    //注入线程池
    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;
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
        //获取异步编排对象
        /*
        *获取ids、获取销售属性是否选中、三级分类需要skuinfo
        * */
        CompletableFuture<SkuInfo> skuCompletableFuture = CompletableFuture.supplyAsync(() -> {
            SkuInfo skuInfo = productFeignClient.getAttrValueList(skuId);
            result.put("skuInfo", skuInfo);
            return skuInfo;
        }, threadPoolExecutor);
        //获取三级分类的异步编排对象
        CompletableFuture<Void> categoryViewCompletableFuture = skuCompletableFuture.thenAcceptAsync(skuInfo -> {
            if (skuInfo != null) {
                //获取分类数据
                BaseCategoryView categoryView = productFeignClient.getCategoryView(skuInfo.getCategory3Id());
                result.put("categoryView", categoryView); //返回三级分类
            }
        }, threadPoolExecutor);
        //获取销售属性以及是否选中关系的异步编排对象
        CompletableFuture<Void> saleIsCheckCompletableFuture = skuCompletableFuture.thenAcceptAsync(skuInfo -> {
            if (skuInfo != null) {
                //获取销售属性+销售属性值 是否选中
                List<SpuSaleAttr> spuSaleAttrListCheckBySku = productFeignClient.getSpuSaleAttrListCheckBySku(skuId, skuInfo.getSpuId());
                result.put("spuSaleAttrList",spuSaleAttrListCheckBySku);//销售属性集合以及当前选中关系
            }
        }, threadPoolExecutor);

        //获取ids组合map的异步编排属性 3127 | 3785  21
        CompletableFuture<Void> skuValueIdsMapCompletableFuture = skuCompletableFuture.thenAcceptAsync(skuInfo -> {
            if (skuInfo != null) {
                //查询销售属性值id与skuid组合层的map
                Map skuValueIdsMap = productFeignClient.getSkuValueIdsMap(skuInfo.getSpuId());
                //将map转换成页面需要的json对象
                String valueJson = JSONObject.toJSONString(skuValueIdsMap);
                result.put("valuesSkuJson",valueJson); //valuesSkuJson ==> 3127 | 3785  21
            }
        }, threadPoolExecutor);
        //获取价格
        //获取价格的异步编排对象
        CompletableFuture<Void> priceCompletableFuture = CompletableFuture.runAsync(() -> {
            BigDecimal price = productFeignClient.getSkuPrice(skuId);
            result.put("price", price);
        }, threadPoolExecutor);
        //获取海报数据
        //获取海报的异步编排对象 海报需要skuinfo
        CompletableFuture<Void> posterCompletableFuture = skuCompletableFuture.thenAcceptAsync(skuInfo -> {
            List<SpuPoster> spuPosterList = productFeignClient.findSpuPosterBySpuId(skuInfo.getSpuId());
            result.put("spuPosterList",spuPosterList);
        }, threadPoolExecutor);

        //获取商品平台属性
        //获取商品平台属性异步编排对象
        CompletableFuture<Void> attrListCompletableFuture = CompletableFuture.runAsync(() -> {
            List<BaseAttrInfo> attrList = productFeignClient.getAttrList(skuId);
            //前端需要map形式封装到map里
            List<Map<String, Object>> skuAttrList = attrList.stream().map(baseAttrInfo -> {
                Map<String, Object> map = new HashMap<String, Object>();
                map.put("attrName", baseAttrInfo.getAttrName());
                map.put("attrValue", baseAttrInfo.getAttrValueList().get(0).getValueName());
                return map;
            }).collect(Collectors.toList());
            result.put("skuAttrList",skuAttrList);
        }, threadPoolExecutor);

        CompletableFuture.allOf(
                skuCompletableFuture,
                attrListCompletableFuture,
                posterCompletableFuture,
                priceCompletableFuture,
                categoryViewCompletableFuture,
                saleIsCheckCompletableFuture,
                skuValueIdsMapCompletableFuture
        ).join();
        return result;
    }
}
