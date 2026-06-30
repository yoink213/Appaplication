-- Smart Warehouse Spring Boot 版本
-- 数据库初始化脚本
-- 直接使用原有数据库即可，无需修改

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for big_warehouse
-- ----------------------------
DROP TABLE IF EXISTS `big_warehouse`;
CREATE TABLE `big_warehouse`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `code` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '货物编号，格式 A-001',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '货物名称',
  `category` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '分类',
  `quantity` int NOT NULL DEFAULT 0 COMMENT '当前实时库存（已扣出库）',
  `price` decimal(10, 2) NOT NULL COMMENT '单价（元）',
  `location` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '存放货架位置',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '规格/描述',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `idx_code`(`code` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 11 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '大仓库总库存表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of big_warehouse
-- ----------------------------
INSERT INTO `big_warehouse` VALUES (1, 'A-001', '中性笔（0.5mm黑色）', '文具', 49350, 2.00, 'A区-1排-1层', '晨光品牌，12支/盒，书写流畅', '2026-06-29 14:59:33', '2026-06-29 14:59:33');
INSERT INTO `big_warehouse` VALUES (2, 'A-002', 'A4打印纸（500张/包）', '办公耗材', 49650, 25.00, 'A区-1排-2层', '得力品牌，70g/m²，双面打印不渗透', '2026-06-29 14:59:33', '2026-06-29 14:59:33');
INSERT INTO `big_warehouse` VALUES (3, 'A-003', '一次性纸杯（100只装）', '日用品', 40900, 12.00, 'B区-2排-1层', '加厚型，250ml，食品级材质', '2026-06-29 14:59:33', '2026-06-29 14:59:33');
INSERT INTO `big_warehouse` VALUES (4, 'A-004', '垃圾袋（50只装）', '日用品', 49850, 8.00, 'B区-3排-1层', '背心式，加厚不易破，45*50cm', '2026-06-29 14:59:33', '2026-06-29 14:59:33');
INSERT INTO `big_warehouse` VALUES (5, 'A-005', '矿泉水（550ml*24瓶）', '饮品', 49785, 30.00, 'C区-1排-1层', '农夫山泉，整箱装，饮用天然水', '2026-06-29 14:59:33', '2026-06-29 14:59:33');
INSERT INTO `big_warehouse` VALUES (6, 'A-006', '方便面（12桶装）', '食品', 49955, 45.00, 'C区-2排-3层', '康师傅红烧牛肉面，经典口味', '2026-06-29 14:59:33', '2026-06-29 14:59:33');
INSERT INTO `big_warehouse` VALUES (7, 'A-007', '充电宝（10000mAh）', '电子配件', 48200, 89.00, 'D区-1排-1层', '小米品牌，双向快充，轻薄便携', '2026-06-29 14:59:33', '2026-06-29 14:59:33');
INSERT INTO `big_warehouse` VALUES (8, 'A-008', '手机数据线（1m）', '电子配件', 49740, 15.00, 'D区-2排-2层', 'Type-C接口，支持快充，编织线材', '2026-06-29 14:59:33', '2026-06-29 14:59:33');
INSERT INTO `big_warehouse` VALUES (9, 'A-009', '抽纸（3层100抽*8包）', '日用品', 49250, 20.00, 'E区-1排-1层', '维达品牌，绵柔亲肤，无香型', '2026-06-29 14:59:33', '2026-06-29 14:59:33');
INSERT INTO `big_warehouse` VALUES (10, 'A-010', '洗衣液（2kg装）', '日用品', 49979, 35.00, 'E区-2排-1层', '蓝月亮深层洁净，薰衣草香，去渍强', '2026-06-29 14:59:33', '2026-06-29 14:59:33');

-- ----------------------------
-- Table structure for orders
-- ----------------------------
DROP TABLE IF EXISTS `orders`;
CREATE TABLE `orders`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `order_no` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '订单编号，格式 ORD-000',
  `small_item_code` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '小仓库编号，格式 001',
  `big_warehouse_id` int NOT NULL COMMENT '关联大仓库货物ID',
  `quantity` int NOT NULL COMMENT '出库数量',
  `total_price` decimal(10, 2) NOT NULL COMMENT '订单总价',
  `deadline` datetime NOT NULL COMMENT '派送截止日期',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态: 0-待接单 1-已接单 2-已取货 3-已送达 4-已完成 5-已拒绝',
  `admin_id` int NOT NULL COMMENT '派单管理员ID',
  `worker_id` int NULL DEFAULT NULL COMMENT '接单工人ID',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `idx_order_no`(`order_no` ASC) USING BTREE,
  UNIQUE INDEX `idx_small_code`(`small_item_code` ASC) USING BTREE,
  INDEX `idx_status_worker`(`status` ASC, `worker_id` ASC) USING BTREE,
  INDEX `fk_order_big`(`big_warehouse_id` ASC) USING BTREE,
  INDEX `fk_order_admin`(`admin_id` ASC) USING BTREE,
  INDEX `fk_order_worker`(`worker_id` ASC) USING BTREE,
  CONSTRAINT `fk_order_admin` FOREIGN KEY (`admin_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_order_big` FOREIGN KEY (`big_warehouse_id`) REFERENCES `big_warehouse` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_order_worker` FOREIGN KEY (`worker_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 51 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '订单/小仓任务表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of orders
-- ----------------------------
INSERT INTO `orders` VALUES (1, 'ORD-000', '001', 1, 100, 200.00, '2026-07-10 18:00:00', 0, 1, NULL, '2026-06-29 14:59:33', '2026-06-29 14:59:33');
INSERT INTO `orders` VALUES (2, 'ORD-001', '002', 2, 80, 2000.00, '2026-07-11 12:00:00', 0, 1, NULL, '2026-06-29 14:59:33', '2026-06-29 14:59:33');
INSERT INTO `orders` VALUES (3, 'ORD-002', '003', 3, 2000, 24000.00, '2026-07-09 20:00:00', 1, 1, 2, '2026-06-29 14:59:33', '2026-06-29 14:59:33');
INSERT INTO `orders` VALUES (4, 'ORD-003', '004', 4, 30, 240.00, '2026-07-12 15:00:00', 1, 1, 3, '2026-06-29 14:59:33', '2026-06-29 14:59:33');
INSERT INTO `orders` VALUES (5, 'ORD-004', '005', 5, 60, 1800.00, '2026-07-08 10:00:00', 2, 1, 2, '2026-06-29 14:59:33', '2026-06-29 14:59:33');
INSERT INTO `orders` VALUES (6, 'ORD-005', '006', 6, 10, 450.00, '2026-07-15 09:00:00', 2, 1, 4, '2026-06-29 14:59:33', '2026-06-29 14:59:33');
INSERT INTO `orders` VALUES (7, 'ORD-006', '007', 7, 500, 44500.00, '2026-07-14 17:00:00', 3, 1, 3, '2026-06-29 14:59:33', '2026-06-29 14:59:33');
INSERT INTO `orders` VALUES (8, 'ORD-007', '008', 8, 80, 1200.00, '2026-07-07 11:00:00', 4, 1, 2, '2026-06-29 14:59:33', '2026-06-29 14:59:33');
INSERT INTO `orders` VALUES (9, 'ORD-008', '009', 9, 100, 2000.00, '2026-07-06 16:00:00', 5, 1, 4, '2026-06-29 14:59:33', '2026-06-29 14:59:33');
INSERT INTO `orders` VALUES (10, 'ORD-009', '010', 10, 3, 105.00, '2026-07-20 10:00:00', 0, 1, NULL, '2026-06-29 14:59:33', '2026-06-29 14:59:33');

-- ----------------------------
-- Table structure for sys_sequence
-- ----------------------------
DROP TABLE IF EXISTS `sys_sequence`;
CREATE TABLE `sys_sequence`  (
  `seq_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '序列名称',
  `current_val` int NOT NULL DEFAULT 0 COMMENT '当前最新值',
  PRIMARY KEY (`seq_name`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '系统编号种子表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_sequence
-- ----------------------------
INSERT INTO `sys_sequence` VALUES ('ORDER_NO', -1);
INSERT INTO `sys_sequence` VALUES ('SMALL_ITEM', 0);

-- ----------------------------
-- Table structure for users
-- ----------------------------
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '登录账号',
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '登录密码（明文仅演示）',
  `role` tinyint NOT NULL COMMENT '角色 0-管理员 1-工人',
  `real_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '真实姓名',
  `balance` decimal(10, 2) NULL DEFAULT 0.00 COMMENT '工人账户余额',
  `status` tinyint NULL DEFAULT 1 COMMENT '账号状态 0-停用 1-启用',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `idx_username`(`username` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '系统用户表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of users
-- ----------------------------
INSERT INTO `users` VALUES (1, 'admin', '123456', 0, '系统管理员', 0.00, 1, '2026-06-29 14:59:33');
INSERT INTO `users` VALUES (2, 'worker_zhang', '123456', 1, '张师傅', 1280.50, 1, '2026-06-29 14:59:33');
INSERT INTO `users` VALUES (3, 'worker_li', '123456', 1, '李师傅', 560.00, 1, '2026-06-29 14:59:33');
INSERT INTO `users` VALUES (4, 'worker_wang', '123456', 1, '王师傅', 0.00, 1, '2026-06-29 14:59:33');

SET FOREIGN_KEY_CHECKS = 1;
