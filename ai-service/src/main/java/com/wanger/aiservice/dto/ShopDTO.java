package com.wanger.aiservice.dto;

import lombok.Data;

/**
 * 降级报告所需的店铺基础字段，从 shop-service /shop/{id} 的 Result.data 转换而来。
 */
@Data
public class ShopDTO {

    private String name;

    /** 评分，1~5 分乘 10 保存 */
    private Integer score;

    /** 销量 */
    private Integer sold;

    /** 评论数 */
    private Integer comments;

    /** 均价 */
    private Long avgPrice;

    /** 商圈 */
    private String area;
}
