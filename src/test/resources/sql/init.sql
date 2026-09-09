-- ============================================================
-- W3D4 测试库初始化脚本（@Sql BEFORE_TEST_METHOD 执行）
-- 幂等设计：DDL(IF NOT EXISTS) + DELETE 清表 + INSERT 固定 id 测试数据
-- 注意：id 全部用 90xxxx 号段，避免与主库 tuyou 数据/Redis 缓存撞车
-- ============================================================

-- ---------- DDL（与主库 init.sql 一致，t_product 额外带 ft_name 全文索引供搜索测试） ----------
CREATE TABLE IF NOT EXISTS t_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(50) NOT NULL UNIQUE,
  password VARCHAR(100) NOT NULL,
  phone VARCHAR(20),
  email VARCHAR(100),
  avatar VARCHAR(255),
  status TINYINT NOT NULL DEFAULT 1,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_phone (phone)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS t_category (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(50) NOT NULL,
  level TINYINT NOT NULL DEFAULT 1,
  parent_id BIGINT NOT NULL DEFAULT 0,
  sort INT NOT NULL DEFAULT 0,
  KEY idx_parent (parent_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS t_destination (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(50) NOT NULL,
  pinyin VARCHAR(100),
  hot_flag TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_name (name)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS t_product (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  category_id BIGINT NOT NULL,
  destination_id BIGINT NOT NULL,
  name VARCHAR(200) NOT NULL,
  subtitle VARCHAR(500),
  price DECIMAL(10,2) NOT NULL,
  sales INT NOT NULL DEFAULT 0,
  score DECIMAL(2,1) DEFAULT 5.0,
  status TINYINT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_cat_dest (category_id, destination_id),
  KEY idx_status_sales (status, sales),
  -- W3D2 与主库同步的优化索引（测试库保持 schema 一致）
  KEY idx_cat_dest_price (category_id, destination_id, price),
  KEY idx_cat_status_sales (category_id, status, sales),
  FULLTEXT KEY ft_name (name) WITH PARSER ngram
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS t_product_sku (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  product_id BIGINT NOT NULL,
  depart_date DATE NOT NULL,
  stock INT NOT NULL,
  price DECIMAL(10,2) NOT NULL,
  KEY idx_product_date (product_id, depart_date)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS t_cart (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  sku_id BIGINT NOT NULL,
  quantity INT NOT NULL DEFAULT 1,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_sku (user_id, sku_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS t_order (
  id BIGINT PRIMARY KEY,
  order_no VARCHAR(32) NOT NULL UNIQUE,
  user_id BIGINT NOT NULL,
  total_amount DECIMAL(10,2) NOT NULL,
  status TINYINT NOT NULL DEFAULT 0,
  pay_time DATETIME,
  cancel_time DATETIME,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS t_order_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  sku_id BIGINT NOT NULL,
  product_name VARCHAR(200),
  price DECIMAL(10,2),
  quantity INT NOT NULL,
  KEY idx_order (order_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS t_payment (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  pay_no VARCHAR(64) NOT NULL,
  channel VARCHAR(20),
  status TINYINT NOT NULL DEFAULT 0,
  amount DECIMAL(10,2),
  pay_time DATETIME,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- ---------- 清表（保证幂等，先子表后主表） ----------
DELETE FROM t_order_item;
DELETE FROM t_payment;
DELETE FROM t_order;
DELETE FROM t_cart;
DELETE FROM t_product_sku;
DELETE FROM t_product;
DELETE FROM t_category;
DELETE FROM t_destination;
DELETE FROM t_user;

-- 重置自增：测试断言依赖 cart/user 从 id=1 开始（如 PUT /api/cart/1）
ALTER TABLE t_cart AUTO_INCREMENT = 1;
ALTER TABLE t_user AUTO_INCREMENT = 1;

-- ---------- 测试数据 ----------
INSERT INTO t_category (id, name, level, parent_id, sort) VALUES
  (9001, '跟团游', 1, 0, 1),
  (9002, '国内', 2, 9001, 1);

INSERT INTO t_destination (id, name, pinyin, hot_flag) VALUES
  (9001, '桂林', 'guilin', 1),
  (9002, '北京', 'beijing', 0);

-- 3 条产品：2 上架 + 1 下架（供搜索/列表/上下架断言）
-- create_time 显式错开：列表按 create_time DESC，保证 records[0] 固定
INSERT INTO t_product (id, category_id, destination_id, name, subtitle, price, sales, score, status, create_time) VALUES
  (900001, 9001, 9001, '桂林6日游测试产品', '桂林测试副标题', 2999.00, 100, 4.5, 1, '2026-09-01 10:00:02'),
  (900002, 9001, 9002, '北京5日游测试产品', '北京测试副标题', 1999.00, 50, 4.0, 1, '2026-09-01 10:00:01'),
  (900003, 9002, 9001, '下架测试产品', '已下架', 99.00, 0, 3.0, 0, '2026-09-01 10:00:00');

-- 2 条 SKU（下单/购物车用，库存充足）
INSERT INTO t_product_sku (id, product_id, depart_date, stock, price) VALUES
  (800001, 900001, '2026-10-01', 100, 2999.00),
  (800002, 900002, '2026-10-02', 50, 1999.00);
