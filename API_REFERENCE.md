# 在线选课系统 API 参考文档

本文档详细列出了系统中所有可用的API接口，包括请求路径、参数、返回格式以及可能的错误代码。

## 通用格式

所有API返回JSON格式数据，一般包含以下字段：

```json
{
  "success": true/false,      // 请求是否成功
  "message": "操作结果描述",   // 请求结果的文字描述
  "data": {}                  // 返回的数据，成功时存在
}
```

错误响应格式：

```json
{
  "success": false,
  "message": "错误描述"
}
```

## 认证与授权

大部分API需要登录后才能访问。登录成功后，服务器会设置SESSION，因此前端请求需要带上COOKIE。

## API 列表

### 学生管理

#### 学生登录

- **路径**: `/api/students/login`
- **方法**: `POST`
- **参数**:
  ```json
  {
    "student_id": "学生ID",
    "password": "密码"
  }
  ```
- **成功响应**:
  ```json
  {
    "success": true,
    "message": "登录成功",
    "data": {
      "student_id": "学生ID",
      "name": "学生姓名",
      "balance": 123.45
    }
  }
  ```

#### 学生注册

- **路径**: `/api/students/register`
- **方法**: `POST`
- **参数**:
  ```json
  {
    "student_id": "学生ID",
    "name": "学生姓名",
    "password": "密码",
    "dept_id": "院系ID"
  }
  ```
- **成功响应**:
  ```json
  {
    "success": true,
    "message": "注册成功",
    "data": {
      "student_id": "学生ID",
      "name": "学生姓名"
    }
  }
  ```

#### 获取学生信息

- **路径**: `/api/students/getInfo`
- **方法**: `GET`
- **参数**: 无
- **成功响应**:
  ```json
  {
    "success": true,
    "message": "操作成功",
    "data": {
      "student_id": "学生ID",
      "name": "学生姓名",
      "balance": 123.45
    }
  }
  ```

### 课程相关

#### 获取所有课程

- **路径**: `/api/courses`
- **方法**: `GET`
- **参数**: 无
- **成功响应**:
  ```json
  {
    "success": true,
    "message": "获取课程列表成功",
    "data": [
      {
        "course_id": "C001",
        "name": "数据库原理",
        "credit": 3,
        "teacher_name": "张教授",
        "max_student": 30,
        "current_student": 25
      }
    ]
  }
  ```

#### 选课

- **路径**: `/api/selection/enroll`
- **方法**: `POST`
- **参数**:
  ```json
  {
    "course_id": "课程ID"
  }
  ```
- **成功响应**:
  ```json
  {
    "success": true,
    "message": "选课成功"
  }
  ```

#### 退课

- **路径**: `/api/selection/drop`
- **方法**: `POST`
- **参数**:
  ```json
  {
    "course_id": "课程ID"
  }
  ```
- **成功响应**:
  ```json
  {
    "success": true,
    "message": "退课成功"
  }
  ```

#### 获取已选课程

- **路径**: `/api/selection/list`
- **方法**: `GET`
- **参数**: 无
- **成功响应**:
  ```json
  {
    "success": true,
    "message": "获取已选课程成功",
    "data": [
      {
        "course_id": "C001",
        "name": "数据库原理",
        "credit": 3,
        "teacher_name": "张教授",
        "selection_time": "2023-01-01 12:00:00"
      }
    ]
  }
  ```

### 好友相关

#### 获取好友列表

- **路径**: `/api/friendship`
- **方法**: `GET`
- **参数**: 无
- **成功响应**:
  ```json
  {
    "success": true,
    "message": "获取好友和请求列表成功",
    "data": [
      {
        "student_id": "S2023002",
        "name": "李四",
        "status": "ACCEPTED"
      }
    ]
  }
  ```

#### 添加好友

- **路径**: `/api/friendship/add`
- **方法**: `POST`
- **参数**:
  ```json
  {
    "friendId": "学生ID"
  }
  ```
- **成功响应**:
  ```json
  {
    "success": true,
    "message": "好友请求已发送，等待对方确认"
  }
  ```

#### 接受好友请求

- **路径**: `/api/friendship/accept/{requesterId}`
- **方法**: `POST`
- **参数**: 路径参数 - requesterId
- **成功响应**:
  ```json
  {
    "success": true,
    "message": "已接受好友请求"
  }
  ```

