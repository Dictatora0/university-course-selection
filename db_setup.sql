-- 创建数据库
CREATE DATABASE IF NOT EXISTS course_selection DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 使用数据库
USE course_selection;

-- 创建学生表
CREATE TABLE IF NOT EXISTS Student (
    student_id VARCHAR(20) PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    birth_date DATE,
    id_card VARCHAR(18),
    address VARCHAR(200),
    password VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (id_card)
);

-- 创建院系表
CREATE TABLE IF NOT EXISTS Department (
    dept_id VARCHAR(20) PRIMARY KEY,
    dept_name VARCHAR(100) NOT NULL,
    UNIQUE (dept_name)
);

-- 创建课程表
CREATE TABLE IF NOT EXISTS Course (
    course_id VARCHAR(20) PRIMARY KEY,
    course_name VARCHAR(100) NOT NULL,
    dept_id VARCHAR(20) NOT NULL,
    credit DECIMAL(3,1) DEFAULT 3.0,
    FOREIGN KEY (dept_id) REFERENCES Department(dept_id),
    INDEX idx_fk_course_dept (dept_id)
);

-- 创建选课表
CREATE TABLE IF NOT EXISTS Enrollment (
    student_id VARCHAR(20),
    course_id VARCHAR(20),
    enrollment_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    grade DECIMAL(4,1),
    PRIMARY KEY (student_id, course_id),
    FOREIGN KEY (student_id) REFERENCES Student(student_id),
    FOREIGN KEY (course_id) REFERENCES Course(course_id),
    CHECK (grade IS NULL OR (grade >= 0.0 AND grade <= 100.0))
);

-- 创建朋友关系表
CREATE TABLE IF NOT EXISTS Friendship (
    student_id1 VARCHAR(20),
    student_id2 VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (student_id1, student_id2),
    FOREIGN KEY (student_id1) REFERENCES Student(student_id),
    FOREIGN KEY (student_id2) REFERENCES Student(student_id),
    CHECK (student_id1 < student_id2)
);

-- 创建消息表
CREATE TABLE IF NOT EXISTS Message (
    message_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    from_student_id VARCHAR(20) NOT NULL,
    to_student_id VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    send_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_read BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (from_student_id) REFERENCES Student(student_id),
    FOREIGN KEY (to_student_id) REFERENCES Student(student_id),
    INDEX idx_fk_message_from (from_student_id),
    INDEX idx_fk_message_to (to_student_id)
);

-- 创建登录日志表
CREATE TABLE IF NOT EXISTS LoginLog (
    log_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id VARCHAR(20) NOT NULL,
    login_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(50),
    device_info VARCHAR(200),
    FOREIGN KEY (student_id) REFERENCES Student(student_id),
    INDEX idx_fk_loginlog_student (student_id)
);

-- 创建交易记录表
CREATE TABLE IF NOT EXISTS Transaction (
    transaction_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sender_id VARCHAR(20) NOT NULL,
    receiver_id VARCHAR(20) NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    transaction_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    description VARCHAR(200),
    FOREIGN KEY (sender_id) REFERENCES Student(student_id),
    FOREIGN KEY (receiver_id) REFERENCES Student(student_id),
    INDEX idx_fk_transaction_sender (sender_id),
    INDEX idx_fk_transaction_receiver (receiver_id)
);

-- 注意：示例学生数据已移除，因为密码需要哈希存储。
-- 请通过应用程序的注册功能创建学生账户。 