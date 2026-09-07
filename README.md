# tuyou-booking# 途游预订 - 旅游产品预订系统（跟团游/门票/酒店）

\## 数据库设计（10 张表）



```mermaid

erDiagram

&#x20;   t\_user ||--o{ t\_order : "下单"

&#x20;   t\_user ||--o{ t\_cart : "加购"

&#x20;   t\_user ||--o{ t\_review : "评价"

&#x20;   t\_category ||--o{ t\_product : "分类"

&#x20;   t\_destination ||--o{ t\_product : "目的地"

&#x20;   t\_product ||--|{ t\_product\_sku : "日期库存"

&#x20;   t\_product ||--o{ t\_review : "被评"

&#x20;   t\_order ||--|{ t\_order\_item : "明细"

&#x20;   t\_order\_item }o--|| t\_product\_sku : "购买"

&#x20;   t\_order ||--o{ t\_payment : "支付"

```



表清单：t\_user / t\_category / t\_destination / t\_product / t\_product\_sku / t\_cart / t\_order / t\_order\_item / t\_payment / t\_review



