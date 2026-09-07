# 途游预订 · 项目进度档案（PROGRESS）

> **用途**：对话上下文丢失 / 想开新对话继续时，把本文件发给 AI 并说"继续途游项目"，即可恢复全部关键信息。
> **维护**：每完成一个大步骤由 AI 更新；建议随代码一起提交到 GitHub。
> 最后更新：2026-09-07

---

## 一、环境清单（你的机器）

| 项 | 值 | 备注 |
|---|---|---|
| 项目路径 | `D:\projects\tuyou-booking` | GitHub 同名仓库 |
| GitHub | `winter3642/tuyou-booking`（Public） | 已开启邮箱隐私，提交用 noreply 邮箱 |
| Java / Maven | Java 17 / Maven 3.9.14 | Maven 已配阿里云镜像 |
| MySQL | 8.0.45 本机运行中，库 `tuyou`，用户 `tuyou`/`tuyou123` | 仅本地测试凭据 |
| Redis | Docker 容器 `tuyou-redis`，端口 6379，密码 `tuyou123` | 开机先开 Docker Desktop → `docker start tuyou-redis` |
| git | 已设 `http.sslVerify false`（本机 SSL 证书问题） | 学习项目可接受 |
| 提交规范 | `feat: / docs: / perf:` | 面试官会看 commit 历史 |

## 二、完成进度

- [x] **W0 环境准备**（Redis PONG / tuyou 库 / GitHub 仓库 2 commits）
- [x] **W1D1a 建表 SQL**：10 张表执行成功（`sql/init.sql`，`SHOW TABLES` 确认）
- [ ] W1D1b 造数据脚本（`scripts/gen_data.py`：产品 10 万 / SKU 50 万 / 用户 50 万 / 订单 100 万 / 明细 200 万）
- [ ] W1D1c ER 图入 README + 提交
- [ ] W1D2 Spring Boot 骨架（可启动 + health 接口）
- [ ] W1D3 用户模块（注册/登录/JWT，jjwt 0.12 新 API）
- [ ] W1D4 产品模块 CRUD + 分类/目的地
- [ ] W1D5 第一波测试（JaCoCo）+ 提交
- [ ] W2 搜索筛选 + 订单链路 + Redis 防超卖（Lua 原子扣减 / 缓存三防）
- [ ] W3 慢 SQL 优化（EXPLAIN / 深分页 / 覆盖率 80%+）
- [ ] W4 CI/CD + 部署 + 压测 + 文档

## 三、10 张表清单（W3 优化对象）

`t_user` `t_category` `t_destination` `t_product` `t_product_sku` `t_cart` `t_order` `t_order_item` `t_payment` `t_review`

关键设计点：订单主键雪花 ID（分库分表铺路）；订单明细冗余快照；t_order 联合索引 `(user_id, create_time)`；t_product 索引 `(category_id, destination_id)` + `(status, sales)`。

## 四、下一步

**W1D1b**：运行 `scripts/gen_data.py` 造数据 → `SELECT COUNT(*)` 核对数据量达标（产品 10 万+ / SKU 50 万+ / 用户 50 万+ / 订单 100 万+ / 明细 200 万+）。
