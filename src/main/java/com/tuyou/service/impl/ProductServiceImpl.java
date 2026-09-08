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
        List<ProductVO> records = result.getRecords().stream().map(p -> {
            ProductVO vo = new ProductVO();
            BeanUtils.copyProperties(p, vo);
            return vo;
        }).collect(Collectors.toList());
        return PageVO.of(result.getTotal(), records);
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
}
