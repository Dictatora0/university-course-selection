# 在线选课系统 - 模型字段命名规范

## 引言

本文档定义了在线选课系统中的前后端数据交互字段命名规范，以确保在Java后端、MySQL数据库和JavaScript前端之间保持一致的命名风格。规范的命名约定将降低开发复杂度，减少错误发生概率，提高系统的可维护性和扩展性。

## 命名原则

1. **一致性**：同一概念在整个系统中应使用相同的命名
2. **完整性**：命名应具有描述性，避免过度缩写
3. **明确性**：命名应清晰表达字段用途，无歧义

## 命名规范

### 数据库表和字段命名

1. **表名**：使用单数形式，首字母大写的Pascal命名法，如`Student`、`Course`
2. **字段名**：使用snake_case（蛇形命名法），如`student_id`、`course_name`
3. **主键**：表名前缀加上`_id`，如`student_id`、`course_id`
4. **外键**：被引用表名前缀加上`_id`，如`dept_id`（引用Department表）
5. **布尔类型字段**：使用`is_`或`has_`前缀，如`is_active`、`has_paid`
6. **时间戳字段**：使用`created_at`、`updated_at`、`deleted_at`等后缀

### Java模型字段命名

1. **实体类**：使用首字母大写的Pascal命名法，如`Student`、`Course`
2. **属性**：使用驼峰命名法(camelCase)，如`studentId`、`courseName`
3. **JSON序列化**：使用`@SerializedName`注解，值与数据库字段名一致（snake_case），如：
   ```java
   @SerializedName("student_id")
   private String studentId;
   ```

### JavaScript/前端命名

1. **API请求/响应字段**：与Java的`@SerializedName`保持一致，使用snake_case，如：
   ```javascript
   const data = {
     student_id: "S2023001",
     course_id: "CS101"
   };
   ```

2. **变量命名**：使用驼峰命名法(camelCase)，如：
   ```javascript
   const studentId = response.data.student_id;
   ```

3. **HTML表单字段**：
   - `id`属性：使用驼峰命名法(camelCase)，如`studentId`
   - `name`属性：使用snake_case，与API字段保持一致，如`student_id`

## 常见字段命名规范

以下是系统中常用实体的标准字段命名：

### 学生(Student)

| 数据库字段 | Java属性 | 前端API字段 | 说明 |
|----------|----------|-----------|-----|
| student_id | studentId | student_id | 学号 |
| name | name | name | 姓名 |
| password | password | password | 密码（请求时） |
| birth_date | birthDate | birth_date | 出生日期 |
| id_card | idCard | id_card | 身份证号 |
| address | address | address | 地址 |
| created_at | createdAt | created_at | 创建时间 |
| balance | balance | balance | 账户余额 |
| dept_id | deptId | dept_id | 院系ID |

### 课程(Course)

| 数据库字段 | Java属性 | 前端API字段 | 说明 |
|----------|----------|-----------|-----|
| course_id | courseId | course_id | 课程编号 |
| course_name | courseName | course_name | 课程名称 |
| credits | credits | credits | 学分 |
| description | description | description | 课程描述 |
| teacher_id | teacherId | teacher_id | 教师ID |
| max_capacity | maxCapacity | max_capacity | 最大容量 |
| current_enrolled | currentEnrolled | current_enrolled | 当前已选人数 |
| dept_id | deptId | dept_id | 所属院系ID |

### 好友关系(Friendship)

| 数据库字段 | Java属性 | 前端API字段 | 说明 |
|----------|----------|-----------|-----|
| student_id | studentId | student_id | 学生ID |
| friend_id | friendId | friend_id | 好友ID |
| status | status | status | 状态（PENDING/ACCEPTED/REJECTED） |
| created_at | createdAt | created_at | 创建时间 |
| updated_at | updatedAt | updated_at | 更新时间 |
| reject_time | rejectTime | reject_time | 拒绝时间 |

### 消息(Message)

| 数据库字段 | Java属性 | 前端API字段 | 说明 |
|----------|----------|-----------|-----|
| message_id | messageId | message_id | 消息ID |
| from_student_id | fromStudentId | from_student_id | 发送者ID |
| to_student_id | toStudentId | to_student_id | 接收者ID |
| content | content | content | 消息内容 |
| send_time | sendTime | send_time | 发送时间 |
| is_read | isRead | is_read | 是否已读 |

## 实施措施

1. **代码审查**：在代码审查中特别关注字段命名是否遵循规范
2. **自动化检查**：使用`check_field_consistency.sh`工具定期检查字段命名一致性
3. **持续集成**：将字段命名检查集成到CI流程中

## 迁移策略

对于现有代码中不符合规范的部分，按照以下策略进行迁移：

1. 优先修改前端JavaScript代码以适应后端Java模型
2. 如需修改Java模型，确保保留原有的`@SerializedName`以保持兼容性
3. 修改数据库字段时，需要编写适当的迁移脚本

## 总结

遵循统一的字段命名规范将:
- 减少前后端集成时的错误
- 提高代码可读性与可维护性
- 简化API文档编写
- 加快新功能的开发速度

所有团队成员应当熟悉并严格遵守本规范。 