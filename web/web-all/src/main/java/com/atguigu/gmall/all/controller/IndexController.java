package com.atguigu.gmall.all.controller;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.product.client.ProductFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import javax.servlet.http.HttpServletRequest;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author 24657
 * @apiNote
 * @date 2023/8/4 18:50
 */
@Controller
@SuppressWarnings("all")
public class IndexController {
    @Autowired
    private ProductFeignClient productFeignClient;
    @Autowired
    TemplateEngine templateEngine;

    /*
    * 缓存渲染
    * */
    @GetMapping({"/","index.html","index"})
    public String index(Model model){
        Result<List<JSONObject>> result = productFeignClient.getBaseCategoryList();
        model.addAttribute("list",result.getData());
        return "index/index";
    }
    /*
    *静态代理方式
    * */
    @GetMapping("createIndex")
    @ResponseBody
    public Result createIndex(){
        //获取后台存储数据
        Result<List<JSONObject>> result = productFeignClient.getBaseCategoryList();
        Context context = new Context();
        context.setVariable("list",result.getData());
        //定义文件输入位置
        FileWriter fileWriter=null;
        try {
            fileWriter = new FileWriter("C:\\Users\\24657\\Desktop\\staticpage\\index.html");
        } catch (IOException e) {
            e.printStackTrace();
        }
        templateEngine.process("index/index",context,fileWriter );
        return result.ok();
    }

}
