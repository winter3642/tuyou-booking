package com.tuyou.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 目的地表
 */
@Data
@TableName("t_destination")
public class Destination {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String pinyin;
    private Integer hotFlag;
}
