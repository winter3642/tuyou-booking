# 途游预订 · 项目进度档案（PROGRESS）

> **用途**：对话上下文丢失 / 想开新对话继续时，把本文件发给 AI 并说"继续途游项目"，即可恢复全部关键信息。
> **维护**：每完成一个大步骤由 AI 更新；建议随代码一起提交到 GitHub。
> 最后更新：2026-09-09

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
- [x] **W1D5 第一波测试**：4 个测试类 18 用例全绿（JwtUtil 4 / UserService 8 / ProductService 5 / Health 集成 1），JaCoCo 报告生成（分析 20 类）
- [x] **W2D1 产品搜索**：关键词/分类/目的地/价格区间/排序白名单 5 步验收全通；上架过滤生效（下架搜不到）；`keyword=桂林` 557ms 全表扫描记录在案（W3 优化基线）
- [x] **W2D2 购物车**：加购幂等（同 user+sku 唯一索引、先查后更，多次加购仅 1 行数量累加）6 步验收全通；改数量/删除归属校验生效（admin 越权改 test01 购物车返回 403）；列表批量 IN 组装避免 N+1
- [x] **W2D3 订单链路**：下单事务全通——雪花ID订单(222981114292080640)+明细快照+支付单(mock)+扣库存(44→40/104→103)+清购物车；条件扣库存 `UPDATE...WHERE stock>=?` 防超卖生效（超量下单→400库存不足→事务回滚无新订单）；越权查他人订单 403；测试 37 个全绿
- [x] **W2D4 Redis Lua 防超卖 + 缓存三防**（W2 收官）：`StockRedisService` Lua 原子扣减（未初始化返回 -1 / 不足返回 0 / 成功返回 1），下单链路"Redis 预扣 → DB 扣减兜底 → 失败回滚 Redis"；产品详情 Cache-Aside + 三防（空值缓存 60s 防穿透、Redisson 互斥锁防击穿、600s+0~300s 随机抖动防雪崩）。实测：`GET product:detail:1` 缓存 JSON + TTL 668；`GET product:detail:99999` → `\x00NULL\x00`；`stock:sku:4` nil→40(初始化)→39(预扣)，MySQL sku4=39 双写一致；超量(51>40)下单被 Lua 原子拒绝 400 且不落库。测试 47 个全绿（新增 StockRedis 6 + 订单回滚/初始化 2 + 缓存 3）
- [x] **W3D2 关键词搜索优化**：LIKE '%kw%' → ngram FULLTEXT（`ft_name`），EXPLAIN `type=fulltext/rows=1` 替代全索引扫描；实测低频词 SQL 88.7ms→37.8ms、API ~180ms→~100ms；高频词两方案持平（LIKE 提前截断 vs FULLTEXT 固定解析成本，取舍已入 README）
- [x] **W3D2 组合条件索引**：`idx_cat_dest_price`（等值在前、范围在后）+ `idx_cat_status_sales`（分类+上架+销量排序），无索引反扫 49631 行→范围扫描 964 行（5.1ms→3.4ms）
- [x] **W3D3 深分页延迟关联**：offset≥10000 走两步查询（覆盖索引取主键→IN 回表重排），EXPLAIN ANALYZE 91.5ms→21.6ms（CPU 口径）；API 级在 10 万行内存回表下两方案接近（~20ms），差距随数据量/冷缓存放大——诚实记录见 README
- [x] **W3D4 测试补全**：104 个测试全绿、JaCoCo **95.9%**（470/490 行，不含 entity/dto/vo）；补了锁分支 4 例、深分页 3 例、雪花 ID 4 例、拦截器 6 例、全局异常 4 例、缓存失效断言等
- [x] **W3D5 汇总**：优化前后对比表入 README（EXPLAIN 与 API 双口径）+ 提交
- [ ] W4 CI/CD + 部署 + 压测 + 文档（操作指南见 `docs/W4-操作指南.md`）

## 三、10 张表清单（W3 优化对象）

`t_user` `t_category` `t_destination` `t_product` `t_product_sku` `t_cart` `t_order` `t_order_item` `t_payment` `t_review`

关键设计点：订单主键雪花 ID（分库分表铺路）；订单明细冗余快照；t_order 联合索引 `(user_id, create_time)`；t_product 索引 `(category_id, destination_id)` + `(status, sales)`。

## 三·五、已踩坑记录（重要）

