package com.atguigu.gmall.list.repository;

import com.atguigu.gmall.model.list.Goods;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Component;

/**
 * @author 24657
 * @apiNote
 * @date 2023/8/7 16:18
 */

public interface GoodsRepository extends ElasticsearchRepository<Goods,Long> {

}
