# tuyou-booking# 途游预订 - 旅游产品预订系统（跟团游/门票/酒店）

## 数据库设计（10 张表）
```mermaid
erDiagram
    t_user ||--o{ t_order : "下单"
    t_user ||--o{ t_cart : "加购"
    t_user ||--o{ t_review : "评价"
    t_category ||--o{ t_product : "分类"
    t_destination ||--o{ t_product : "目的地"
    t_product ||--|{ t_product_sku : "日期库存"
    t_product ||--o{ t_review : "被评"
    t_order ||--|{ t_order_item : "明细"
    t_order_item }o--|| t_product_sku : "购买"
    t_order ||--o{ t_payment : "支付"



