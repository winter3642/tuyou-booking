package com.tuyou.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private static final String DETAIL_KEY_PREFIX = "product:detail:";
    private static final String DETAIL_LOCK_PREFIX = "lock:product:detail:";
    /** 空值缓存标记：穿透拦截时缓存 60s */
    private static final String CACHE_NULL = "\u0000NULL\u0000";
    /** 防雪崩：基础过期 10 分钟 + 0~5 分钟随机抖动 */
    private static final long BASE_TTL_SECONDS = 600;
    private static final long TTL_JITTER_SECONDS = 300;

    private final ProductMapper productMapper;
    private final ProductSkuMapper skuMapper;
    private final StringRedisTemplate redisTemplate;
    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;

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

    /** 深分页阈值：offset 超过该值改用延迟关联（W3D3），避免为丢弃的行做大量回表 */
    private static final long DEEP_PAGE_OFFSET_THRESHOLD = 10_000;

    @Override
    public PageVO<ProductVO> search(String keyword, Long categoryId, Long destinationId,
                                    BigDecimal minPrice, BigDecimal maxPrice, String sortBy,
                                    int pageNum, int pageSize) {
        long offset = (long) (pageNum - 1) * pageSize;
        if (offset >= DEEP_PAGE_OFFSET_THRESHOLD) {
            // 深分页走延迟关联：第一步只查主键（覆盖索引，不回表），第二步按主键取整行
            return searchDeepPage(keyword, categoryId, destinationId, minPrice, maxPrice, sortBy, pageNum, pageSize);
        }
        Page<Product> page = new Page<>(pageNum, pageSize);
        IPage<Product> result = productMapper.selectPage(page, buildQuery(keyword, categoryId, destinationId,
                minPrice, maxPrice, sortBy));
        return toPageVO(result);
    }

    /** 构建搜索条件（普通分页与深分页共用，保证两种路径条件一致） */
    private LambdaQueryWrapper<Product> buildQuery(String keyword, Long categoryId, Long destinationId,
                                                   BigDecimal minPrice, BigDecimal maxPrice, String sortBy) {
        LambdaQueryWrapper<Product> qw = new LambdaQueryWrapper<>();
        // 用户端只展示上架产品
        qw.eq(Product::getStatus, 1);
        // 关键词：FULLTEXT 全文索引（ft_name, ngram 分词），LIKE '%kw%' 前导通配符无法走索引 —— W3D2 优化
        // BOOLEAN MODE：不算相关度分数，高命中词也不会劣化；清洗布尔操作符防语法错误/注入
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim().replaceAll("[+\\-<>()~*\"@]", " ");
            if (!kw.isBlank()) {
                qw.apply("MATCH(name) AGAINST({0} IN BOOLEAN MODE)", kw);
            }
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
        return qw;
    }

    /**
     * 深分页（W3D3 延迟关联）：
     * 优化前 SELECT * ... LIMIT 90000,20 —— 要跳过 9 万行，每跳一行都要回表取整行再丢弃
     * 优化后分两步 —— ① 只取主键（idx_status_sales 覆盖索引纯遍历，Extra=Using index）
     * ② 主键 IN 取回 20 行整行，再按第①步顺序重排（IN 不保证顺序）
     * EXPLAIN ANALYZE 同口径实测（10 万行、offset=90000）：91.5ms → 21.6ms（CPU 成本）
     * API 级耗时在本数据规模（内存回表）下两方案接近，差距随数据量/冷缓存放大，见 README 优化专题
     */
    private PageVO<ProductVO> searchDeepPage(String keyword, Long categoryId, Long destinationId,
                                             BigDecimal minPrice, BigDecimal maxPrice, String sortBy,
                                             int pageNum, int pageSize) {
        long offset = (long) (pageNum - 1) * pageSize;
        LambdaQueryWrapper<Product> qw = buildQuery(keyword, categoryId, destinationId, minPrice, maxPrice, sortBy);
        long total = productMapper.selectCount(qw);
        if (total == 0) {
            return PageVO.of(0L, List.of());
        }
        // ① 覆盖索引取主键：select 只保留 id，extra 出现 Using index，跳过 offset 行没有回表成本
        List<Product> idRows = productMapper.selectList(
                qw.select(Product::getId).last("LIMIT " + offset + ", " + pageSize));
        if (idRows.isEmpty()) {
            return PageVO.of(total, List.of());
        }
        List<Long> ids = idRows.stream().map(Product::getId).toList();
        // ② 主键批量取整行（每行最多一次回表，共 pageSize 次）
        List<Product> rows = productMapper.selectBatchIds(ids);
        Map<Long, Integer> order = new HashMap<>();
        for (int i = 0; i < ids.size(); i++) {
            order.put(ids.get(i), i);
        }
        rows.sort(Comparator.comparingInt(p -> order.getOrDefault(p.getId(), Integer.MAX_VALUE)));
        return toPageVO(rows, total);
    }

    /**
     * 产品详情：Cache-Aside 缓存 + 三防
     * - 防穿透：不存在的 id 缓存空值 60s
     * - 防击穿：Redisson 互斥锁重建，热点 key 过期瞬间只放一个线程查库
     * - 防雪崩：过期时间加随机抖动，避免整点集体失效
     */
    @Override
    public ProductDetailVO detail(Long id) {
        String key = DETAIL_KEY_PREFIX + id;
        // 1) 缓存命中直接返回
        String cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            if (CACHE_NULL.equals(cached)) {
                throw new BizException(ResultCode.NOT_FOUND);
            }
            return parse(cached);
        }
        // 2) 未命中：互斥锁重建（防击穿）
        RLock lock = redissonClient.getLock(DETAIL_LOCK_PREFIX + id);
        boolean locked = false;
        try {
            locked = lock.tryLock(2, TimeUnit.SECONDS);
            if (!locked) {
                // 没抢到锁：说明别的线程正在重建，等 50ms 后重读缓存
                Thread.sleep(50);
                String again = redisTemplate.opsForValue().get(key);
                if (again != null) {
                    if (CACHE_NULL.equals(again)) {
                        throw new BizException(ResultCode.NOT_FOUND);
                    }
                    return parse(again);
                }
                // 等不到回填（锁竞争激烈）：降级直接查库
                return queryAndCache(id, key);
            }
            // 3) 抢到锁后双重检查（可能别的线程已回填）
            String again = redisTemplate.opsForValue().get(key);
            if (again != null) {
                if (CACHE_NULL.equals(again)) {
                    throw new BizException(ResultCode.NOT_FOUND);
                }
                return parse(again);
            }
            return queryAndCache(id, key);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return queryAndCache(id, key); // 中断时降级直查
        } finally {
            if (locked) {
                lock.unlock();
            }
        }
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
        // 先改 DB 再删缓存（W2D3）：避免"删了缓存还没改完 DB"的窗口期别的请求把旧值写回缓存
        redisTemplate.delete(DETAIL_KEY_PREFIX + id);
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BizException(ResultCode.NOT_FOUND);
        }
        product.setStatus(status);
        productMapper.updateById(product);
        // 上下架同样失效详情缓存，防止列表/详情状态不一致
        redisTemplate.delete(DETAIL_KEY_PREFIX + id);
    }

    /** 查库 + 回填缓存 */
    private ProductDetailVO queryAndCache(Long id, String key) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            // 防穿透：空值也缓存（60s 短过期），拦截对不存在 id 的反复轰炸
            redisTemplate.opsForValue().set(key, CACHE_NULL, Duration.ofSeconds(60));
            throw new BizException(ResultCode.NOT_FOUND);
        }
        ProductDetailVO vo = buildDetailVO(product);
        // 防雪崩：过期时间 = 基础 10 分钟 + 随机 0~5 分钟
        long ttl = BASE_TTL_SECONDS + ThreadLocalRandom.current().nextLong(TTL_JITTER_SECONDS + 1);
        redisTemplate.opsForValue().set(key, toJson(vo), Duration.ofSeconds(ttl));
        return vo;
    }

    private ProductDetailVO buildDetailVO(Product product) {
        ProductDetailVO vo = new ProductDetailVO();
        BeanUtils.copyProperties(product, vo);
        List<ProductSku> skus = skuMapper.selectList(
                new LambdaQueryWrapper<ProductSku>()
                        .eq(ProductSku::getProductId, product.getId())
                        .orderByAsc(ProductSku::getDepartDate));
        List<SkuVO> skuVOs = skus.stream().map(s -> {
            SkuVO svo = new SkuVO();
            BeanUtils.copyProperties(s, svo);
            return svo;
        }).collect(Collectors.toList());
        vo.setSkuList(skuVOs);
        return vo;
    }

    private String toJson(ProductDetailVO vo) {
        try {
            return objectMapper.writeValueAsString(vo);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("产品详情序列化失败", e);
        }
    }

    private ProductDetailVO parse(String json) {
        try {
            return objectMapper.readValue(json, ProductDetailVO.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("产品详情反序列化失败", e);
        }
    }

    private PageVO<ProductVO> toPageVO(IPage<Product> result) {
        List<ProductVO> records = result.getRecords().stream().map(p -> {
            ProductVO vo = new ProductVO();
            BeanUtils.copyProperties(p, vo);
            return vo;
        }).collect(Collectors.toList());
        return PageVO.of(result.getTotal(), records);
    }

    /** 深分页路径没有 IPage，用显式 total 构建分页结果 */
    private PageVO<ProductVO> toPageVO(List<Product> rows, long total) {
        List<ProductVO> records = rows.stream().map(p -> {
            ProductVO vo = new ProductVO();
            BeanUtils.copyProperties(p, vo);
            return vo;
        }).collect(Collectors.toList());
        return PageVO.of(total, records);
    }
}
