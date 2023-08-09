package com.atguigu.gmall.list.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.list.repository.GoodsRepository;
import com.atguigu.gmall.list.service.SearchService;
import com.atguigu.gmall.model.list.*;
import com.atguigu.gmall.model.product.BaseAttrInfo;
import com.atguigu.gmall.model.product.BaseCategoryView;
import com.atguigu.gmall.model.product.BaseTrademark;
import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.product.client.ProductFeignClient;
import com.sun.deploy.ui.ProgressDialog;
import org.apache.lucene.search.join.ScoreMode;
import org.aspectj.weaver.ast.Var;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.smartcardio.ATR;
import javax.swing.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author 24657
 * @apiNote
 * @date 2023/8/7 16:17
 */
@Service
@SuppressWarnings("all")
public class SearchServiceImpl implements SearchService {

    @Autowired
    private ProductFeignClient productFeignClient;
    @Autowired
    private RedisTemplate redisTemplate;

    //es高级客户端类
    @Autowired
    private RestHighLevelClient restHighLevelClient;
    /**
     * 搜索商品
     * @param searchParam
     * @return
     * @throws IOException
     */
    @Override
    public SearchResponseVo search(SearchParam searchParam) throws IOException {
        //构建dsl语句
        SearchRequest searchRequest=this.buildQueryDsl(searchParam);
        SearchResponse response = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        System.out.println(response);
        //反编组 将response转换成对应烦返回实体类
        SearchResponseVo responseVo=this.parseSearchResult(response);
        //补全返回实体类中的具体信息
        responseVo.setPageNo(searchParam.getPageNo());
        responseVo.setPageSize(searchParam.getPageSize());
        //获取总页数
        long totalPage = (responseVo.getTotal() + searchParam.getPageSize() - 1) / searchParam.getPageSize();
        responseVo.setTotalPages(totalPage);
        return responseVo;
    }
    //反编组 将返回的 SearchResponse类型转换成对应的实体类类型
    /*
    * 聚合的返回值类型
    *  "aggregations": {
    "tmIdAgg": {
      "doc_count_error_upper_bound": 0,
      "sum_other_doc_count": 0,
      "buckets": [
        {
          "key": 1,
          "doc_count": 4,
          "tmNameAgg": {
            "doc_count_error_upper_bound": 0,
            "sum_other_doc_count": 0,
            "buckets": [
              {
                "key": "小米",
                "doc_count": 4
              }
            ]
          },
          "tmLogoUrlAgg": {
            "doc_count_error_upper_bound": 0,
            "sum_other_doc_count": 0,
            "buckets": [
              {
                "key": "http://192.168.56.101:9000/gmall/16907923318504dbxiaomi.png",
                "doc_count": 4
              }
            ]
          }
        },
        {
          "key": 3,
          "doc_count": 4,
          "tmNameAgg": {
            "doc_count_error_upper_bound": 0,
            "sum_other_doc_count": 0,
            "buckets": [
              {
                "key": "华为",
                "doc_count": 4
              }
            ]
          },
          "tmLogoUrlAgg": {
            "doc_count_error_upper_bound": 0,
            "sum_other_doc_count": 0,
            "buckets": [
              {
                "key": "http://192.168.56.101:9000/gmall/1690792343667b70华为.png",
                "doc_count": 4
              }
            ]
          }
        }
      ]
    },
    "attrsAgg": {
      "doc_count": 56,
      "attrIdAgg": {
        "doc_count_error_upper_bound": 0,
        "sum_other_doc_count": 0,
        "buckets": [
          {
            "key": 23,
            "doc_count": 8,
            "attrNameAgg": {
              "doc_count_error_upper_bound": 0,
              "sum_other_doc_count": 0,
              "buckets": [
                {
                  "key": "运行内存",
                  "doc_count": 8
                }
              ]
            },
            "attrValueAgg": {
              "doc_count_error_upper_bound": 0,
              "sum_other_doc_count": 0,
              "buckets": [
                {
                  "key": "6G",
                  "doc_count": 4
                },
                {
                  "key": "12G",
                  "doc_count": 2
                },
                {
                  "key": "8G",
                  "doc_count": 2
                }
              ]
            }
          },
    * */
    private SearchResponseVo parseSearchResult(SearchResponse response) {
        SearchHits hits = response.getHits();
        //创建返回对象
        SearchResponseVo responseVo = new SearchResponseVo();
        //获取品牌的集合
        Map<String, Aggregation> aggregationMap  = response.getAggregations().asMap();
        ParsedLongTerms tmIdAgg = (ParsedLongTerms )aggregationMap.get("tmIdAgg");
        //获得SearchResponseTmVo列表
        List<SearchResponseTmVo> trademarkList  = tmIdAgg.getBuckets().stream().map(bucket -> {
            SearchResponseTmVo trademark  = new SearchResponseTmVo();
            //获取品牌id
            trademark.setTmId(Long.parseLong(bucket.getKeyAsString()));
            ParsedStringTerms tmNameAgg = (ParsedStringTerms )bucket.getAggregations().asMap().get("tmNameAgg");
            //设置品牌名称
            trademark.setTmName(tmNameAgg.getBuckets().get(0).getKeyAsString());
            ParsedStringTerms tmLogoUrlAgg = (ParsedStringTerms)bucket.getAggregations().asMap().get("tmLogoUrlAgg");
            //设置品牌图片
            trademark.setTmLogoUrl(tmLogoUrlAgg.getBuckets().get(0).getKeyAsString());
            return trademark;
        }).collect(Collectors.toList());
        responseVo.setTrademarkList(trademarkList);
        //赋值商品列表
        SearchHit[] subHits = hits.getHits();
        ArrayList<Goods> goodsList = new ArrayList<>();
        //商品列表不为空进行遍历
        if (subHits!=null && subHits.length>0){
            //循环遍历
            for (SearchHit subHit : subHits) {
                //将subHit转换成商品对象 将json类型转换成对类型
                Goods goods = JSONObject.parseObject(subHit.getSourceAsString(), Goods.class);
                //获取高亮
                if (subHit.getHighlightFields().get("title") != null) {
                    Text title = subHit.getHighlightFields().get("title").getFragments()[0];
                    goods.setTitle(title.toString());
                }
                goodsList.add(goods);
            }
        }
        responseVo.setGoodsList(goodsList);
        //获取平台属性数据 平台属性是嵌套结构
        /*
        * attrsAgg>attrIdAgg>[{attrNameAgg,attrValueAgg},{attrNameAgg,attrValueAgg},{...}]
        * */
        ParsedNested attrAgg =(ParsedNested ) aggregationMap.get("attrAgg");
        ParsedLongTerms  attrIdAgg = attrAgg.getAggregations().get("attrIdAgg");
        List<? extends Terms.Bucket> buckets = attrIdAgg.getBuckets();
        if (!CollectionUtils.isEmpty(buckets)) {
            List<SearchResponseAttrVo> searchResponseAttrVos = buckets.stream().map(bucket -> {
                SearchResponseAttrVo searchResponseAttrVo = new SearchResponseAttrVo();
                searchResponseAttrVo.setAttrId(Long.parseLong(bucket.getKeyAsString()));
                ParsedStringTerms  attrNameAgg =(ParsedStringTerms )bucket.getAggregations().asMap().get("attrNameAgg");
                //获取attrName平台属性名称
                String attrName = attrNameAgg.getBuckets().get(0).getKeyAsString();
                searchResponseAttrVo.setAttrName(attrName);
                //获取平台属性值
                ParsedStringTerms  attrValueAgg =(ParsedStringTerms) bucket.getAggregations().asMap().get("attrValueAgg");
                List<String> values = attrValueAgg.getBuckets().stream().map(subbucket -> {
                   return  subbucket.getKeyAsString();
                }).collect(Collectors.toList());
                searchResponseAttrVo.setAttrValueList(values);
                return searchResponseAttrVo;
            }).collect(Collectors.toList());
            responseVo.setAttrsList(searchResponseAttrVos);
        }
        //获取总记录数
        responseVo.setTotal(hits.getTotalHits().value);
        return responseVo;
    }

