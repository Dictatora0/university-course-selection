-- 使用正确的数据库
USE course_selection;

-- 删除现有表并重新创建
DROP TABLE IF EXISTS Transaction;

-- 创建正确的Transaction表
CREATE TABLE Transaction (
    transaction_id VARCHAR(50) PRIMARY KEY,
    student_id VARCHAR(20) NOT NULL,
    related_student_id VARCHAR(20),
    amount DECIMAL(10, 2) NOT NULL,
    type VARCHAR(20) NOT NULL,
    description VARCHAR(255),
    transaction_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES Student(student_id) ON DELETE CASCADE,
    FOREIGN KEY (related_student_id) REFERENCES Student(student_id) ON DELETE SET NULL
);

-- 为自动生成交易ID创建一个辅助表
CREATE TABLE IF NOT EXISTS TransactionIdSequence (
    id INT AUTO_INCREMENT PRIMARY KEY,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建一个存储过程，用于生成唯一交易ID
DELIMITER //
DROP PROCEDURE IF EXISTS GenerateTransactionId //
CREATE PROCEDURE GenerateTransactionId(INOUT new_id VARCHAR(50))
BEGIN
    INSERT INTO TransactionIdSequence (created_at) VALUES (NOW());
    SET new_id = CONCAT('TRX', LPAD(LAST_INSERT_ID(), 10, '0'));
END //
DELIMITER ;

-- 添加一条说明
SELECT '已创建新的Transaction表结构和交易ID生成机制' AS '执行结果'; 