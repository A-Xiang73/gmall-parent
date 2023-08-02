package com.atguigu.gmall.item.service;

import java.util.Map;
import java.util.Objects;

public interface ItemService {
    Map<String, Objects> getBySkuId(Long skuId);
}
