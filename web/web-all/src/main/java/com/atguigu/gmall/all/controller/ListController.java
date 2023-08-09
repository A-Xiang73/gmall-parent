package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.list.client.ListFeignClient;
import com.atguigu.gmall.model.list.SearchParam;
import org.aspectj.weaver.ast.Var;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import sun.security.provider.ConfigFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author 24657
 * @apiNote
 * @date 2023/8/8 20:37
 */
@Controller
@SuppressWarnings("all")
public class ListController {
    @Autowired
    private ListFeignClient  listFeignClient;
    @GetMapping("list.html")
    public String search(SearchParam searchParam, Model model){
        Result<Map> result = listFeignClient.list(searchParam);
        model.addAllAttributes(result.getData());
        //记录拼接url
        String urlParam=makeUrlParam(searchParam);
        //处理品牌条件回显 ---面包屑显示
        String trademarkParam= this.makeTrademark(searchParam.getTrademark());
        //处理品牌条件回显 23:4G:运行内存
        List<Map<String,String>> propsParamList=this.makeProps(searchParam.getProps());
        //处理排序  1:hotScore 2:price  2热度 1综合
        Map<String,Object> orderMap=this.dealOrder(searchParam.getOrder());
        //需要searchParam中的order参数进行排序 order有默认值
        model.addAttribute("searchParam",searchParam);
        model.addAttribute("urlParam",urlParam);
        model.addAttribute("trademarkParam",trademarkParam);
        model.addAttribute("propsParamList",propsParamList);
        model.addAttribute("orderMap",orderMap);
        return "list/index";
    }

    //处理排序  1:hotScore 2:price  2热度 1综合
    private Map<String, Object> dealOrder(String order) {
        Map<String,Object> orderMap=new HashMap<>();
        if (!StringUtils.isEmpty(order)) {
            String[] split = order.split(":");
            orderMap.put("type", split[0]);
            orderMap.put("sort",split[1]);
        }else {
            orderMap.put("type", 2);
            orderMap.put("sort","desc");
        }
        return orderMap;
    }

    //处理品牌条件回显 23:4G:运行内存
    private List<Map<String, String>> makeProps(String[] props) {
        List<Map<String, String>> propsParamList=new ArrayList<>();
        if (props != null&&props.length>0) {
            for (String prop : props) {
                Map<String,String> map= new HashMap<>();
                String[] split = prop.split(":");
                if (split!=null&&split.length==3) {
                    map.put("attrId",split[0]);
                    map.put("attrValue",split[1]);
                    map.put("attrName",split[2]);
                    propsParamList.add(map);
                }
            }
        }
        return propsParamList;
    }

    //处理品牌回显 trademark=2:华为
    private String makeTrademark(String trademark) {
        //trademark=2:华为
        if (trademark != null) {
            String[] split = trademark.split(":");
            if (split!=null&&split.length==2) {
                return "品牌:"+split[1];
            }
        }
        return "";
    }

    /*
    * 一级参数？
     * http://list.gmall.com/list.html?category3Id=61
     * http://list.gmall.com/list.html?keyword=%E6%89%8B%E6%9C%BA
     * 拼接urlParam路径
     * 二级参数&
     * &trademark=1:小米
     * &props=23:6G:运行内存&props=24:128G:机身内存
    * */
    private String makeUrlParam(SearchParam searchParam) {
        StringBuilder urlParam = new StringBuilder();
        //最开始只有两个接口 关键字进入或则类型进入
        if (searchParam.getKeyword() != null) {
            urlParam.append("keyword=").append(searchParam.getKeyword());
        }
        //判断一级分类
        if (searchParam.getCategory1Id() != null) {
            urlParam.append("category1Id=").append(searchParam.getCategory1Id());
        }
        //判断二级分类
        if (searchParam.getCategory2Id() != null) {
            urlParam.append("category2Id=").append(searchParam.getCategory2Id());
        }
        //判断三级分类
        if (searchParam.getCategory3Id() != null) {
            urlParam.append("category3Id=").append(searchParam.getCategory3Id());
        }
        //处理品牌
        if (searchParam.getTrademark() != null) {
            if (urlParam.length() > 0) {
                //不是第一级别所以要加&连接
                urlParam.append("&trademark=").append(searchParam.getTrademark());
            }
        }
        //判断平台属性值
        if (searchParam.getProps()!=null) {
            for (String prop : searchParam.getProps()) {
                if (urlParam.length()>0) {
                    urlParam.append("&props=").append(prop);
                }
            }
        }
        return "list.html?"+urlParam.toString();
    }

}
