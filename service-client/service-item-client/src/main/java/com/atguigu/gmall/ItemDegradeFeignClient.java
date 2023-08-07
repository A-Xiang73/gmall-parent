package com.atguigu.gmall;

import com.atguigu.gmall.common.result.Result;
import org.springframework.stereotype.Component;

/**
 * @author 24657
 * @apiNote
 * @date 2023/8/1 16:55
 */
@Component
public class ItemDegradeFeignClient implements ItemFeignClient{
    @Override
    public Result getItem(Long skuId) {
        return Result.fail();
    }
}
