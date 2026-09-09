package com.tuyou.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tuyou.common.BizException;
import com.tuyou.dto.ProductDTO;
import com.tuyou.entity.Product;
import com.tuyou.entity.ProductSku;
import com.tuyou.mapper.ProductMapper;
import com.tuyou.mapper.ProductSkuMapper;
import com.tuyou.vo.PageVO;
import com.tuyou.vo.ProductDetailVO;
import com.tuyou.vo.ProductVO;
import com.tuyou.vo.SkuVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 产品服务单元测试（W2D4：含缓存三防用例）
 */
@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    private static final String CACHE_NULL = "\u0000NULL\u0000";

    @Mock
    private ProductMapper productMapper;

    @Mock
    private ProductSkuMapper skuMapper;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOps;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RLock lock;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @InjectMocks
    private ProductServiceImpl productService;

    private void mockCacheMiss() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(any(String.class))).thenReturn(null);
        when(redissonClient.getLock(any(String.class))).thenReturn(lock);
        try {
            when(lock.tryLock(2, TimeUnit.SECONDS)).thenReturn(true);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("分页：返回 total 与 VO 列表")
    void pageOk() {
        Product p = new Product();
        p.setId(1L);
        p.setName("测试产品");
        p.setPrice(new BigDecimal("100.00"));
        p.setStatus(1);

        Page<Product> page = new Page<>(1, 10);
        page.setRecords(Collections.singletonList(p));
        page.setTotal(999L);
        when(productMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        PageVO<ProductVO> result = productService.page(1, 10, 1);
        assertEquals(999L, result.getTotal());
        assertEquals(1, result.getRecords().size());
        assertEquals("测试产品", result.getRecords().get(0).getName());
    }

    @Test
    @DisplayName("详情：缓存未命中 + 产品不存在 → 空值缓存 + 404")
    void detailNotFound() {
        mockCacheMiss();
        when(productMapper.selectById(99L)).thenReturn(null);

        BizException ex = assertThrows(BizException.class, () -> productService.detail(99L));
        assertEquals(404, ex.getCode());
        verify(valueOps).set("product:detail:99", CACHE_NULL, Duration.ofSeconds(60));
    }

    @Test
    @DisplayName("详情：缓存未命中 → 查库 + 回填缓存")
    void detailOk() {
        mockCacheMiss();
        Product p = new Product();
        p.setId(1L);
        p.setName("桂林6日游");
        p.setStatus(1);
        when(productMapper.selectById(1L)).thenReturn(p);

        ProductSku s1 = new ProductSku();
        s1.setId(1L);
        s1.setProductId(1L);
        s1.setDepartDate(LocalDate.of(2026, 10, 1));
        s1.setStock(50);
        s1.setPrice(new BigDecimal("2999.00"));
        when(skuMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.singletonList(s1));

        ProductDetailVO vo = productService.detail(1L);
        assertEquals("桂林6日游", vo.getName());
        assertEquals(1, vo.getSkuList().size());
        assertEquals(LocalDate.of(2026, 10, 1), vo.getSkuList().get(0).getDepartDate());
        // 回填缓存（过期时间带随机抖动，验证 set 被调用）
        verify(valueOps).set(any(String.class), any(String.class), any(Duration.class));
    }

    @Test
    @DisplayName("详情：缓存命中直接返回，不查库")
    void detailCacheHit() throws Exception {
        ProductDetailVO cached = new ProductDetailVO();
        cached.setId(1L);
        cached.setName("桂林6日游");
        cached.setStatus(1);
        SkuVO svo = new SkuVO();
        svo.setId(1L);
        svo.setDepartDate(LocalDate.of(2026, 10, 1));
        svo.setStock(50);
        svo.setPrice(new BigDecimal("2999.00"));
        cached.setSkuList(Collections.singletonList(svo));

        // 先序列化成字符串再 stub：避免序列化异常被 Mockito 伪装成 UnfinishedStubbing
        String cachedJson;
        try {
            cachedJson = objectMapper.writeValueAsString(cached);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("测试缓存数据序列化失败", e);
        }

        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("product:detail:1")).thenReturn(cachedJson);

        ProductDetailVO vo = productService.detail(1L);
        assertEquals("桂林6日游", vo.getName());
        assertEquals(1, vo.getSkuList().size());
        verify(productMapper, never()).selectById(any(Long.class));
        verify(skuMapper, never()).selectList(any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("详情：缓存为空值标记 → 直接 404（防穿透拦截）")
    void detailCacheNullMarker() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("product:detail:99")).thenReturn(CACHE_NULL);

        BizException ex = assertThrows(BizException.class, () -> productService.detail(99L));
        assertEquals(404, ex.getCode());
        verify(productMapper, never()).selectById(any(Long.class));
    }

    @Test
    @DisplayName("新增：返回 MP 回填的自增 id")
    void createOk() {
        ProductDTO dto = new ProductDTO();
        dto.setCategoryId(1L);
        dto.setDestinationId(1L);
        dto.setName("测试新品");
        dto.setPrice(new BigDecimal("888.00"));
        dto.setStatus(1);

        doAnswer(inv -> {
            Product product = inv.getArgument(0);
            product.setId(100001L);
            return 1;
        }).when(productMapper).insert(any(Product.class));

        Long id = productService.create(dto);
        assertEquals(100001L, id);
    }

    @Test
    @DisplayName("修改产品：不存在抛 404；存在则更新非空字段")
    void update() {
        when(productMapper.selectById(1L)).thenReturn(null);
        ProductDTO dto = new ProductDTO();
        assertThrows(BizException.class, () -> productService.update(1L, dto));

        Product p = new Product();
        p.setId(2L);
        p.setName("旧名");
        p.setPrice(new BigDecimal("100.00"));
        when(productMapper.selectById(2L)).thenReturn(p);

        ProductDTO dto2 = new ProductDTO();
        dto2.setName("新名");
        dto2.setPrice(new BigDecimal("200.00"));
        productService.update(2L, dto2);

        assertEquals("新名", p.getName());
        assertEquals(new BigDecimal("200.00"), p.getPrice());
        verify(productMapper).updateById(p);
        // 修改后必须失效详情缓存（Cache-Aside，先改 DB 再删缓存）
        verify(redisTemplate).delete("product:detail:2");
    }

    @Test
    @DisplayName("上下架：产品不存在抛 404；存在则更新状态")
    void updateStatus() {
        when(productMapper.selectById(1L)).thenReturn(null);
        assertThrows(BizException.class, () -> productService.updateStatus(1L, 0));

        Product p = new Product();
        p.setId(2L);
        p.setStatus(1);
        when(productMapper.selectById(2L)).thenReturn(p);
        productService.updateStatus(2L, 0);
        verify(productMapper).updateById(p);
        assertEquals(0, p.getStatus());
    }

    // ---------- W3D4 补全：缓存击穿互斥锁分支 ----------

    private String toJson(ProductDetailVO vo) {
        try {
            return objectMapper.writeValueAsString(vo);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("测试缓存数据序列化失败", e);
        }
    }

    private ProductDetailVO detailVO() {
        ProductDetailVO vo = new ProductDetailVO();
        vo.setId(1L);
        vo.setName("桂林6日游");
        vo.setStatus(1);
        vo.setSkuList(Collections.emptyList());
        return vo;
    }

    /**
     * 锁流程专用 stub：第一次读缓存返回 firstGet，等待/双重检查后的第二次读返回 secondGet。
     * 不调用 mockCacheMiss（它的 get(any)/tryLock 会被这里的精确 stub 遮蔽，触发 UnnecessaryStubbing）
     */
    private void stubLockFlow(boolean acquired, String firstGet, String secondGet) throws Exception {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("product:detail:1")).thenReturn(firstGet, secondGet);
        when(redissonClient.getLock(any(String.class))).thenReturn(lock);
        when(lock.tryLock(2, TimeUnit.SECONDS)).thenReturn(acquired);
    }

    @Test
    @DisplayName("详情：抢锁失败 → 等待后读到别人回填的缓存，不查库")
    void detailLockFailedThenCached() throws Exception {
        stubLockFlow(false, null, toJson(detailVO()));

        ProductDetailVO vo = productService.detail(1L);

        assertEquals("桂林6日游", vo.getName());
        verify(productMapper, never()).selectById(any(Long.class));
    }

    @Test
    @DisplayName("详情：抢锁失败 → 缓存仍无值 → 降级直接查库回填")
    void detailLockFailedThenQuery() throws Exception {
        stubLockFlow(false, null, null);
        Product p = new Product();
        p.setId(1L);
        p.setName("桂林6日游");
        p.setStatus(1);
        when(productMapper.selectById(1L)).thenReturn(p);
        when(skuMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        ProductDetailVO vo = productService.detail(1L);

        assertEquals("桂林6日游", vo.getName());
        verify(valueOps).set(any(String.class), any(String.class), any(Duration.class));
    }

    @Test
    @DisplayName("详情：抢到锁后双重检查发现缓存已回填，不重复查库")
    void detailLockAcquiredButCached() throws Exception {
        stubLockFlow(true, null, toJson(detailVO()));

        ProductDetailVO vo = productService.detail(1L);

        assertEquals("桂林6日游", vo.getName());
        verify(productMapper, never()).selectById(any(Long.class));
    }

    @Test
    @DisplayName("详情：抢锁被中断 → 恢复中断标记并降级查库")
    void detailLockInterrupted() throws Exception {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("product:detail:1")).thenReturn(null);
        when(redissonClient.getLock(any(String.class))).thenReturn(lock);
        when(lock.tryLock(2, TimeUnit.SECONDS)).thenThrow(new InterruptedException());
        Product p = new Product();
        p.setId(1L);
        p.setName("桂林6日游");
        p.setStatus(1);
        when(productMapper.selectById(1L)).thenReturn(p);
        when(skuMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        ProductDetailVO vo = productService.detail(1L);

        assertEquals("桂林6日游", vo.getName());
        // 规范做法：捕获 InterruptedException 后恢复中断标记（面试点）
        assertTrue(Thread.interrupted(), "中断标记应被恢复");
    }
}
