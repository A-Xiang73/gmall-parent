package com.atguigu.gmall.model.comment;

import com.atguigu.gmall.model.base.BaseMongoEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * CommentInfo
 */
@Data
@ApiModel(description = "好评等级")
public class CommentInfoAppraiseVo {
	

	@ApiModelProperty(value = "好评等级")
	private Integer appraise;

	@ApiModelProperty(value = "个数")
	private Integer count;

}

