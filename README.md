# 在线选课系统

## 项目介绍

在线选课系统是一个基于Java Web技术构建的学生选课平台，支持学生注册、登录、查看课程、选课退课以及社交功能（好友添加、消息交流等）。系统采用传统的MVC架构，前端使用原生HTML/CSS/JavaScript，后端基于Servlet和JDBC技术，数据存储在MySQL数据库中。

## 系统特点

- **完整的选课业务流程**：支持课程浏览、选课、退课等核心功能
- **用户认证与会话管理**：完善的登录注册系统，基于Session的用户认证
- **社交功能**：好友添加、接受/拒绝好友请求、即时消息聊天，还可推荐好友
- **余额管理**：学生可充值、提现和转账，有转账记录和余额明细以及转账预警，管理员可录入课程、成绩等。
- **RESTful API设计**：规范的API设计，便于前后端分离
- **响应式界面**：适配不同设备的用户界面

## 技术栈

- **前端**：HTML5, CSS3, JavaScript (ES6+)
- **后端**：Java Servlet, JDBC
- **服务器**：Apache Tomcat 9.0
- **数据库**：MySQL 8.0
- **构建工具**：Maven

## 系统架构

系统采用经典的三层架构：

1. **表示层**：HTML/CSS/JavaScript构建的前端界面
2. **业务逻辑层**：Servlet处理HTTP请求，实现业务逻辑
3. **数据访问层**：DAO模式实现对数据库的操作

## 数据库设计

核心表结构包括：

- `Student`：学生信息，含学号、姓名、密码等字段
- `Course`：课程信息，含课程编号、名称、学分等字段
- `Enrollment`：选课记录，关联学生和课程
- `Friendship`：好友关系，记录两名学生之间的好友状态
- `Message`：消息记录，包含发送者、接收者、内容等信息
- `Transaction`：交易记录，记录充值、提现、转账等财务操作

## 主要功能模块

### 用户管理
- 学生注册与登录
- 个人信息维护
- 密码修改

### 课程管理
- 课程列表浏览
- 按院系筛选课程
- 课程详情查看

### 选课系统
- 选择课程
- 查看已选课程
- 退选课程

### 社交功能
- 好友搜索与添加
- 好友请求管理
- 好友间即时消息

### 财务管理
- 账户充值
- 余额提现
- 好友间转账

## 最近更新

### 2024-05-23 改进
1. **添加院系信息显示**：
   - 在个人信息页面添加院系信息显示
   - 在页面顶部添加院系名称显示
   - 修改loadProfileInfo函数以加载院系信息并保存到localStorage
   - 优化initUI函数以显示院系信息
2. **修复页面跳转问题**：
   - 修正了登录成功后无法正确跳转到学生仪表盘的问题
   - 使用绝对路径代替相对路径，确保跳转的可靠性
   - 优化了未登录用户的重定向逻辑
3. **完善转账功能**：
   - 实现了API.js中的transfer方法
   - 修复了好友间转账时可能出现的错误
4. **增强调试能力**：
   - 添加了详细的控制台日志，便于排查问题
   - 统一了错误处理方式

### 2024-05-22 改进
1. **修复消息发送功能**：
   - 修复了消息发送API中字段名不一致的问题
   - 在API.js中将消息发送请求的字段从`toId`改为`toStudentId`
   - 后端MessageServlet已兼容两种字段名

### 2024-05-21 改进
1. **添加了字段命名一致性检查工具**
2. **创建了FieldConsistencyTest单元测试类**

### 2024-05-20 改进
1. **改进用户会话管理**：
   - 修复了登录后用户信息不保存到localStorage的问题
   - 添加了重新获取用户信息的逻辑
   - 实现了登录成功后保存用户信息的功能

## 字段命名一致性问题

在开发过程中，我们发现了前后端交互中存在的字段命名不一致问题，主要集中在以下几个方面：

1. 消息发送：
   - 前端API使用`toId`发送消息接收者ID
   - 后端MessageServlet期望使用`toStudentId`字段
   - 已修复：后端添加了兼容逻辑，前端API已统一使用`toStudentId`

2. 好友请求：
   - 后端FriendshipServlet使用`friendId`字段
   - 前端API正确使用`friendId`字段，保持一致性

3. 学生信息：
   - 后端StudentServlet使用`student_id`作为登录字段
   - 登录后的信息返回也使用下划线命名法（如`student_id`、`birth_date`等）

