# -*- coding: utf-8 -*-
"""
途游预订 · 造数据脚本（W1D1b）

生成：分类 / 目的地 / 产品 / SKU / 用户 / 订单 / 订单明细
数据量（文档目标）：产品 10 万+ / SKU 50 万+ / 用户 50 万+ / 订单 100 万+ / 明细 200 万+
本机跑不动？把下方 N_* 减半（如 N_ORDER=500_000），仍能测出慢 SQL 差距。

运行：
    pip install pymysql
    python gen_data.py
"""
import os
import random
import time
from datetime import date, datetime, timedelta

import pymysql

# ================= 数据量配置（按需调整） =================
N_PRODUCT = 100_000        # 产品数
SKU_PER_PRODUCT = 5        # 每产品 SKU 数（日期粒度）
N_USER = 500_000           # 用户数
N_ORDER = 1_000_000        # 订单数（明细约 2 倍）
FLUSH = 5_000              # 每批插入行数

DEST = ['北京', '上海', '南京', '杭州', '三亚', '成都', '西安', '厦门', '丽江', '桂林']
DEST_PY = {'北京': 'beijing', '上海': 'shanghai', '南京': 'nanjing', '杭州': 'hangzhou',
           '三亚': 'sanya', '成都': 'chengdu', '西安': 'xian', '厦门': 'xiamen',
           '丽江': 'lijiang', '桂林': 'guilin'}

# 连接参数环境变量化：默认连本机 3306 主库，压测环境用 TUYOU_DB_PORT=3307 指向 Docker Compose 的 MySQL
conn = pymysql.connect(
    host=os.environ.get('TUYOU_DB_HOST', 'localhost'),
    port=int(os.environ.get('TUYOU_DB_PORT', '3306')),
    user=os.environ.get('TUYOU_DB_USER', 'tuyou'),
    password=os.environ.get('TUYOU_DB_PASSWORD', 'tuyou123'),
    database=os.environ.get('TUYOU_DB_NAME', 'tuyou'),
    charset='utf8mb4')
cur = conn.cursor()
t0 = time.time()


def log(msg):
    print(f"[{time.time() - t0:8.1f}s] {msg}", flush=True)


# ---------- 1. 分类（三级树，12 条） ----------
cats = [('跟团游', 1, 0, 1), ('门票', 1, 0, 2), ('酒店', 1, 0, 3),
        ('国内游', 2, 1, 1), ('出境游', 2, 1, 2), ('景区门票', 2, 2, 1),
        ('国内酒店', 2, 3, 1), ('境外酒店', 2, 3, 2),
        ('华东', 3, 4, 1), ('华北', 3, 4, 2), ('华南', 3, 4, 3), ('西南', 3, 4, 4)]
cur.executemany(
    "INSERT INTO t_category(name,level,parent_id,sort) VALUES(%s,%s,%s,%s)", cats)
conn.commit()
log(f"分类 {len(cats)} 条")

# ---------- 2. 目的地（10 条） ----------
dests = [(d, DEST_PY[d], random.randint(0, 1)) for d in DEST]
cur.executemany(
    "INSERT INTO t_destination(name,pinyin,hot_flag) VALUES(%s,%s,%s)", dests)
conn.commit()
log(f"目的地 {len(dests)} 条")

# ---------- 3. 产品（10 万） ----------
rows = []
for i in range(1, N_PRODUCT + 1):
    rows.append((random.randint(1, 3), random.randint(1, 10),
                 f'{random.choice(DEST)}{random.randint(1, 9)}日游产品{i}',
                 f'产品{i}副标题',
                 round(random.uniform(99, 9999), 2),
                 random.randint(0, 100000),
                 round(random.uniform(3.5, 5.0), 1),
                 random.randint(0, 1)))
    if len(rows) >= FLUSH:
        cur.executemany(
            "INSERT INTO t_product(category_id,destination_id,name,subtitle,price,sales,score,status)"
            " VALUES(%s,%s,%s,%s,%s,%s,%s,%s)", rows)
        conn.commit()
        rows = []
        if i % 20000 == 0:
            log(f"产品 {i}/{N_PRODUCT}")
if rows:
    cur.executemany(
        "INSERT INTO t_product(category_id,destination_id,name,subtitle,price,sales,score,status)"
        " VALUES(%s,%s,%s,%s,%s,%s,%s,%s)", rows)
    conn.commit()
log(f"产品完成 {N_PRODUCT}")

