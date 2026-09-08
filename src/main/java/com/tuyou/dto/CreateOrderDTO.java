package com.tuyou.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 下单入参：待结算的购物车条目 id 列表
 */
@Data
public class CreateOrderDTO {

    @NotEmpty(message = "请选择要结算的商品")
    private List<Long> cartIds;
}
