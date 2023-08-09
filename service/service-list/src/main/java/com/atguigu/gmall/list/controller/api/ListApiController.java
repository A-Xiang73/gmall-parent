package com.atguigu.gmall.list.controller.api;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.list.service.SearchService;
import com.atguigu.gmall.model.list.Goods;


import com.atguigu.gmall.model.list.SearchParam;
import com.atguigu.gmall.model.list.SearchResponseVo;
import lombok.experimental.PackagePrivate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

/**
 * @author 24657
 * @apiNote
 * @date 2023/8/5 16:36
 */
@RestController
@RequestMapping("api/list")
@SuppressWarnings("all")
public class ListApiController {
    @Autowired
    private ElasticsearchRestTemplate elasticsearchRestTemplate;
    @Autowired
    private SearchService searchService;


    //初始化mapping结构到es中
    @GetMapping("inner/createIndex")
    public Result createIndex(){
        elasticsearchRestTemplate.createIndex(Goods.class);
        elasticsearchRestTemplate.putMapping(Goods.class);
        return Result.ok();
    }

    /**
     * 上架商品
     * @param skuId
     * @return
     */
    @GetMapping("inner/upperGoods/{skuId}")
    public Result upperGoods(@PathVariable("skuId") Long skuId){
        searchService.upperGoods(skuId);
        return Result.ok();
    }
    /**
     * 下架商品
     * @param skuId
     * @return
     */
    @GetMapping("inner/lowerGoods/{skuId}")
    public Result lowerGoods(@PathVariable("skuId") Long skuId){
        searchService.lowerGoods(skuId);
        return Result.ok();
    }
    /**
     * 更新商品incrHotScore
     *
     * @param skuId
     * @return
     */
    @GetMapping("inner/incrHotScore/{skuId}")
    public Result incrHotScore(@PathVariable("skuId") Long skuId){
        searchService.incrHotScore(skuId);
        return Result.ok();
    }
    /**
     * 搜索商品
     * @param searchParam
     * @return
     * @throws IOException
     */
    @PostMapping()
    public Result list(@RequestBody SearchParam searchParam) throws IOException {
        SearchResponseVo response = searchService.search(searchParam);
        return Result.ok(response);
    }
}
