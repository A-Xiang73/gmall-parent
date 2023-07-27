package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.service.ManageService;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author 24657
 * @apiNote
 * @date 2023/7/25 15:26
 */
@Api(tags = "商品基础属性接口")
@RestController
@RequestMapping("admin/product")
public class BaseManagerController {
    @Autowired
    private ManageService manageService;


    /* 接口
    选中准修改数据 ， 根据该attrId 去查找AttrInfo，该对象下 List<BaseAttrValue> ！
    所以在返回的时候，需要返回BaseAttrInfo*/
    /**
     * 根据attrId 查询平台属性对象
     * @param attrId
     * @return
     */
    @GetMapping("getAttrValueList/{attrId}")
    public Result<List<BaseAttrValue>> getAttrValueList(@PathVariable Long attrId){

        BaseAttrInfo baseAttrInfo=manageService.getAttrInfo(attrId);
        List<BaseAttrValue> list=baseAttrInfo.getAttrValueList();
        return  Result.ok(list);
    }


    /**
     * 保存-更新平台属性方法
     * @param baseAttrInfo
     * @return
     */
    @PostMapping("saveAttrInfo")
    public  Result saveAttrInfo(@RequestBody BaseAttrInfo baseAttrInfo){
        manageService.saveAttrInfo(baseAttrInfo);
        return Result.ok();
    }



    /**
     * 根据分类Id 获取平台属性数据
     * @param category1Id
     * @param category2Id
     * @param category3Id
     * @return
     */
    @GetMapping("attrInfoList/{category1Id}/{category2Id}/{category3Id}")
    public Result<List<BaseAttrInfo>>  attrInfoList(@PathVariable Long category1Id,
                                              @PathVariable Long category2Id,
                                              @PathVariable Long category3Id
                                              ){
        List<BaseAttrInfo> list=manageService.getAttrInfoList(category1Id,category2Id,category3Id);
        return Result.ok(list);
    }


    /**
     * 查询所有的一级分类信息
     * @return
     */
    @GetMapping("getCategory1")
    public Result<List<BaseCategory1>> getCategory1(){
        List<BaseCategory1> list=manageService.getCategory1();
        return  Result.ok(list);
    }
    /**
     * 查询所有的二级分类信息
     * @return
     */
    @GetMapping("getCategory2/{category1Id}")
    public Result<List<BaseCategory2>> getCategory2(@PathVariable Long category1Id){
        List<BaseCategory2> list=manageService.getCategory2(category1Id);
        return  Result.ok(list);
    }
    /**
     * 查询所有的三级分类信息
     * @return
     */
    @GetMapping("getCategory3/{category2Id}")
    public Result<List<BaseCategory3>> getCategory3(@PathVariable Long category2Id){
        List<BaseCategory3> list=manageService.getCategory3(category2Id);
        return  Result.ok(list);
    }
}
