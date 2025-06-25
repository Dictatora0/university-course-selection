-- 为Student表添加account_status列，默认值为启用状态(true/1)
ALTER TABLE Student ADD COLUMN account_status BOOLEAN DEFAULT TRUE;

-- 更新所有已有的记录，设置account_status为true
UPDATE Student SET account_status = TRUE;

-- 查看更改后的表结构
DESCRIBE Student; 