# ---------- 4. SKU（每产品 5 个日期 = 50 万） ----------
rows = []
for pid in range(1, N_PRODUCT + 1):
    base = random.uniform(99, 9999)
    for _ in range(SKU_PER_PRODUCT):
        d = date(2026, 1, 1) + timedelta(days=random.randint(0, 365))
        rows.append((pid, d, random.randint(10, 500),
                     round(base * random.uniform(0.9, 1.3), 2)))
    if len(rows) >= FLUSH:
        cur.executemany(
            "INSERT INTO t_product_sku(product_id,depart_date,stock,price)"
            " VALUES(%s,%s,%s,%s)", rows)
        conn.commit()
        rows = []
        if pid % 20000 == 0:
            log(f"SKU {pid * SKU_PER_PRODUCT}/{N_PRODUCT * SKU_PER_PRODUCT}")
if rows:
    cur.executemany(
        "INSERT INTO t_product_sku(product_id,depart_date,stock,price)"
        " VALUES(%s,%s,%s,%s)", rows)
    conn.commit()
log(f"SKU 完成 {N_PRODUCT * SKU_PER_PRODUCT}")

# ---------- 5. 用户（50 万，password 为占位哈希） ----------
BCRYPT_PLACEHOLDER = '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy'
rows = []
for i in range(1, N_USER + 1):
    rows.append((f'user_{i}', BCRYPT_PLACEHOLDER, f'138{i:08d}', 1))
    if len(rows) >= FLUSH:
        cur.executemany(
            "INSERT INTO t_user(username,password,phone,status) VALUES(%s,%s,%s,%s)", rows)
        conn.commit()
        rows = []
        if i % 100000 == 0:
            log(f"用户 {i}/{N_USER}")
if rows:
    cur.executemany(
        "INSERT INTO t_user(username,password,phone,status) VALUES(%s,%s,%s,%s)", rows)
    conn.commit()
log(f"用户完成 {N_USER}")

# ---------- 6. 订单（100 万）+ 明细（约 200 万） ----------
order_rows, item_rows = [], []
for i in range(1, N_ORDER + 1):
    uid = random.randint(1, N_USER)
    status = random.choices([0, 1, 2, 3], weights=[40, 40, 10, 10])[0]
    create = datetime(2025, 9, 7) + timedelta(
        days=random.randint(0, 365), minutes=random.randint(0, 1439))
    pay_t = create + timedelta(minutes=random.randint(5, 180)) if status in (1, 3) else None
    cancel_t = create + timedelta(minutes=random.randint(5, 600)) if status == 2 else None

    total = 0
    for _ in range(random.randint(1, 3)):
        sku_id = random.randint(1, N_PRODUCT * SKU_PER_PRODUCT)
        pid = (sku_id - 1) // SKU_PER_PRODUCT + 1
        price = round(random.uniform(99, 9999), 2)
        qty = random.randint(1, 3)
        total += round(price * qty, 2)
        item_rows.append((i, pid, sku_id, f'测试产品{pid}', price, qty))

    order_rows.append((i, f'T{i:010d}', uid, round(total, 2), status,
                       pay_t, cancel_t, create))

    if i % 50000 == 0:
        cur.executemany(
            "INSERT INTO t_order(id,order_no,user_id,total_amount,status,pay_time,cancel_time,create_time)"
            " VALUES(%s,%s,%s,%s,%s,%s,%s,%s)", order_rows)
        cur.executemany(
            "INSERT INTO t_order_item(order_id,product_id,sku_id,product_name,price,quantity)"
            " VALUES(%s,%s,%s,%s,%s,%s)", item_rows)
        conn.commit()
        order_rows, item_rows = [], []
        log(f"订单 {i}/{N_ORDER}（明细累计 {i * 2:.0f} 万左右）")
if order_rows:
    cur.executemany(
        "INSERT INTO t_order(id,order_no,user_id,total_amount,status,pay_time,cancel_time,create_time)"
        " VALUES(%s,%s,%s,%s,%s,%s,%s,%s)", order_rows)
    cur.executemany(
        "INSERT INTO t_order_item(order_id,product_id,sku_id,product_name,price,quantity)"
        " VALUES(%s,%s,%s,%s,%s,%s)", item_rows)
    conn.commit()
log(f"订单/明细 完成")

# ---------- 汇总核对 ----------
for t in ['t_category', 't_destination', 't_product', 't_product_sku',
          't_user', 't_order', 't_order_item']:
    cur.execute(f"SELECT COUNT(*) FROM {t}")
    log(f"{t}: {cur.fetchone()[0]:,}")

conn.close()
log("全部完成，可在 MySQL 里 SELECT COUNT(*) 复核")
