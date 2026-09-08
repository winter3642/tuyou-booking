package com.tuyou.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付流水：与订单分离，一单可多次支付尝试
 * status: 0待支付 1成功 2失败
 */
@Data
@TableName("t_payment")
public class Payment {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private String payNo;
    private String channel;
    private Integer status;
    private BigDecimal amount;
    private LocalDateTime payTime;
    private LocalDateTime createTime;
}
