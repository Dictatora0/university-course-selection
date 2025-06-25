USE course_selection;

DROP TABLE IF EXISTS Administrator;

CREATE TABLE Administrator (
    admin_id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    password VARCHAR(255) NOT NULL, -- Should be hashed in a real application
    role VARCHAR(50) NOT NULL,       -- e.g., SUPER_ADMIN, COURSE_ADMIN
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP NULL DEFAULT NULL
);

-- 插入默认管理员账号 (密码为明文 123456，实际应用中应该使用哈希值)
INSERT INTO Administrator (admin_id, name, password, role) VALUES 
('admin100', '系统管理员', '123456', 'SUPER_ADMIN');

SELECT 'Administrator table created successfully' AS 'Execution Result'; 