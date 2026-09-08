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
- [x] **W1D1b 造数据脚本**：已跑通，数据量：产品 100,000 / SKU 500,000 / 用户 500,000 / 订单 1,000,000 / 明细 1,999,068，耗时约 2 分钟
- [x] **W1D1c ER 图入 README + 提交**（commit 邮箱已全部重写为 noreply）
- [x] **W1D2 Spring Boot 骨架**：启动成功 + `/api/health` 通过；期间解决 Redis 端口冲突（本机 Windows Redis 服务抢占 6379，已停用）与 git SSL 证书问题（http.sslVerify false）
- [x] **W1D3 用户模块**：注册/登录/JWT/`/api/user/me` 全通（test01 用户 id=500001），错误 token 返回 401
- [x] **W1D4 产品模块**：分页列表/详情(含SKU)/新增/修改/上下架/分类树/目的地 9 步清单全通（admin 鉴权 403 生效；测试新品 id=100001）
- [ ] W1D5 第一波测试（JaCoCo）+ 提交
- [ ] W2 搜索筛选 + 订单链路 + Redis 防超卖（Lua 原子扣减 / 缓存三防）
- [ ] W3 慢 SQL 优化（EXPLAIN / 深分页 / 覆盖率 80%+）
- [ ] W4 CI/CD + 部署 + 压测 + 文档

## 三、10 张表清单（W3 优化对象）

`t_user` `t_category` `t_destination` `t_product` `t_product_sku` `t_cart` `t_order` `t_order_item` `t_payment` `t_review`

关键设计点：订单主键雪花 ID（分库分表铺路）；订单明细冗余快照；t_order 联合索引 `(user_id, create_time)`；t_product 索引 `(category_id, destination_id)` + `(status, sales)`。

## 三·五、已踩坑记录（重要）

- **MyBatis-Plus 3.5.9 分页插件找不到**：`PaginationInnerInterceptor` 从 `mybatis-plus-extension` 移到了独立模块 `mybatis-plus-jsqlparser`。pom 必须显式加：`com.baomidou:mybatis-plus-jsqlparser:${mybatis-plus.version}`（3.5.9 开始依赖 JSqlParser 的插件全部拆分到此模块）。
- **Apifox 占位域名**：新建接口默认地址 `dev-cn.your-api-server.com` 是示例占位符，必须改成 `http://localhost:8080`；环境 baseURL 无尾斜杠时，接口路径必须以 `/` 开头（否则拼成 `localhost:8080api/...` 404）。
- **JWT token 特征**：本系统 HS384 算法，token 以 `eyJhbGciOiJIUzM4NCJ9` 开头（含 typ 段的是 JWT.io 示例，不能用）。

## 四、下一步

**W1D5 第一波测试**：引入 spring-boot-starter-test + JaCoCo 覆盖率插件；给 JwtUtil、ResultCode、用户服务（BCrypt/JWT）写单元测试；给产品分页/详情写集成测试；`mvn test` 全绿 + JaCoCo 报告，提交 W1 收官 commit。
