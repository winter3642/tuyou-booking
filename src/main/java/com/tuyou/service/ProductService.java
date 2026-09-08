package com.tuyou.service;

import com.tuyou.dto.ProductDTO;
import com.tuyou.vo.PageVO;
import com.tuyou.vo.ProductDetailVO;
import com.tuyou.vo.ProductVO;

import java.math.BigDecimal;

public interface ProductService {

    PageVO<ProductVO> page(int pageNum, int pageSize, Integer status);

    /**
     * 产品搜索（用户端，只查上架产品）
     *
     * @param keyword       关键词（产品名 LIKE，故意不加索引，W3 慢 SQL 优化靶子）
     * @param categoryId    分类
     * @param destinationId 目的地
     * @param minPrice      最低价
     * @param maxPrice      最高价
     * @param sortBy        排序：price_asc / price_desc / score_desc / 默认 sales_desc
     */
    PageVO<ProductVO> search(String keyword, Long categoryId, Long destinationId,
                             BigDecimal minPrice, BigDecimal maxPrice, String sortBy,
                             int pageNum, int pageSize);

    ProductDetailVO detail(Long id);

    Long create(ProductDTO dto);

    void update(Long id, ProductDTO dto);

    void updateStatus(Long id, Integer status);
}