    //构建dsl语句
    private SearchRequest buildQueryDsl(SearchParam searchParam) {
        //构建查询器
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        //构建boolQueryBuilder
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        //判断查询条件是否为空关键子 关键字查询
        if (!StringUtils.isEmpty(searchParam.getKeyword())) {
            //小米手机  小米and手机
            //构建MatchQueryBuiler  match查询是不进行分词的 与term的区别 term相当于 where= term是要进行分词的 加上operator（operator.and） 表示key不可分割
            MatchQueryBuilder title = QueryBuilders.matchQuery("title", searchParam.getKeyword()).operator(Operator.AND);
            boolQueryBuilder.must(title);
        }
        //构建品牌查询
        String trademark = searchParam.getTrademark();
        if (!StringUtils.isEmpty(trademark)) {
            //trademark=2:华为
            String[] split = StringUtils.split(trademark, ":");
            if(split!=null&&split.length==2){
                //根据品牌id进行过滤
                /*
                * filter 和query的区别 filter只查询出搜索条件的数据 ，不计算相关度分数
                * query要计算相关度分数，按照分数进行排序
                * filter比query的性能好，两者可以一起使用
                * */
                boolQueryBuilder.filter(QueryBuilders.termQuery("tmId",split[0]));
            }
        }
        //构建分类过滤 用户在点击的时候，只能点击一个值，所以此处用term term相当于where=？ ，terms相当于 where in
        if(null!=searchParam.getCategory1Id()){
            boolQueryBuilder.filter(QueryBuilders.termQuery("category1Id",searchParam.getCategory1Id()));
        }
        // 构建分类过滤
        if(null!=searchParam.getCategory2Id()){
            boolQueryBuilder.filter(QueryBuilders.termQuery("category2Id",searchParam.getCategory2Id()));
        }
        // 构建分类过滤
        if(null!=searchParam.getCategory3Id()){
            boolQueryBuilder.filter(QueryBuilders.termQuery("category3Id",searchParam.getCategory3Id()));
        }
        //构建平台属性查询 23:4G:运行内存
        String[] props = searchParam.getProps();
        if(props!=null&&props.length>0){
            //遍历循环 数组中存放 23:4G:运行内存 类似的字符串
            //props=["23:4G:运行内存 ","28:6.9寸:品目尺寸","29:128G:机身内存"]
            for (String prop : props) {
                String[] split = prop.split(":");
                if(split!=null&&split.length==3){
                    //构建嵌套查询
                    BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
                    //嵌套查询子查询
                    BoolQueryBuilder subBoolQuery  = QueryBuilders.boolQuery();
                    //构建子查询中的过滤条件
                    subBoolQuery.must(QueryBuilders.termQuery("attrs.attrId",split[0]));
                    subBoolQuery.must(QueryBuilders.termQuery("attrs.attrValue",split[1]));
                    boolQuery.must(QueryBuilders.nestedQuery("attrs",subBoolQuery, ScoreMode.None));
                    //添加到整个过滤对象中
                    boolQueryBuilder.filter(boolQuery);
                }
            }
        }
        //执行查询方法
        searchSourceBuilder.query(boolQueryBuilder);
        //构建分页
        int from = (searchParam.getPageNo()-1)*searchParam.getPageSize();
        searchSourceBuilder.from(from);
        searchSourceBuilder.size(searchParam.getPageSize());
        //排序
        String order = searchParam.getOrder();
        if (!StringUtils.isEmpty(order)) {
            //判断排序规则 1：asc 1：desc , 2：asc 2：desc
            String[] split = StringUtils.split(order, ":");
            if(split!=null&&split.length==2){
                //排序的字段
                String field=null;
                //数组中的第一个参数
                switch (split[0]){
                    case "1":
                        field="hotScore";
                        break;
                    case "2":
                        field="price";
                        break;
                }
                searchSourceBuilder.sort(field,"asc".equals(split[1])? SortOrder.ASC:SortOrder.DESC);
            }else {
                //没有传值的时候给默认值
                searchSourceBuilder.sort("hotScore",SortOrder.DESC);
            }
        }
        //构建高亮
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.field("title");
        highlightBuilder.postTags("</span>");
        highlightBuilder.preTags("<span style=color:red>");
        searchSourceBuilder.highlighter(highlightBuilder);
        //设置品牌聚合
        TermsAggregationBuilder termsAggregationBuilder = AggregationBuilders.terms("tmIdAgg").field("tmId")
                .subAggregation(AggregationBuilders.terms("tmNameAgg").field("tmName"))
                .subAggregation(AggregationBuilders.terms("tmLogoUrlAgg").field("tmLogoUrl"));
        searchSourceBuilder.aggregation(termsAggregationBuilder);
        //设置平台属性聚合
        searchSourceBuilder.aggregation(AggregationBuilders.nested("attrAgg","attrs")
                .subAggregation(AggregationBuilders.terms("attrIdAgg").field("attrs.attrId")
                        .subAggregation(AggregationBuilders.terms("attrNameAgg").field("attrs.attrName"))
                        .subAggregation(AggregationBuilders.terms("attrValueAgg").field("attrs.attrValue"))
                )
        );
        //结果集合过滤  需要的结果和排除的结果
        searchSourceBuilder.fetchSource(new String[]{"id","defaultImg","title","price"},null);
        SearchRequest searchRequest = new SearchRequest("goods");
        searchRequest.source(searchSourceBuilder);
        System.out.println( "dsl:" + searchSourceBuilder.toString());
        return searchRequest;
    }

