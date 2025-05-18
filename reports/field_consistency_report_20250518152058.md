# 字段一致性检查报告
生成时间: 2025年 5月18日 星期日 15时20分58秒 CST

## Java模型类字段

### Message

| Java字段名 | SerializedName | 数据类型 |
|------------|----------------|----------|
| messageId
fromStudentId | *未指定* | Long
String |
| toStudentId
content | *未指定* | String
String |
| sendTime
isRead | *未指定* | Date
boolean |
| fromStudentName
toStudentName | *未指定* | String
String |

### TransactionControl

| Java字段名 | SerializedName | 数据类型 |
|------------|----------------|----------|
| controlId
maxSingleAmount | *未指定* | int
BigDecimal |
| dailyLimit
maxDailyTransactions | *未指定* | BigDecimal
int |
| enabled
lastUpdated | *未指定* | boolean
Date |
| updatedBy
updaterName | *未指定* | String
String |

### LoginLog

| Java字段名 | SerializedName | 数据类型 |
|------------|----------------|----------|
| logId
studentId | *未指定* | Long
String |
| loginTime
ipAddress | *未指定* | Date
String |
| deviceInfo
studentName | *未指定* | String
String |

### Department

| Java字段名 | SerializedName | 数据类型 |
|------------|----------------|----------|
| deptId
deptName | *未指定* | String
String |

### Administrator

| Java字段名 | SerializedName | 数据类型 |
|------------|----------------|----------|
| adminId
name | *未指定* | String
String |
| password
role | *未指定* | String
String |
| createdAt
lastLogin | *未指定* | Date
Date |

### TransactionAlert

| Java字段名 | SerializedName | 数据类型 |
|------------|----------------|----------|
| alertId
studentId | *未指定* | int
String |
| alertTime
alertType | *未指定* | Date
String |
| description
resolved | *未指定* | String
boolean |
| resolvedBy
resolvedAt | *未指定* | String
Date |
| studentName
resolverName | *未指定* | String
String |

### Friendship

| Java字段名 | SerializedName | 数据类型 |
|------------|----------------|----------|
| studentId1
studentId2 | *未指定* | String
String |
| friendshipDate
friendName | *未指定* | Timestamp
String |
| friendDepartment
status | *未指定* | String
FriendshipStatus |
| requestTime
confirmTime | *未指定* | Timestamp
Timestamp |

### Course

| Java字段名 | SerializedName | 数据类型 |
|------------|----------------|----------|
| courseId
courseName | *未指定* | String
String |
| deptId
credit | *未指定* | String
BigDecimal |

### Enrollment

| Java字段名 | SerializedName | 数据类型 |
|------------|----------------|----------|
| studentId
courseId | *未指定* | String
String |
| grade
enrollmentDate | *未指定* | BigDecimal
Date |
| courseName
credit | *未指定* | String
BigDecimal |

### CourseRecommendation

| Java字段名 | SerializedName | 数据类型 |
|------------|----------------|----------|
| recommendationId
studentId | *未指定* | int
String |
| courseId
recommendScore | *未指定* | String
double |
| reason
createdAt | *未指定* | String
Date |
| viewed
viewedAt | *未指定* | boolean
Date |
| accepted
acceptedAt | *未指定* | boolean
Date |
| studentName
courseName | *未指定* | String
String |
| courseTeacher
courseTimeInfo | *未指定* | String
String |

### Student

| Java字段名 | SerializedName | 数据类型 |
|------------|----------------|----------|
| studentId | student_id | String |
| name | birth_date | String |
| birthDate | id_card | Date |
| idCard
address | *未指定* | String
String |
| password | created_at | String |
| createdAt
balance | *未指定* | Date
double |
| deptId | department_id | String |
| deptName | dept_name | String |
| status | status | String |
| recommendReason | recommend_reason | String |
| rejectTime | reject_time | Timestamp |

### Transaction

| Java字段名 | SerializedName | 数据类型 |
|------------|----------------|----------|
| transactionId
studentId | *未指定* | String
String |
| type
amount | *未指定* | TransactionType
BigDecimal |
| transactionDate
description | *未指定* | Timestamp
String |
| relatedStudentId
relatedUserId | *未指定* | String
String |
| status
studentName | *未指定* | TransactionStatus
String |

## JavaScript API字段

### admin_dashboard.js

| API路径 | 字段名 | 使用位置 |
|---------|--------|----------|

### login.js

| API路径 | 字段名 | 使用位置 |
|---------|--------|----------|
|  | body | 第�� |
|  | student_id | 第�� |
|  | password | 第�� |
|  | body | 第�� |
|  | admin_id | 第�� |
|  | password | 第�� |

### api.js

| API路径 | 字段名 | 使用位置 |
|---------|--------|----------|
|  | body | 第�� |
|  | student_id | 第�� |
|  | password | 第�� |
|  | body | 第�� |
|  | body | 第�� |
|  | courseId | 第�� |
|  | body | 第�� |
|  | body | 第�� |
|  | body | 第�� |
|  | body | 第�� |
|  | friendId | 第�� |
|  | body | 第�� |
|  | toId | 第�� |
|  | content | 第�� |

### student_dashboard.js

| API路径 | 字段名 | 使用位置 |
|---------|--------|----------|

## HTML表单字段

### index.html

| 表单ID | 输入字段ID | 字段名称 |
|--------|------------|----------|

### student_dashboard.html

| 表单ID | 输入字段ID | 字段名称 |
|--------|------------|----------|
| addFriendForm | addFriendIdInput | *未指定* |
| mainMessageForm | mainMessageInput | *未指定* |
| chatWindowMessageForm | chatWindowInput | *未指定* |
| transferForm | transferRecipientId | *未指定* |
| transferForm | transferAmountInput_modal | *未指定* |
| transferForm | transferNotes | *未指定* |

### admin_login.html

| 表单ID | 输入字段ID | 字段名称 |
|--------|------------|----------|
| adminLoginForm | adminId | *未指定* |
| adminLoginForm | password | *未指定* |

### admin_dashboard.html

| 表单ID | 输入字段ID | 字段名称 |
|--------|------------|----------|

## 潜在的字段不一致

### Java中存在但JavaScript中可能不同名的字段

| Java序列化名 | JavaScript中可能的对应字段 |
|--------------|----------------------------|

## 建议

1. 确保前后端字段命名一致，特别是API请求和响应中的字段。
2. 优先使用后端定义的字段名（通过@SerializedName指定）。
3. 在JavaScript中使用与后端一致的字段名，避免额外的映射工作。
4. 对于新功能，确保从一开始就保持字段名的一致性。