- **MyBatis-Plus 3.5.9 分页插件找不到**：`PaginationInnerInterceptor` 从 `mybatis-plus-extension` 移到了独立模块 `mybatis-plus-jsqlparser`。pom 必须显式加：`com.baomidou:mybatis-plus-jsqlparser:${mybatis-plus.version}`（3.5.9 开始依赖 JSqlParser 的插件全部拆分到此模块）。
- **Apifox 占位域名**：新建接口默认地址 `dev-cn.your-api-server.com` 是示例占位符，必须改成 `http://localhost:8080`；环境 baseURL 无尾斜杠时，接口路径必须以 `/` 开头（否则拼成 `localhost:8080api/...` 404）。
- **JWT token 特征**：本系统 HS384 算法，token 以 `eyJhbGciOiJIUzM4NCJ9` 开头（含 typ 段的是 JWT.io 示例，不能用）。
- **Mockito 与 3.5.9 重载歧义**：BaseMapper 有 `insert(T)` 和 `insert(Collection<T>)` 两个重载，测试里 `verify(...).insert(any())` 会报"引用不明确"，必须写 `any(User.class)` 等明确类型。
- **JaCoCo 用法**：pom 加 jacoco-maven-plugin（prepare-agent + report@verify），跑 `mvn test jacoco:report`，报告在 `target/site/jacoco/index.html`。
- **RedisScript 的包路径**：`RedisScript`/`DefaultRedisScript` 在 `org.springframework.data.redis.core.script` 子包，**不在** `core` 下（import 写错会"找不到符号"）。
- **SkuVO 字段**：只有 `id/departDate/stock/price`，**没有 productId**（BeanUtils 只复制同名属性，测试里别 set 不存在的字段）。
- **单元测试里的 ObjectMapper**：`new ObjectMapper()` 默认**不注册 JSR310 模块**，序列化 `LocalDate` 会抛异常（真实运行用的是 Spring Boot 自动配置的 mapper，没问题）；测试里必须 `new ObjectMapper().registerModule(new JavaTimeModule())`。
- **Mockito `thenReturn` 参数里别写可能抛异常的调用**：序列化等异常会被吞成误导性的 `UnfinishedStubbing`；先求值到变量再 stub。
- **verify 次数要对齐真实调用**：`redisNotInitThenOk` 场景 `tryDeduct` 真实被调 2 次（探测未初始化 + 初始化后重试），断言要 `times(2)`。
- **Mockito @Spy 与 @InjectMocks**：被注入的 `ObjectMapper` 用 `@Spy` + 字段初始化器注册模块，测试类内直接用同一实例做序列化。
- **@Sql 脚本中文乱码（W3D4 踩坑）**：Spring 读 `@Sql` 脚本默认用平台编码（Windows=GBK），UTF-8 脚本里的中文 INSERT 进库全是乱码，断言 expected 正确、actual 乱码，排查半天。修法：类上加 `@SqlConfig(encoding = "UTF-8")`。**排查手法**：别信终端显示（Git Bash 会把两边都显示成乱码），用 Python 按字节比对 surefire 报告与 .class 常量池。
- **pom 要显式 `project.build.sourceEncoding=UTF-8`**：虽然 Spring Boot parent 已设，但显式声明可防依赖 Maven 全局配置导致的中文编译乱码（W3D4 排障时顺手固化）。
- **测试数据库无 root 权限怎么办**：Docker 起专用测试 MySQL（`docker run -d --name tuyou-test-mysql -p 3307:3306 -e MYSQL_ROOT_PASSWORD=root -e MYSQL_DATABASE=tuyou_test mysql:8`），跑测试时 `TEST_DB_URL='jdbc:mysql://localhost:3307/tuyou_test?...' TEST_DB_USER=root TEST_DB_PASSWORD=root mvn test`（application-test.yml 已支持环境变量覆盖）。本机 MySQL 想建 tuyou_test 库需 root 执行 CREATE DATABASE + GRANT。
- **FULLTEXT 实测教训**：EXPLAIN ANALYZE 与 SHOW PROFILES 对 fulltext 的耗时口径差异大；全文索引有固定解析成本（本机 ~40-55ms/条），分页接口 COUNT+PAGE 两条查询都要付一次；LIKE 在高频词下靠 LIMIT 提前截断反而快。**优化要测边界场景 + 双口径记录，面试才经得起追问。**

## 四、下一步

**W4（4-5 天）**：按 `docs/W4-操作指南.md` 执行——Dockerfile + GitHub Actions（CI 里 MySQL/Redis 服务、测试库环境变量已就绪）→ 本机 Docker Compose 全流程 → JMeter 压测（下单 QPS + 1000 并发超卖验证）→ README/视频/Release → 简历对接。