    /**
     * 更新商品incrHotScore
     * @param skuIdI
     * @return
     */
    @Override
    public void incrHotScore(Long skuId) {
        //定义key 保存 每个商品的点击率 每增加10个保存一次
        String hotKey="hotScore";
        Double hotScore = redisTemplate.opsForZSet().incrementScore(hotKey, "skuId:" + skuId, 1);
        if (hotScore%10==0) {
            //更新es
            Goods goods = goodsRepository.findById(skuId).get();
            goods.setHotScore(Math.round(hotScore));
            goodsRepository.save(goods);
        }
    }

    /**
     * 上架商品列表 将数据存到elasticSearch
     * @param skuId
     */
    @Autowired
    private GoodsRepository goodsRepository;
    @Override
    public void upperGoods(Long skuId) {
        Goods goods = new Goods();
        //查询sku对应得平台属性
        List<BaseAttrInfo> attrList = productFeignClient.getAttrList(skuId);
        List<SearchAttr> searchAttrList = attrList.stream().map(baseAttrInfo -> {
            SearchAttr searchAttr = new SearchAttr();
            searchAttr.setAttrId(baseAttrInfo.getId());
            searchAttr.setAttrName(baseAttrInfo.getAttrName());
            searchAttr.setAttrValue(baseAttrInfo.getAttrValueList().get(0).getValueName());
            return searchAttr;
        }).collect(Collectors.toList());
        goods.setAttrs(searchAttrList);
        //查询sku信息
        SkuInfo skuInfo = productFeignClient.getAttrValueList(skuId);
        // 查询品牌
        BaseTrademark baseTrademark = productFeignClient.getTrademark(skuInfo.getTmId());
        if (baseTrademark != null){
            goods.setTmId(skuInfo.getTmId());
            goods.setTmName(baseTrademark.getTmName());
            goods.setTmLogoUrl(baseTrademark.getLogoUrl());
        }
        //查询分类
        BaseCategoryView categoryView = productFeignClient.getCategoryView(skuInfo.getCategory3Id());
        if (categoryView != null) {
            goods.setCategory1Id(categoryView.getCategory1Id());
            goods.setCategory1Name(categoryView.getCategory1Name());
            goods.setCategory2Id(categoryView.getCategory2Id());
            goods.setCategory2Name(categoryView.getCategory2Name());
            goods.setCategory3Id(categoryView.getCategory3Id());
            goods.setCategory3Name(categoryView.getCategory3Name());
        }
        goods.setDefaultImg(skuInfo.getSkuDefaultImg());
        goods.setPrice(skuInfo.getPrice().doubleValue());
        goods.setId(skuInfo.getId());
        goods.setTitle(skuInfo.getSkuName());
        goods.setCreateTime(new Date());
        this.goodsRepository.save(goods);
    }

    /**
     * 下架商品列表
     * @param skuId
     */
    @Override
    public void lowerGoods(Long skuId) {
        this.goodsRepository.deleteById(skuId);
    }
}
