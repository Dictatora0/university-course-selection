-- 为TransactionControl表添加频繁转账预警相关字段
ALTER TABLE TransactionControl
ADD COLUMN frequent_transfer_time_window INT DEFAULT 30 COMMENT '频繁转账检测时间窗口（分钟）',
ADD COLUMN frequent_transfer_threshold INT DEFAULT 3 COMMENT '频繁转账检测阈值（次数）';

-- 更新现有记录，设置默认值
UPDATE TransactionControl
SET frequent_transfer_time_window = 30,
    frequent_transfer_threshold = 3;

-- 创建索引以优化频繁转账查询
CREATE INDEX idx_transaction_time_type ON Transaction(transaction_time, type); 