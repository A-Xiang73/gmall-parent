package com.atguigu.gmall.list.client;

import com.atguigu.gmall.common.result.Result;
import org.springframework.stereotype.Component;

/**
 * @author 24657
 * @apiNote
 * @date 2023/8/7 18:14
 */
@Component
public class ListDegradeFeignClient implements ListFeignClient{

    @Override
    public Result incrHotScore(Long skuId) {
        return null;
    }
}
