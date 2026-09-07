-- ============================================================
-- 途游预订 · 数据库初始化脚本（10 张表）
-- 字符集 utf8mb4 / 引擎 InnoDB / 无物理外键（逻辑关联，生产惯例）
-- ============================================================

-- 1. 用户表
CREATE TABLE IF NOT EXISTS t_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(50) NOT NULL UNIQUE,
  password VARCHAR(100) NOT NULL COMMENT 'BCrypt哈希',
  phone VARCHAR(20),
  email VARCHAR(100),
  avatar VARCHAR(255),
  status TINYINT NOT NULL DEFAULT 1 COMMENT '1正常 0禁用',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_phone (phone)
) ENGINE=InnoDB COMMENT='用户表';

-- 2. 分类表（三级树形：跟团游 → 国内 → 华东，parent_id 支撑树）
CREATE TABLE IF NOT EXISTS t_category (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(50) NOT NULL,
  level TINYINT NOT NULL DEFAULT 1 COMMENT '1一级 2二级 3三级',
  parent_id BIGINT NOT NULL DEFAULT 0 COMMENT '0=顶级',
  sort INT NOT NULL DEFAULT 0 COMMENT '排序权重',
  KEY idx_parent (parent_id)
) ENGINE=InnoDB COMMENT='产品分类表';

-- 3. 目的地表（pinyin 供搜索联想）
CREATE TABLE IF NOT EXISTS t_destination (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(50) NOT NULL,
  pinyin VARCHAR(100) COMMENT '拼音，如 beijing',
  hot_flag TINYINT NOT NULL DEFAULT 0 COMMENT '1热门',
  UNIQUE KEY uk_name (name)
) ENGINE=InnoDB COMMENT='目的地表';

-- 4. 产品主表
CREATE TABLE IF NOT EXISTS t_product (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  category_id BIGINT NOT NULL COMMENT '1跟团游 2门票 3酒店',
  destination_id BIGINT NOT NULL,
  name VARCHAR(200) NOT NULL,
  subtitle VARCHAR(500),
  price DECIMAL(10,2) NOT NULL COMMENT '起价',
  sales INT NOT NULL DEFAULT 0 COMMENT '销量，排序用',
  score DECIMAL(2,1) DEFAULT 5.0,
  status TINYINT NOT NULL DEFAULT 0 COMMENT '0下架 1上架',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_cat_dest (category_id, destination_id),
  KEY idx_status_sales (status, sales)
) ENGINE=InnoDB COMMENT='产品主表';

-- 5. SKU 库存表（按日期粒度，节假日可调价）
CREATE TABLE IF NOT EXISTS t_product_sku (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  product_id BIGINT NOT NULL,
  depart_date DATE NOT NULL COMMENT '出行日期（酒店为入住日）',
  stock INT NOT NULL COMMENT '剩余库存',
  price DECIMAL(10,2) NOT NULL COMMENT '该日期价格',
  KEY idx_product_date (product_id, depart_date)
) ENGINE=InnoDB COMMENT='SKU库存表';

-- 6. 购物车表（同一用户同一 SKU 只允许一行，uk 防重复加购）
CREATE TABLE IF NOT EXISTS t_cart (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  sku_id BIGINT NOT NULL,
  quantity INT NOT NULL DEFAULT 1,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_sku (user_id, sku_id)
) ENGINE=InnoDB COMMENT='购物车表';

-- 7. 订单表（id 为雪花ID非自增，为分库分表铺路）
CREATE TABLE IF NOT EXISTS t_order (
  id BIGINT PRIMARY KEY COMMENT '雪花ID',
  order_no VARCHAR(32) NOT NULL UNIQUE COMMENT '业务订单号',
  user_id BIGINT NOT NULL,
  total_amount DECIMAL(10,2) NOT NULL,
  status TINYINT NOT NULL DEFAULT 0 COMMENT '0待支付 1已支付 2已取消 3已完成',
  pay_time DATETIME,
  cancel_time DATETIME,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_user_time (user_id, create_time)
) ENGINE=InnoDB COMMENT='订单表';

-- 8. 订单明细表（product_name/price 冗余快照，防商品改名改价后历史订单失真）
CREATE TABLE IF NOT EXISTS t_order_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  sku_id BIGINT NOT NULL,
  product_name VARCHAR(200) NOT NULL COMMENT '冗余快照',
  price DECIMAL(10,2) NOT NULL COMMENT '下单时价格快照',
  quantity INT NOT NULL,
  KEY idx_order (order_id)
) ENGINE=InnoDB COMMENT='订单明细表';

-- 9. 支付流水表（支付与订单分离，一单可多次支付尝试）
CREATE TABLE IF NOT EXISTS t_payment (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  pay_no VARCHAR(32) NOT NULL UNIQUE COMMENT '支付流水号',
  channel VARCHAR(20) NOT NULL DEFAULT 'mock' COMMENT '支付渠道',
  status TINYINT NOT NULL DEFAULT 0 COMMENT '0待支付 1成功 2失败',
  amount DECIMAL(10,2) NOT NULL COMMENT '支付金额',
  pay_time DATETIME,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_order (order_id)
) ENGINE=InnoDB COMMENT='支付流水表';

-- 10. 评价表（uk 防同一订单对同一产品重复评价）
CREATE TABLE IF NOT EXISTS t_review (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  score TINYINT NOT NULL COMMENT '1-5分',
  content VARCHAR(1000),
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_product (product_id),
  UNIQUE KEY uk_order_product (order_id, product_id)
) ENGINE=InnoDB COMMENT='评价表';
