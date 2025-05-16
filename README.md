# 大学生在线选课系统

这是一个基于Java和MySQL的大学生在线选课系统，实现了学生选课、成绩管理、好友管理、好友之间的消息发送和转账等功能。
目前系统使用的是前端模拟数据，所以所有的操作都只会在前端显示，不会实际与后端交互。

## 功能特点

- **用户管理**：学生注册和登录功能，记录学生登录日志
- **选课系统**：学生可以浏览和选择课程，查看已选课程和成绩
- **成绩管理**：管理员可以录入学生成绩，学生可以查看自己的成绩
- **好友系统**：学生可以添加好友，查看好友列表，系统提供好友推荐功能
- **消息系统**：好友之间可以发送和接收消息
- **转账功能**：好友之间可以相互转账，系统提供转账报警功能

## 系统架构

- **前端**：HTML, CSS, JavaScript, Bootstrap
- **后端**：Java, Servlet, JDBC
- **数据库**：MySQL

## 数据库设计

系统包含以下主要表：
- `Student`：学生信息表
- `Department`：院系信息表
- `Course`：课程信息表
- `Enrollment`：选课记录表
- `Friendship`：好友关系表
- `Message`：消息记录表
- `Transaction`：转账记录表
- `LoginLog`：登录日志表

## 安装与使用

### 环境要求
- JDK 8+
- MySQL 5.7+
- Tomcat 8+

### 安装步骤

1. 克隆代码库
   ```
   git clone https://github.com/username/university-course-selection.git
   ```

2. 创建数据库
   ```sql
   CREATE DATABASE university;
   ```

3. 使用`database.sql`脚本创建表结构和初始数据
   ```
   mysql -u username -p university < database.sql
   ```

4. 配置数据库连接
   在`src/util/DBUtil.java`中修改数据库连接参数

5. 编译项目
   ```
   javac -d classes src/**/*.java
   ```

6. 部署到Tomcat

### 使用说明

- 管理员可以通过`/admin`路径访问管理员界面
- 学生可以通过主页`/`访问学生登录界面

## 高级功能

- **好友推荐**：基于共同好友关系进行好友推荐
- **转账报警**：当短时间内发生大量转账时，系统会自动报警

## 项目结构

```
.
├── database.sql          # 数据库脚本
├── src/                  # 源代码
│   ├── model/            # 实体类
│   ├── dao/              # 数据访问对象
│   ├── service/          # 业务逻辑
│   ├── util/             # 工具类
│   └── web/              # Servlet控制器
├── web/                  # 前端文件
│   ├── css/              # 样式文件
│   ├── js/               # JavaScript文件
│   └── img/              # 图片资源
└── README.md             # 项目说明
``` 