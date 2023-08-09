package com.atguigu.gmall.list.client;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.list.SearchParam;
import org.springframework.stereotype.Component;

/**
 * @author 24657
 * @apiNote
 * @date 2023/8/7 18:14
 */
@Component
public class ListDegradeFeignClient implements ListFeignClient{

    @Override
    public Result lowerGoods(Long skuId) {
        return null;
    }

    @Override
    public Result upperGoods(Long skuId) {
        return null;
    }

    @Override
    public Result list(SearchParam listParam) {
        return null;
    }

    @Override
    public Result incrHotScore(Long skuId) {
        return null;
    }
}
