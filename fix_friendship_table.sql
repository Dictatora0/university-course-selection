-- 使用正确的数据库
USE course_selection;

-- 先检查是否存在表，如果存在则重新创建
DROP TABLE IF EXISTS Friendship;

-- 创建具有正确结构的Friendship表
CREATE TABLE Friendship (
    friendship_id INT AUTO_INCREMENT PRIMARY KEY,
    student_id VARCHAR(20) NOT NULL,
    friend_id VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, ACCEPTED, REJECTED
    request_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    confirm_time TIMESTAMP NULL,
    UNIQUE KEY unique_friendship (student_id, friend_id),
    FOREIGN KEY (student_id) REFERENCES Student(student_id) ON DELETE CASCADE,
    FOREIGN KEY (friend_id) REFERENCES Student(student_id) ON DELETE CASCADE
);

-- 添加一条说明
SELECT '已更新Friendship表结构，添加了status、request_time和confirm_time字段' AS '执行结果'; 