//
//
package com.atguigu.gmall.model.product;

import com.atguigu.gmall.model.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * <p>
 * BaseCategoryView
 * </p>
 *
 */
@Data
public class CategoryTrademarkVo {
	
	@ApiModelProperty(value = "三级分类编号")
	private Long category3Id;

	@ApiModelProperty(value = "品牌id")
	private List<Long> trademarkIdList;


}

