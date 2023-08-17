package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.util.AuthContextHolder;
import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.product.client.ProductFeignClient;
import org.bouncycastle.cert.ocsp.Req;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

/**
 * @author 24657
 * @apiNote
 * @date 2023/8/11 18:26
 */

@Controller
@SuppressWarnings("all")
public class CartController {

    @Autowired
    private ProductFeignClient productFeignClient;

    //跳转到添加购物车成功页面 提供页面模版需要的数据
    @RequestMapping("addCart.html")
    public String toAddCart(@RequestParam(value = "skuId") Long skuId, @RequestParam(value = "skuNum") Integer skuNum , Model model){
        /*
        * 需要的参数
        * skuInfo
        * skuNum
        * 传入的参数有
        * skuId skuNum
        * */
        model.addAttribute("skuNum",skuNum);
        SkuInfo skuInfo = productFeignClient.getAttrValueList(skuId);
        model.addAttribute("skuInfo",skuInfo);
        return "cart/addCart.html";
    }
    //跳转到购物车详情页面
    @RequestMapping("cart.html")
    public String toCart(){
        //这里直接跳转到index购物车页面，该页面调用vue的钩子方法，调用后端service-cart服务的接口，返回result对象 在进行页面渲染
        return "cart/index.html";
    }
}
