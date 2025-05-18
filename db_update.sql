-- 使用course_selection数据库
USE course_selection;

-- 为Student表添加account_status字段
ALTER TABLE Student ADD COLUMN account_status BOOLEAN DEFAULT TRUE;

-- 更新所有现有记录的account_status为TRUE（启用状态）
UPDATE Student SET account_status = TRUE WHERE account_status IS NULL;