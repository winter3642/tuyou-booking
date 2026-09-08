package com.tuyou.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 产品新增/修改入参
 */
@Data
public class ProductDTO {

    @NotNull(message = "分类不能为空")
    private Long categoryId;

    @NotNull(message = "目的地不能为空")
    private Long destinationId;

    @NotBlank(message = "产品名称不能为空")
    @Size(max = 200, message = "产品名称过长")
    private String name;

    @Size(max = 500, message = "副标题过长")
    private String subtitle;

    @NotNull(message = "价格不能为空")
    @DecimalMin(value = "0.01", message = "价格必须大于 0")
    private BigDecimal price;

    private Integer sales;

    private BigDecimal score;

    private Integer status;
}
