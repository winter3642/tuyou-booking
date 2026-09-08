package com.tuyou.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tuyou.common.BizException;
import com.tuyou.common.ResultCode;
import com.tuyou.dto.ProductDTO;
import com.tuyou.entity.Product;
import com.tuyou.entity.ProductSku;
import com.tuyou.mapper.ProductMapper;
import com.tuyou.mapper.ProductSkuMapper;
import com.tuyou.service.ProductService;
import com.tuyou.vo.PageVO;
import com.tuyou.vo.ProductDetailVO;
import com.tuyou.vo.ProductVO;
import com.tuyou.vo.SkuVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductMapper productMapper;
    private final ProductSkuMapper skuMapper;

    @Override
    public PageVO<ProductVO> page(int pageNum, int pageSize, Integer status) {
        Page<Product> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Product> qw = new LambdaQueryWrapper<>();
        if (status != null) {
            qw.eq(Product::getStatus, status);
        }
        qw.orderByDesc(Product::getCreateTime);
        IPage<Product> result = productMapper.selectPage(page, qw);
        return toPageVO(result);
    }

    @Override
    public PageVO<ProductVO> search(String keyword, Long categoryId, Long destinationId,
                                    BigDecimal minPrice, BigDecimal maxPrice, String sortBy,
                                    int pageNum, int pageSize) {
        Page<Product> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Product> qw = new LambdaQueryWrapper<>();
        // 用户端只展示上架产品
        qw.eq(Product::getStatus, 1);
        // 关键词：LIKE '%kw%' 无法走索引，10 万行全表扫描 —— W3 慢 SQL 优化靶子
        if (keyword != null && !keyword.isBlank()) {
            qw.like(Product::getName, keyword.trim());
        }
        if (categoryId != null) {
            qw.eq(Product::getCategoryId, categoryId);
        }
        if (destinationId != null) {
            qw.eq(Product::getDestinationId, destinationId);
        }
        if (minPrice != null) {
            qw.ge(Product::getPrice, minPrice);
        }
        if (maxPrice != null) {
            qw.le(Product::getPrice, maxPrice);
        }
        // 排序白名单：只允许固定枚举，从根上杜绝"排序字段注入"
        switch (sortBy == null ? "" : sortBy) {
            case "price_asc" -> qw.orderByAsc(Product::getPrice);
            case "price_desc" -> qw.orderByDesc(Product::getPrice);
            case "score_desc" -> qw.orderByDesc(Product::getScore);
            default -> qw.orderByDesc(Product::getSales); // 默认按销量
        }
        IPage<Product> result = productMapper.selectPage(page, qw);
        return toPageVO(result);
    }

    @Override
    public ProductDetailVO detail(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BizException(ResultCode.NOT_FOUND);
        }
        ProductDetailVO vo = new ProductDetailVO();
        BeanUtils.copyProperties(product, vo);

        List<ProductSku> skus = skuMapper.selectList(
                new LambdaQueryWrapper<ProductSku>()
                        .eq(ProductSku::getProductId, id)
                        .orderByAsc(ProductSku::getDepartDate));
        List<SkuVO> skuVOs = skus.stream().map(s -> {
            SkuVO svo = new SkuVO();
            BeanUtils.copyProperties(s, svo);
            return svo;
        }).collect(Collectors.toList());
        vo.setSkuList(skuVOs);
        return vo;
    }

    @Override
    public Long create(ProductDTO dto) {
        Product product = new Product();
        BeanUtils.copyProperties(dto, product);
        productMapper.insert(product);
        return product.getId();
    }

    @Override
    public void update(Long id, ProductDTO dto) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BizException(ResultCode.NOT_FOUND);
        }
        // MP updateById 默认忽略 null 字段，所以 dto 里没传的字段不会被覆盖
        BeanUtils.copyProperties(dto, product);
        productMapper.updateById(product);
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BizException(ResultCode.NOT_FOUND);
        }
        product.setStatus(status);
        productMapper.updateById(product);
    }

    private PageVO<ProductVO> toPageVO(IPage<Product> result) {
        List<ProductVO> records = result.getRecords().stream().map(p -> {
            ProductVO vo = new ProductVO();
            BeanUtils.copyProperties(p, vo);
            return vo;
        }).collect(Collectors.toList());
        return PageVO.of(result.getTotal(), records);
    }
}
