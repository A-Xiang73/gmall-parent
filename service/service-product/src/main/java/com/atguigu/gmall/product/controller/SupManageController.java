package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.SpuInfo;
import com.atguigu.gmall.product.service.ManageService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.lettuce.core.Limit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author 24657
 * @apiNote
 * @date 2023/7/26 20:58
 */
@RestController // @ResponseBody + @Controller
@RequestMapping("admin/product")
public class SupManageController {
    @Autowired
    private ManageService manageService;
    // 根据查询条件封装控制器
    // springMVC 的时候，有个叫对象属性传值 如果页面提交过来的参数与实体类的参数一致，
    // 则可以使用实体类来接收数据
    // http://api.gmall.com/admin/product/1/10?category3Id=61
    // @RequestBody 作用 将前台传递过来的json{"category3Id":"61"}  字符串变为java 对象。
    @GetMapping("/{page}/{size}")
    public Result getSpuInfoPage(@PathVariable Long page,
                                 @PathVariable Long size,
                                 SpuInfo spuInfo){//通过问号传递的参数可以直接接收只要参数名一样，也可以直接用对象接收
        Page<SpuInfo> spuInfoPage = new Page<>(page, size);
        IPage<SpuInfo> ipage=manageService.getSpuInfoPage(spuInfoPage,spuInfo);
        return Result.ok(ipage);
    }
}
