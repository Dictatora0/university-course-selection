-- 创建数据库
CREATE DATABASE IF NOT EXISTS course_selection DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE course_selection;

-- 院系表
CREATE TABLE IF NOT EXISTS Department (
    dept_id VARCHAR(20) PRIMARY KEY,
    dept_name VARCHAR(100) NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 学生表
CREATE TABLE IF NOT EXISTS Student (
    student_id VARCHAR(20) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    password VARCHAR(100) NOT NULL,
    birth_date DATE,
    id_card VARCHAR(18),
    address VARCHAR(255),
    dept_id VARCHAR(20),
    balance DECIMAL(10, 2) DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (dept_id) REFERENCES Department(dept_id) ON DELETE SET NULL
);

-- 课程表
CREATE TABLE IF NOT EXISTS Course (
    course_id VARCHAR(20) PRIMARY KEY,
    course_name VARCHAR(100) NOT NULL,
    dept_id VARCHAR(20),
    credit DECIMAL(3, 1) DEFAULT 3.0,
    description TEXT,
    capacity INT DEFAULT 50,
    current_enrollment INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (dept_id) REFERENCES Department(dept_id) ON DELETE SET NULL
);

-- 选课表
CREATE TABLE IF NOT EXISTS Enrollment (
    enrollment_id INT AUTO_INCREMENT PRIMARY KEY,
    student_id VARCHAR(20) NOT NULL,
    course_id VARCHAR(20) NOT NULL,
    enrollment_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    grade DECIMAL(5, 2),
    UNIQUE KEY unique_enrollment (student_id, course_id),
    FOREIGN KEY (student_id) REFERENCES Student(student_id) ON DELETE CASCADE,
    FOREIGN KEY (course_id) REFERENCES Course(course_id) ON DELETE CASCADE
);

-- 好友关系表
CREATE TABLE IF NOT EXISTS Friendship (
    friendship_id INT AUTO_INCREMENT PRIMARY KEY,
    student_id VARCHAR(20) NOT NULL,
    friend_id VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY unique_friendship (student_id, friend_id),
    FOREIGN KEY (student_id) REFERENCES Student(student_id) ON DELETE CASCADE,
    FOREIGN KEY (friend_id) REFERENCES Student(student_id) ON DELETE CASCADE
);

-- 消息表
CREATE TABLE IF NOT EXISTS Message (
    message_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    from_student_id VARCHAR(20) NOT NULL,
    to_student_id VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    send_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_read BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (from_student_id) REFERENCES Student(student_id) ON DELETE CASCADE,
    FOREIGN KEY (to_student_id) REFERENCES Student(student_id) ON DELETE CASCADE
);

-- 交易表
CREATE TABLE IF NOT EXISTS Transaction (
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

-- 登录日志表
CREATE TABLE IF NOT EXISTS LoginLog (
    log_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id VARCHAR(20) NOT NULL,
    login_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(50),
    device_info VARCHAR(255),
    FOREIGN KEY (student_id) REFERENCES Student(student_id) ON DELETE CASCADE
);

-- 插入一个默认院系
INSERT INTO Department (dept_id, dept_name, description) 
VALUES ('DEPT1', '计算机科学与技术', '计算机科学与技术学院');

-- 插入一个默认课程
INSERT INTO Course (course_id, course_name, dept_id, credit, description) 
VALUES ('C001', '数据库系统', 'DEPT1', 4.0, '数据库系统基础课程'); 