package com.tuyou.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 加购入参
 */
@Data
public class CartDTO {

    @NotNull(message = "sku不能为空")
    private Long skuId;

    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量至少为1")
    @Max(value = 99, message = "数量不能超过99")
    private Integer quantity;
}
