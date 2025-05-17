-- 使用正确的数据库
USE course_selection;

-- 检查交易表结构
DESC Transaction;

-- 修复Transaction表，添加必要的字段
ALTER TABLE Transaction
ADD COLUMN transaction_id_auto INT AUTO_INCREMENT FIRST,
MODIFY transaction_id VARCHAR(50) NOT NULL,
ADD PRIMARY KEY (transaction_id_auto);

-- 添加一条说明
SELECT '已更新Transaction表结构，添加了auto increment主键' AS '执行结果'; 