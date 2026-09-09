-- W3D4 测试清理脚本（@Sql AFTER_TEST_METHOD 执行）
-- 清空测试库全部表，保证每个测试方法数据独立
DELETE FROM t_order_item;
DELETE FROM t_payment;
DELETE FROM t_order;
DELETE FROM t_cart;
DELETE FROM t_product_sku;
DELETE FROM t_product;
DELETE FROM t_category;
DELETE FROM t_destination;
DELETE FROM t_user;
