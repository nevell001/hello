-- 热销商品功能
ALTER TABLE products ADD COLUMN is_hot TINYINT(1) DEFAULT 0 COMMENT '是否热销商品';
