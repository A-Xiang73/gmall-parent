package com.atguigu.gmall.product.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.common.cache.GmallCache;
import com.atguigu.gmall.common.config.RedisConfig;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.mapper.*;
import com.atguigu.gmall.product.service.ManageService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.lettuce.core.RedisClient;
import lombok.experimental.PackagePrivate;
import org.aspectj.weaver.ast.Var;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author 24657
 * @apiNote
 * @date 2023/7/25 16:25
 */
@Service
public class ManageServiceImpl implements ManageService {

    @Autowired
    private BaseCategory1Mapper baseCategory1Mapper;
    @Autowired
    private BaseCategory2Mapper baseCategory2Mapper;
    @Autowired
    private BaseCategory3Mapper baseCategory3Mapper;
    @Autowired
    private BaseAttrInfoMapper baseAttrInfoMapper;
    @Autowired
    private BaseAttrValueMapper baseAttrValueMapper;

    @Autowired
    private SpuInfoMapper spuInfoMapper;
    @Autowired
    private SpuImageMapper spuImageMapper;
    @Autowired
    private SpuSaleAttrMapper spuSaleAttrMapper;
    @Autowired
    private SpuSaleAttrValueMapper spuSaleAttrValueMapper;
    @Autowired
    private SpuPosterMapper spuPosterMapper;
    @Autowired
    private  BaseSaleAttrMapper baseSaleAttrMapper;
    @Autowired
    private SkuInfoMapper skuInfoMapper;
    @Autowired
    private SkuImageMapper skuImageMapper;
    @Autowired
    private SkuSaleAttrValueMapper skuSaleAttrValueMapper;
    @Autowired
    private SkuAttrValueMapper skuAttrValueMapper;
    @Autowired
    private BaseCategoryViewMapper baseCategoryViewMapper;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private RedissonClient redissonClient;
    /**
     * 获取返回主页的全部分类信息
     * @return
     */
    @GmallCache(prefix = "category:",suffix = ":info")
    @Override
    public List<JSONObject> getBaseCategoryList() {
        List<BaseCategoryView> baseCategoryViewList = baseCategoryViewMapper.selectList(null);
        Map<Long, List<BaseCategoryView>> categoryid1Group = baseCategoryViewList.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory1Id));
        List<JSONObject> result=new ArrayList<>();
        int index=1;
        for (Map.Entry<Long, List<BaseCategoryView>> categoryId1Entry : categoryid1Group.entrySet()) {
            JSONObject category1Map = new JSONObject();
            Long category1Id = categoryId1Entry.getKey();
            category1Map.put("categoryId",category1Id);
            category1Map.put("index",index);
            index++;
            List<BaseCategoryView> category2List = categoryId1Entry.getValue();
            String category1Name = category2List.get(0).getCategory1Name();
            category1Map.put("categoryName",category1Name);
            List<JSONObject> category1Child=new ArrayList<>();
            //遍历二级分类
            Map<Long, List<BaseCategoryView>> categoryid2Group = category2List.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory2Id));
            for (Map.Entry<Long, List<BaseCategoryView>> categoryId2Entry : categoryid2Group.entrySet()) {
                JSONObject category2Map = new JSONObject();
                Long category2Id = categoryId2Entry.getKey();
                category2Map.put("categoryId",category2Id);
                List<BaseCategoryView> category3List = categoryId2Entry.getValue();
                String category2Name = category3List.get(0).getCategory2Name();
                category2Map.put("categoryName",category2Name);
                List<JSONObject> category2Child=new ArrayList<>();
                //遍历三级分类
                for (BaseCategoryView category3 : category3List) {
                    JSONObject category3Map = new JSONObject();
                    Long category3Id = category3.getCategory3Id();
                    category3Map.put("categoryId",category3Id);
                    String category3Name = category3.getCategory3Name();
                    category3Map.put("categoryName",category3Name);
                    category2Child.add(category3Map);
                }
                category2Map.put("categoryChild",category2Child);
                category1Child.add(category2Map);
            }
            category1Map.put("categoryChild",category1Child);
            result.add(category1Map);
        }
        return result;
    }
    /**
     * 通过skuId 集合来查询数据
     * @param skuId
     * @return 根据skuid获取平台属性集合
     * */
    @Override
    @GmallCache(prefix = "attrlist:",suffix = ":list")
    public List<BaseAttrInfo> getAttrList(Long skuId) {
        return baseAttrInfoMapper.selectBaseAttrInfoListBySkuId(skuId);
    }


    //  根据spuId 获取海报数据
    @Override
    @GmallCache(prefix = "spuPoster:",suffix = ":poster")
    public List<SpuPoster> findSpuPosterBySpuId(Long spuId) {
        QueryWrapper<SpuPoster> spuInfoQueryWrapper = new QueryWrapper<>();
        spuInfoQueryWrapper.eq("spu_id",spuId);
        List<SpuPoster> spuPosterList = spuPosterMapper.selectList(spuInfoQueryWrapper);
        return spuPosterList;
    }

    /**
     * 根据spuId 查询map 集合属性 根据key值查询skuid 切换sku
     * // key = 125|123 ,value = 37
     * @param spuId
     * @return
     */
    @Override
    @GmallCache(prefix = "valueIds:",suffix = ":ids")
    public Map getSkuValueIdsMap(Long spuId) {
        Map<String, String> map = new HashMap<>();
        List<Map> mapList=skuSaleAttrValueMapper.selectSaleAttrValues(spuId);
        mapList.forEach(
                skuMap->{
                    map.put(String.valueOf(skuMap.get("value_ids")),String.valueOf(skuMap.get("sku_id")));
                }
        );
        return map;
    }

    /**
     * 根据spuId，skuId 查询销售属性集合 以及选中的关系
     * @param skuId
     * @param spuId
     * @return
     */
    @Override
    @GmallCache(prefix = "attrIsCheck:",suffix = ":check")
    public List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(Long skuId, Long spuId) {
        return spuSaleAttrMapper.selectSpuSaleAttrListCheckBySku(skuId, spuId);
    }

    /**
     * 获取sku最新价格
     * @param skuId
     * @return
     */
    @Override
    public BigDecimal getSkuPrice(Long skuId) {

        SkuInfo skuInfo = skuInfoMapper.selectById(skuId);
        if (skuInfo != null) {
             return skuInfo.getPrice();
        }
        return new BigDecimal("0");
    }

    /**
     * 通过三级分类id查询分类信息
     * @param category3Id
     * @return
     */
    @Override
    @GmallCache(prefix = "cateGoryView:",suffix = ":view")
    public BaseCategoryView getCategoryViewByCategory3Id(Long category3Id) {
        return baseCategoryViewMapper.selectById(category3Id);
    }

    /**
     * 根据skuId获取sku信息
     * @param skuId
     * @return
     */
    /**
     * 使用redis作分布式锁
     * 1.创建锁key
     * 2.根据key从redis中获取值
     *      获取到了，直接返回
     *      获取不到，则从数据库取
     *          创建锁key
     *          获取锁，获取成功
     *              操作数据库
     *              获取数据，
     *                  若获取到数据，将数据存到redis，返回相应数据
     *              获取不到数据
     *                  返回一个空数据，存到redis缓存中，防止缓存穿透
     *              释放锁
     *          获取失败，进行自旋
     *       设置保底操作，若上述操作发生异常则直接从数据库进行获取
     */
    @Override
    @GmallCache(prefix = "sku:",suffix = ":info")
    public SkuInfo getSkuInfo(Long skuId) {
        SkuInfo skuInfo = skuInfoMapper.selectById(skuId);
        // 根据skuId 查询图片列表集合
        QueryWrapper<SkuImage> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("sku_id", skuId);
        List<SkuImage> skuImageList = skuImageMapper.selectList(queryWrapper);

        skuInfo.setSkuImageList(skuImageList);
        return skuInfo;
    }

    /*
    * 使用redisson作分布式锁
    * */
    private SkuInfo getSkuInfoRedisson(Long skuId) {
        try {
            //使用redis作分布式锁
            SkuInfo skuInfo=null;
            //缓存存储数据 key value
            String skuKey= RedisConst.SKUKEY_PREFIX+ skuId +RedisConst.SKUKEY_SUFFIX;
            //根据skuKey从redis中获取
            skuInfo = (SkuInfo) redisTemplate.opsForValue().get(skuKey);
            //判断skuInfo是否为空
            if (skuInfo != null) {
                //获取到了,直接返回数据
                return skuInfo;
            }
            //获取不到进行数据库操作，穿件锁key
            String skuLock=RedisConst.SKUKEY_PREFIX+ skuId +RedisConst.SKULOCK_SUFFIX;
            RLock lock = redissonClient.getLock(skuLock);
            boolean isQuier = lock.tryLock(RedisConst.SKULOCK_EXPIRE_PX2, RedisConst.SKULOCK_EXPIRE_PX1, TimeUnit.SECONDS);
            //判断是否获得了锁
            if (isQuier) {
                try {
                    //获得了锁，进行数据库操作
                    skuInfo = getInfo(skuId);
                    //如果查到的数据为空，为了避免缓存穿透，存储空的数据放入缓存
                    if (skuInfo == null) {
                        SkuInfo skuNull = new SkuInfo();
                        redisTemplate.opsForValue().set(skuKey,skuNull,RedisConst.SECKILL__TIMEOUT,TimeUnit.SECONDS);
                        return skuNull;
                    }
                    //查数据库有值，将其存入redis缓存
                    redisTemplate.opsForValue().set(skuKey,skuInfo,RedisConst.SECKILL__TIMEOUT,TimeUnit.SECONDS);
                    return skuInfo;
                } finally {
                    lock.unlock();
                }
            }else{
                //获取失败，睡眠自旋
                try {
                    Thread.sleep(100);
                    return getSkuInfo(skuId);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return getInfo(skuId);
    }

    //使用redis做分布式锁
    private SkuInfo getSkuInfoRedis(Long skuId) {
        try {
            //使用redis作分布式锁
            SkuInfo skuInfo=null;
            //缓存存储数据 key value
            String skuKey= RedisConst.SKUKEY_PREFIX+ skuId +RedisConst.SKUKEY_SUFFIX;
            //根据skuKey从redis中获取
            skuInfo = (SkuInfo) redisTemplate.opsForValue().get(skuKey);
            //判断skuInfo是否为空
            if (skuInfo != null) {
                //获取到了,直接返回数据
                return skuInfo;
            }
            //获取不到进行数据库操作，穿件锁key
            String skuLock=RedisConst.SKUKEY_PREFIX+ skuId +RedisConst.SKULOCK_SUFFIX;
            //setIfAbsent就是setnx
            String uuid = UUID.randomUUID().toString().substring(0, 10);
            Boolean isQuier = redisTemplate.opsForValue().setIfAbsent(skuLock, uuid, RedisConst.SECKILL__TIMEOUT, TimeUnit.SECONDS);
            //判断是否获得了锁
            if (isQuier) {
                //获得了锁，进行数据库操作
                 skuInfo = getInfo(skuId);
                 //如果查到的数据为空，为了避免缓存穿透，存储空的数据放入缓存
                if (skuInfo == null) {
                    SkuInfo skuNull = new SkuInfo();
                    redisTemplate.opsForValue().set(skuKey,skuNull,RedisConst.SECKILL__TIMEOUT,TimeUnit.SECONDS);
                    return skuNull;
                }
                //查数据库有值，将其存入redis缓存
                redisTemplate.opsForValue().set(skuKey,skuInfo,RedisConst.SECKILL__TIMEOUT,TimeUnit.SECONDS);
                 //释放锁 使用lua脚本
                String luaScript="if redis.call(\"get\",KEYS[1]) == ARGV[1]\n" +
                        "then\n" +
                        "    return redis.call(\"del\",KEYS[1])\n" +
                        "else\n" +
                        "    return 0\n" +
                        "end";
                //准备执行脚本
                DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
                //将lua放到DefaultRedisScript对象中
                redisScript.setScriptText(luaScript);
                //设置DefaultRedisScrupt这个对象的泛型
                redisScript.setResultType(Long.class);
                //执行删除
                redisTemplate.execute(redisScript,Arrays.asList(skuLock),uuid);
                return skuInfo;
            }else{
                //获取失败，睡眠自旋
                try {
                    Thread.sleep(100);
                    return getSkuInfo(skuId);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return getInfo(skuId);
    }

    private SkuInfo getInfo(Long skuId) {
        SkuInfo skuInfo = skuInfoMapper.selectById(skuId);
        // 根据skuId 查询图片列表集合
        QueryWrapper<SkuImage> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("sku_id", skuId);
        List<SkuImage> skuImageList = skuImageMapper.selectList(queryWrapper);

        skuInfo.setSkuImageList(skuImageList);
        return skuInfo;
    }

    /**
     * 商品上架
     * @param skuId
     * @return
     */
    @Override
    @Transactional
    public void onSale(Long skuId) {
        SkuInfo skuInfo = new SkuInfo();
        skuInfo.setId(skuId);
        skuInfo.setIsSale(1);
        skuInfoMapper.updateById(skuInfo);
    }
    /**
     * SKU分页列表
     * @param
     * @param
     * @return
     */
    @Override
    @Transactional
    public void cancelSale(Long skuId){
        SkuInfo skuInfo = new SkuInfo();
        skuInfo.setId(skuId);
        skuInfo.setIsSale(0);
        skuInfoMapper.updateById(skuInfo);
    }
    @Override
    public IPage<SkuInfo> getPage(Page<SkuInfo> pageParam) {
        QueryWrapper<SkuInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("id");
        Page<SkuInfo> skuInfoPage = skuInfoMapper.selectPage(pageParam, queryWrapper);
        return skuInfoPage;
    }

    /**
     * 保存sku
     * @param skuInfo
     * 使用到的表
     * sku_info
     * sku_image
     * sku_sale_attr_value
     * sku_attr_value
     * @return
     */
    @Override
    public void saveSkuInfo(SkuInfo skuInfo) {
        skuInfoMapper.insert(skuInfo);
        List<SkuImage> skuImageList = skuInfo.getSkuImageList();
        if (!CollectionUtils.isEmpty(skuImageList)) {
          skuImageList.stream().forEach(
                  skuImage -> {
                      skuImage.setSkuId(skuInfo.getId());
                      skuImageMapper.insert(skuImage);
                  }
          );
        }
        List<SkuAttrValue> skuAttrValueList = skuInfo.getSkuAttrValueList();
        if (!CollectionUtils.isEmpty(skuAttrValueList)) {
            skuAttrValueList.stream().forEach(
                    skuAttrValue -> {
                        skuAttrValue.setSkuId(skuInfo.getId());
                        skuAttrValueMapper.insert(skuAttrValue);
                    }
            );
        }
        List<SkuSaleAttrValue> skuSaleAttrValueList = skuInfo.getSkuSaleAttrValueList();
        if (!CollectionUtils.isEmpty(skuSaleAttrValueList)) {
            skuSaleAttrValueList.stream().forEach(
                    skuSaleAttrValue -> {
                        skuSaleAttrValue.setSkuId(skuInfo.getId());
                        skuSaleAttrValue.setSpuId(skuInfo.getSpuId());
                        skuSaleAttrValueMapper.insert(skuSaleAttrValue);
                    }
            );
        }
        //添加布隆过滤
        RBloomFilter<Long> rbloomFilter = redissonClient.getBloomFilter(RedisConst.SKU_BLOOM_FILTER);
        rbloomFilter.add(skuInfo.getId());
    }

    /**
     * 根据spuId 查询销售属性集合
     * @param spuId
     * @return
     */
    @Override
    public List<SpuSaleAttr> getSpuSaleAttrList(Long spuId) {
        return spuSaleAttrMapper.selectSpuSaleAttrList(spuId);
    }

    /**
     * 根据spuId 查询spuImageList
     * @param spuId
     * @return
     */

    @Override
    public List<SpuImage> getSpuImageList(Long spuId) {
//        SpuInfo spuInfo = spuInfoMapper.selectById(spuId);
//        return spuInfo.getSpuImageList();
        QueryWrapper<SpuImage> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("spu_id",spuId);
        List<SpuImage> spuImageList = spuImageMapper.selectList(queryWrapper);
        return spuImageList;
    }

    /*
            与spu相关的五张表
            spuInfo;
            spuImage;
            spuSaleAttr;
            spuSaleAttrValue;
            spuPoster
         */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveSpuInfo(SpuInfo spuInfo) {
        spuInfoMapper.insert(spuInfo);
        List<SpuImage> spuImageList = spuInfo.getSpuImageList();
        if (!CollectionUtils.isEmpty(spuImageList)) {
            for (SpuImage spuImage : spuImageList) {
                spuImage.setSpuId(spuInfo.getId());
                spuImageMapper.insert(spuImage);
            }
        }
        List<SpuSaleAttr> spuSaleAttrList = spuInfo.getSpuSaleAttrList();
        if (!CollectionUtils.isEmpty(spuImageList)) {
            for (SpuSaleAttr spuSaleAttr : spuSaleAttrList) {
                spuSaleAttr.setSpuId(spuInfo.getId());
                spuSaleAttrMapper.insert(spuSaleAttr);
                List<SpuSaleAttrValue> spuSaleAttrValueList = spuSaleAttr.getSpuSaleAttrValueList();
                if (!CollectionUtils.isEmpty(spuSaleAttrValueList)) {
                    for (SpuSaleAttrValue spuSaleAttrValue : spuSaleAttrValueList) {
                        spuSaleAttrValue.setSpuId(spuInfo.getId());
                        spuSaleAttrValue.setSaleAttrName(spuSaleAttr.getSaleAttrName());
                        spuSaleAttrValueMapper.insert(spuSaleAttrValue);
                    }
                }
            }
        }
        //保存海报
        List<SpuPoster> spuPosterList = spuInfo.getSpuPosterList();
        if (!CollectionUtils.isEmpty(spuPosterList)) {
            for (SpuPoster spuPoster : spuPosterList) {
                spuPoster.setSpuId(spuInfo.getId());
                spuPosterMapper.insert(spuPoster);
            }
        }
    }

    @Override
    public List<BaseSaleAttr> getBaseSaleAttrList() {
        return  baseSaleAttrMapper.selectList(null);
    }

    //    根据category3Id查询分类列表
    @Override
    public IPage<SpuInfo> getSpuInfoPage(Page<SpuInfo> spuInfoPage, SpuInfo spuInfo) {
        QueryWrapper<SpuInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("category3_id",spuInfo.getCategory3Id());
        queryWrapper.orderByAsc("id");
        Page<SpuInfo> pages = spuInfoMapper.selectPage(spuInfoPage, queryWrapper);
        return pages;
    }

    /**
     * 根据属性id获取属性值
     * @param attrId
     * @return
     */
    @Override
    public BaseAttrInfo getAttrInfo(Long attrId) {
        LambdaQueryWrapper<BaseAttrInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(BaseAttrInfo::getId,attrId);
        BaseAttrInfo baseAttrInfo = baseAttrInfoMapper.selectOne(queryWrapper);
//        BaseAttrInfo baseAttrInfo = baseAttrInfoMapper.selectById(attrId);
        List<BaseAttrValue> list= getAttrValue(attrId);
        baseAttrInfo.setAttrValueList(list);
        return baseAttrInfo;
    }

    private List<BaseAttrValue> getAttrValue(Long attrId) {
        QueryWrapper<BaseAttrValue> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("attr_id",attrId);
        List<BaseAttrValue> list = baseAttrValueMapper.selectList(queryWrapper);
        return list;
    }

    /**
     * 保存-更新平台属性方法
     * @param baseAttrInfo
     */
    @Override
    public void saveAttrInfo(BaseAttrInfo baseAttrInfo) {
        //什么情况下是新增什么情况下是修改根据是否有id进行判断
        if (baseAttrInfo.getId() != null) {
            //有id为更新
            baseAttrInfoMapper.updateById(baseAttrInfo);
        }else{
            //没有id则添加
            baseAttrInfoMapper.insert(baseAttrInfo);
        }
        QueryWrapper<BaseAttrValue> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("attr_id",baseAttrInfo.getId());
        //删除
        baseAttrValueMapper.delete(queryWrapper);
        //新增
        List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
        if (!CollectionUtils.isEmpty(attrValueList)&&attrValueList.size()>0) {
            attrValueList.stream().forEach(attrValue->{
                attrValue.setAttrId(baseAttrInfo.getId());
                baseAttrValueMapper.insert(attrValue);
            });
        }
    }

    @Override
    public List<BaseAttrInfo> getAttrInfoList(Long category1Id, Long category2Id, Long category3Id) {
        List<BaseAttrInfo> list=baseAttrInfoMapper.getAttrInfoList(category1Id,category2Id,category3Id);
        return list;
    }

    @Override
    public List<BaseCategory1> getCategory1() {
        LambdaQueryWrapper<BaseCategory1> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByAsc();
//        QueryWrapper<BaseCategory1> queryWrapper = new QueryWrapper<>();
        List<BaseCategory1> list = baseCategory1Mapper.selectList(queryWrapper);
        return list;
    }

    @Override
    public List<BaseCategory2> getCategory2(Long category1Id) {
        LambdaQueryWrapper<BaseCategory2> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(BaseCategory2::getCategory1Id,category1Id);
        queryWrapper.orderByAsc();
        List<BaseCategory2> list = baseCategory2Mapper.selectList(queryWrapper);
        return list;
    }

    @Override
    public List<BaseCategory3> getCategory3(Long category2Id) {
        LambdaQueryWrapper<BaseCategory3> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(BaseCategory3::getCategory2Id,category2Id);
        queryWrapper.orderByAsc();
        List<BaseCategory3> list = baseCategory3Mapper.selectList(queryWrapper);
        return list;
    }


}
