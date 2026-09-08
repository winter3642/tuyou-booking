package com.tuyou.service;

import com.tuyou.dto.ProductDTO;
import com.tuyou.vo.PageVO;
import com.tuyou.vo.ProductDetailVO;
import com.tuyou.vo.ProductVO;

public interface ProductService {

    PageVO<ProductVO> page(int pageNum, int pageSize, Integer status);

    ProductDetailVO detail(Long id);

    Long create(ProductDTO dto);

    void update(Long id, ProductDTO dto);

    void updateStatus(Long id, Integer status);
}
