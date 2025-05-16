-- 学生表
CREATE TABLE Student (
    student_id VARCHAR(20) PRIMARY KEY,             -- 学号
    name VARCHAR(50) NOT NULL,                      -- 姓名
    birth_date DATE,                                -- 出生日期
    id_card VARCHAR(18) UNIQUE NOT NULL,            -- 身份证号
    address VARCHAR(200),                           -- 地址
    password VARCHAR(100) NOT NULL,                 -- 密码
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP  -- 创建时间
);

-- 院系表
CREATE TABLE Department (
    dept_id VARCHAR(20) PRIMARY KEY,                -- 院系编号
    dept_name VARCHAR(100) NOT NULL                 -- 院系名称
);

-- 课程表
CREATE TABLE Course (
    course_id VARCHAR(20) PRIMARY KEY,              -- 课程号
    course_name VARCHAR(100) NOT NULL,              -- 课程名称
    dept_id VARCHAR(20) NOT NULL,                   -- 开课院系
    credit DECIMAL(3,1) NOT NULL,                   -- 学分
    FOREIGN KEY (dept_id) REFERENCES Department(dept_id)
);

-- 选课表
CREATE TABLE Enrollment (
    student_id VARCHAR(20),
    course_id VARCHAR(20),
    grade DECIMAL(5,2),                             -- 成绩
    enrollment_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (student_id, course_id),
    FOREIGN KEY (student_id) REFERENCES Student(student_id),
    FOREIGN KEY (course_id) REFERENCES Course(course_id)
);

-- 好友关系表
CREATE TABLE Friendship (
    student_id1 VARCHAR(20),
    student_id2 VARCHAR(20),
    friendship_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (student_id1, student_id2),
    FOREIGN KEY (student_id1) REFERENCES Student(student_id),
    FOREIGN KEY (student_id2) REFERENCES Student(student_id),
    CHECK (student_id1 < student_id2)               -- 确保不重复记录
);

-- 转账记录表
CREATE TABLE Transaction (
    transaction_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    from_student_id VARCHAR(20) NOT NULL,
    to_student_id VARCHAR(20) NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    transaction_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (from_student_id) REFERENCES Student(student_id),
    FOREIGN KEY (to_student_id) REFERENCES Student(student_id)
);

-- 消息记录表
CREATE TABLE Message (
    message_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    from_student_id VARCHAR(20) NOT NULL,
    to_student_id VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    send_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_read BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (from_student_id) REFERENCES Student(student_id),
    FOREIGN KEY (to_student_id) REFERENCES Student(student_id)
);

-- 登录记录表
CREATE TABLE LoginLog (
    log_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id VARCHAR(20) NOT NULL,
    login_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(50),
    FOREIGN KEY (student_id) REFERENCES Student(student_id)
);

-- 插入一些测试数据
INSERT INTO Department VALUES ('CS', '计算机科学与技术');
INSERT INTO Department VALUES ('MA', '数学');
INSERT INTO Department VALUES ('PH', '物理');
INSERT INTO Department VALUES ('CH', '化学');
INSERT INTO Department VALUES ('EN', '英语');

INSERT INTO Student (student_id, name, birth_date, id_card, address, password) VALUES
('2021001', '张三', '2000-01-01', '110101200001010011', '北京市海淀区', '123456'),
('2021002', '李四', '2000-02-02', '110101200002020022', '北京市朝阳区', '123456'),
('2021003', '王五', '2000-03-03', '110101200003030033', '北京市西城区', '123456'),
('2021004', '赵六', '2000-04-04', '110101200004040044', '北京市东城区', '123456'),
('2021005', '钱七', '2000-05-05', '110101200005050055', '北京市丰台区', '123456');

INSERT INTO Course VALUES ('CS101', '程序设计基础', 'CS', 4.0);
INSERT INTO Course VALUES ('CS201', '数据结构', 'CS', 4.0);
INSERT INTO Course VALUES ('CS301', '数据库原理', 'CS', 3.0);
INSERT INTO Course VALUES ('MA101', '高等数学', 'MA', 5.0);
INSERT INTO Course VALUES ('PH101', '大学物理', 'PH', 4.0); 