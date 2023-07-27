package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.BaseCategoryTrademark;
import com.atguigu.gmall.model.product.BaseTrademark;
import com.atguigu.gmall.model.product.CategoryTrademarkVo;
import com.atguigu.gmall.product.service.BaseCategoryTrademarkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author 24657
 * @apiNote
 * @date 2023/7/27 11:15
 */
/*
* 分类品牌管理 管理分类和品牌的中间关联表 分类和品牌是多对多的关系需要一个中间表金心血管关联
* */
@RestController
@RequestMapping("admin/product/baseCategoryTrademark")
public class BaseCategoryTrademarkController {
    @Autowired
    private BaseCategoryTrademarkService baseCategoryTrademarkService;
    /*
     * 删除分类品牌关联
     * */@DeleteMapping("remove/{category3Id}/{trademarkId}")
    public Result remove(@PathVariable Long category3Id, @PathVariable Long trademarkId){
         baseCategoryTrademarkService.remove(category3Id,trademarkId);
         return Result.ok();
    }

    /*
    * 保存分类品牌关联
    * */
    @PostMapping("save")
    public  Result save(@RequestBody CategoryTrademarkVo categoryTrademarkVo){
        baseCategoryTrademarkService.save(categoryTrademarkVo);
        return Result.ok();
    }

    /*
    根据category3Id获取品牌列表
    * */
    @GetMapping("findTrademarkList/{category3Id}")
    public Result findTrademarkList(@PathVariable Long category3Id){
       List<BaseTrademark> baseTrademarkList= baseCategoryTrademarkService.findTrademarkList(category3Id);
        return Result.ok(baseTrademarkList);
    }
   /*
    根据category3Id获取可选品牌列表
    * */
    @GetMapping("findCurrentTrademarkList/{category3Id}")
    public Result findCurrentTrademarkList(@PathVariable Long category3Id){
        List<BaseTrademark> list= baseCategoryTrademarkService.findCurrentTrademarkList(category3Id);
        return Result.ok(list);
    }

}
