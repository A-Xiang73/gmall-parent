package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.BaseCategoryTrademark;
import com.atguigu.gmall.model.product.BaseTrademark;
import com.atguigu.gmall.product.service.BaseCategoryTrademarkService;
import com.atguigu.gmall.product.service.BaseTrademarkService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @author 24657
 * @apiNote
 * @date 2023/7/26 21:28
 */
@Api(tags = "品牌管理")
@RestController
@RequestMapping("/admin/product/baseTrademark")
public class BaseTrademarkController {
    @Autowired
    private BaseTrademarkService baseTrademarkService;

    /*
     * 获取品牌分页列表
     * */
    @ApiOperation(value = "分页列表")
    @GetMapping("{page}/{limit}")
    public Result index(@PathVariable("page") Long page,
                        @PathVariable("limit") Long limit){
        Page<BaseTrademark> pageParm = new Page<>(page, limit);
        IPage<BaseTrademark> pageModel=baseTrademarkService.getPage(pageParm);
        return Result.ok(pageModel);
    }
    /*
     * 删除BaseTrademark
     * */
    @ApiOperation(value = "删除BaseTrademark")
    @DeleteMapping("remove/{id}")
    public  Result remove(@PathVariable Long id){
        baseTrademarkService.removeById(id);
        return Result.ok();
    }
    /*
    * 新增BaseTrademark
    * */
    @ApiOperation(value = "新增BaseTrademark")
    @PostMapping("save")
    public Result save(@RequestBody BaseTrademark baseTrademark){
        baseTrademarkService.save(baseTrademark);
        return Result.ok();
    }
    /*
     * 修改BaseTrademark
     * */
    @ApiOperation(value = "修改BaseTrademark")
    @PutMapping("update")
    public  Result update(@RequestBody BaseTrademark baseTrademark){
        baseTrademarkService.update(baseTrademark);
        return Result.ok();
    }
    /*
     * 根据id获取品牌
     * */
    @ApiOperation(value = "获取BaseTrademark")
    @GetMapping("get/{id}")
    public  Result get(@PathVariable String id){
        BaseTrademark baseTrademark=baseTrademarkService.getById(id);
        return Result.ok(baseTrademark);
    }

}