#### 拒绝好友请求

- **路径**: `/api/friendship/reject/{requesterId}`
- **方法**: `POST`
- **参数**: 路径参数 - requesterId
- **成功响应**:
  ```json
  {
    "success": true,
    "message": "已拒绝好友请求"
  }
  ```

#### 删除好友

- **路径**: `/api/friendship/{friendId}`
- **方法**: `DELETE`
- **参数**: 路径参数 - friendId
- **成功响应**:
  ```json
  {
    "success": true,
    "message": "已删除好友"
  }
  ```

#### 搜索学生

- **路径**: `/api/friendship/search?keyword=关键词`
- **方法**: `GET`
- **参数**: 查询参数 - keyword
- **成功响应**:
  ```json
  {
    "success": true,
    "message": "搜索学生成功",
    "data": [
      {
        "student_id": "S2023002",
        "name": "李四",
        "dept_name": "计算机学院",
        "registration_date": "2023-01-01 12:00:00"
      }
    ]
  }
  ```

### 消息相关

#### 获取与特定好友的消息记录

- **路径**: `/api/message/conversation/{friendId}`
- **方法**: `GET`
- **参数**: 路径参数 - friendId
- **成功响应**:
  ```json
  {
    "success": true,
    "message": "获取对话成功",
    "data": [
      {
        "message_id": "M001",
        "from_student_id": "S2023001",
        "to_student_id": "S2023002",
        "content": "你好！",
        "send_time": "2023-01-01 12:00:00",
        "read": true,
        "from_student_name": "张三",
        "to_student_name": "李四"
      }
    ]
  }
  ```

#### 发送消息

- **路径**: `/api/message/send`
- **方法**: `POST`
- **参数**:
  ```json
  {
    "toId": "接收者学生ID",
    "content": "消息内容"
  }
  ```
- **成功响应**:
  ```json
  {
    "success": true,
    "message": "消息发送成功"
  }
  ```

### 交易相关

#### 获取交易记录

- **路径**: `/api/transactions/list`
- **方法**: `GET`
- **参数**: 无
- **成功响应**:
  ```json
  {
    "success": true,
    "message": "获取交易记录成功",
    "data": [
      {
        "transaction_id": "TRX001",
        "student_id": "S2023001",
        "type": "DEPOSIT",
        "amount": 100.00,
        "transaction_date": "2023-01-01 12:00:00",
        "description": "账户充值"
      }
    ]
  }
  ```

#### 充值

- **路径**: `/api/payment/deposit`
- **方法**: `POST`
- **参数**:
  ```json
  {
    "amount": 100
  }
  ```
- **成功响应**:
  ```json
  {
    "success": true,
    "message": "充值成功，已添加 100 元到账户"
  }
  ```

#### 提现

- **路径**: `/api/payment/withdraw`
- **方法**: `POST`
- **参数**:
  ```json
  {
    "amount": 50
  }
  ```
- **成功响应**:
  ```json
  {
    "success": true,
    "message": "提现成功，已从账户扣除 50 元"
  }
  ```

#### 转账

- **路径**: `/api/payment/transfer`
- **方法**: `POST`
- **参数**:
  ```json
  {
    "toStudentId": "接收者学生ID",
    "amount": 20,
    "description": "转账描述"
  }
  ```
- **成功响应**:
  ```json
  {
    "success": true,
    "message": "转账成功，已向 李四 转账 20 元"
  }
  ```

#### 支付

- **路径**: `/api/payment/pay`
- **方法**: `POST`
- **参数**:
  ```json
  {
    "courseId": "课程ID",
    "amount": 20,
    "description": "付款描述"
  }
  ```
- **成功响应**:
  ```json
  {
    "success": true,
    "message": "支付成功，已支付 20 元"
  }
  ```

## 错误代码

- **400** - 请求参数错误
- **401** - 未登录或会话过期
- **403** - 权限不足
- **404** - 请求的资源不存在
- **409** - 资源冲突
- **500** - 服务器内部错误

## API变更历史

- **2023-05-18**: 
  - 修复了`/api/transactions/`路径处理问题
  - 添加了`/api/payment/pay`支付API
  - 增加了转账预警功能
  - 统一了API字段命名约定 