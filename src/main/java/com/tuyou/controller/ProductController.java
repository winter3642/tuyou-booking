package com.tuyou.controller;

import com.tuyou.common.BizException;
import com.tuyou.common.Result;
import com.tuyou.common.ResultCode;
import com.tuyou.dto.ProductDTO;
import com.tuyou.interceptor.LoginUserHolder;
import com.tuyou.service.ProductService;
import com.tuyou.vo.PageVO;
import com.tuyou.vo.ProductDetailVO;
import com.tuyou.vo.ProductVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

/**
 * 产品接口：公开查询 + 管理端 CRUD
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /** 产品分页列表（管理端视角，含下架；不传 status 查全部） */
    @GetMapping("/products")
    public Result<PageVO<ProductVO>> page(@RequestParam(defaultValue = "1") int page,
                                          @RequestParam(defaultValue = "10") int size,
                                          @RequestParam(required = false) Integer status) {
        return Result.ok(productService.page(page, size, status));
    }

    /** 产品搜索（用户端，只返回上架产品） */
    @GetMapping("/products/search")
    public Result<PageVO<ProductVO>> search(@RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) Long categoryId,
                                            @RequestParam(required = false) Long destinationId,
                                            @RequestParam(required = false) BigDecimal minPrice,
                                            @RequestParam(required = false) BigDecimal maxPrice,
                                            @RequestParam(required = false) String sortBy,
                                            @RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "10") int size) {
        return Result.ok(productService.search(keyword, categoryId, destinationId,
                minPrice, maxPrice, sortBy, page, size));
    }

    /** 产品详情（含 SKU 日期价格列表） */
    @GetMapping("/products/{id}")
    public Result<ProductDetailVO> detail(@PathVariable Long id) {
        return Result.ok(productService.detail(id));
    }

    /** 新增产品（管理员） */
    @PostMapping("/admin/products")
    public Result<Long> create(@Valid @RequestBody ProductDTO dto) {
        checkAdmin();
        return Result.ok(productService.create(dto));
    }

    /** 修改产品（管理员） */
    @PutMapping("/admin/products/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody ProductDTO dto) {
        checkAdmin();
        productService.update(id, dto);
        return Result.ok();
    }

    /** 上架/下架（管理员） */
    @PutMapping("/admin/products/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        checkAdmin();
        productService.updateStatus(id, status);
        return Result.ok();
    }

    /**
     * 简化版角色校验：固定 admin 用户名。
     * 面试话术：真实项目应使用 RBAC 权限表（角色-权限关联），这里为演示用简化判断
     */
    private void checkAdmin() {
        String username = LoginUserHolder.getUsername();
        if (!"admin".equals(username)) {
            throw new BizException(ResultCode.FORBIDDEN);
        }
    }
}
