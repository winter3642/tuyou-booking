package com.tuyou.vo;

import lombok.Data;

import java.util.List;

/**
 * 分类节点（树形）
 */
@Data
public class CategoryVO {
    private Long id;
    private String name;
    private Integer level;
    private Long parentId;
    private Integer sort;
    private List<CategoryVO> children;
}