## 已知问题与解决方案

1. **页面跳转问题**：
   - 问题：在某些情况下，页面跳转使用相对路径可能导致跳转失败
   - 解决方案：统一使用绝对路径进行页面跳转，根据当前路径动态构建目标URL

2. **字段命名不一致**：
   - 问题：前后端交互中字段命名风格不统一
   - 解决方案：API层增加字段映射，逐步统一命名规范

## 建议

1. 按照项目约定，统一使用驼峰命名法（camelCase）作为JSON字段命名规范
2. 所有新功能开发需先定义接口文档，确保前后端字段命名一致
3. 现有不一致的字段逐步修改，确保兼容性

## 联系方式

若有任何问题或建议，请联系项目维护者。

## 开发规范

- [字段命名规范](docs/field_naming_convention.md)：定义了前后端数据交互的字段命名规则
- [API文档](docs/api_documentation.md)：详细的API接口说明
- [数据库设计](docs/database_schema.md)：数据库表结构和关系说明

## 部署说明

### 环境要求
- JDK 11+
- MySQL 8.0+
- Apache Tomcat 9.0+

### 部署步骤
1. 克隆项目到本地
2. 在MySQL中创建名为`course_selection`的数据库
3. 执行`sql/init.sql`脚本，初始化数据库
4. 配置`src/config.properties`中的数据库连接信息
5. 编译项目：`mvn clean package`
6. 将生成的WAR文件部署到Tomcat的webapps目录
7. 启动Tomcat服务器
8. 访问`http://localhost:8081/course-selection`

## 测试账号

- 学生账号：S2023001，密码：123456
- 管理员账号：admin，密码：admin123

## 贡献指南

1. Fork本仓库
2. 创建功能分支：`git checkout -b feature/your-feature-name`
3. 提交更改：`git commit -m 'Add some feature'`
4. 推送到分支：`git push origin feature/your-feature-name`
5. 提交Pull Request

## 许可证

本项目采用MIT许可证，详情见[LICENSE](LICENSE)文件。

## 项目结构

- src/: Java源代码
  - dao/: 数据访问对象
  - model/: 数据模型
  - servlet/: Servlet控制器
  - util/: 工具类
- web/: Web资源
  - css/: 样式文件
  - js/: JavaScript文件
  - WEB-INF/: Web配置

## 功能特性

- 学生用户管理：注册、登录、个人信息管理
- 课程管理：浏览、选课、退课
- 好友系统：添加好友、接受/拒绝好友请求、删除好友
- 消息系统：好友之间发送消息
- 资金管理：余额充值、提现、转账给好友

## 安装与运行

1. 克隆本仓库
2. 配置MySQL数据库
3. 配置Tomcat服务器
4. 部署项目到Tomcat
5. 访问 http://localhost:8080/course-selection

## 更新日志

- 修复了登录后用户信息不保存到localStorage的问题
- 添加了student_dashboard.js中登录状态检查和用户信息获取的逻辑
- 在index.html中实现了登录成功后保存用户信息到localStorage的功能

- 添加了字段命名一致性检查工具（check_field_consistency.sh）
- 创建了FieldConsistencyTest单元测试类，用于验证字段命名一致性

- 修复了消息发送API中字段名不一致的问题：
  - 在API.js中将消息发送请求的字段从`toId`改为`toStudentId`，与后端接收的字段名一致
  - MessageServlet已兼容两种字段名，但API调用应使用统一的字段名

## 字段命名一致性问题

在开发过程中，我们发现了前后端交互中存在的字段命名不一致问题，主要集中在以下几个方面：

1. 消息发送：
   - 前端API使用`toId`发送消息接收者ID
   - 后端MessageServlet期望使用`toStudentId`字段
   - 已修复：后端添加了兼容逻辑，前端API已统一使用`toStudentId`

2. 好友请求：
   - 后端FriendshipServlet使用`friendId`字段
   - 前端API正确使用`friendId`字段，保持一致性

3. 学生信息：
   - 后端StudentServlet使用`student_id`作为登录字段
   - 登录后的信息返回也使用下划线命名法（如`student_id`、`birth_date`等）

## 建议

1. 按照项目约定，统一使用驼峰命名法（camelCase）作为JSON字段命名规范
2. 所有新功能开发需先定义接口文档，确保前后端字段命名一致
3. 现有不一致的字段逐步修改，确保兼容性

## 联系方式

若有任何问题或建议，请联系项目维护者。 