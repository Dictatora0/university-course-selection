#!/bin/bash

# 颜色设置
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[0;33m'
NC='\033[0m' # 无颜色

# 设置API基础URL
BASE_URL="http://localhost:8081/course-selection/api"
COOKIE_JAR="cookies.txt"

# 辅助函数
print_header() {
    echo -e "\n${YELLOW}========================================"
    echo -e "    $1"
    echo -e "========================================${NC}\n"
}

print_test() {
    echo -e "\n${GREEN}[测试] $1${NC}"
}

print_response() {
    echo -e "${YELLOW}响应:${NC}"
    echo "$1" | python3 -m json.tool 2>/dev/null || echo "$1"
    echo ""
}

# 清除cookie文件
rm -f $COOKIE_JAR

# =============================================
# 0. 准备：查看加密的密码
# =============================================
print_header "0. 准备：查看加密的密码"

# 获取S2023000的密码和真实密码
print_test "查看学生的加密密码"
STUDENT_PASSWORD=$(mysql -u root -p12345678 -e "USE course_selection; SELECT student_id, password FROM Student WHERE student_id = 'S2023000';" --silent --skip-column-names 2>/dev/null)
print_response "$STUDENT_PASSWORD"

# =============================================
# 1. 管理员登录并创建新学生账号
# =============================================
print_header "1. 管理员登录并创建新学生账号"

# 1.1 管理员登录
print_test "管理员登录"
ADMIN_LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/admin/login" \
  -c $COOKIE_JAR \
  -H "Content-Type: application/json" \
  -d '{
    "adminId": "admin",
    "password": "adminpass"
  }')
print_response "$ADMIN_LOGIN_RESPONSE"

# 1.2 创建新学生账号
print_test "创建新学生账号"
# 生成随机学号以避免冲突
RANDOM_STUDENT_ID="S$(date +%s)"
CREATE_STUDENT_RESPONSE=$(curl -s -X POST "$BASE_URL/admin/students/create" \
  -b $COOKIE_JAR \
  -H "Content-Type: application/json" \
  -d '{
    "studentId": "'$RANDOM_STUDENT_ID'",
    "name": "测试学生",
    "password": "123456",
    "email": "test@example.com",
    "deptId": "DEPT1",
    "phone": "13800138000"
  }')
print_response "$CREATE_STUDENT_RESPONSE"

# 1.3 管理员登出
print_test "管理员登出"
ADMIN_LOGOUT_RESPONSE=$(curl -s -X GET "$BASE_URL/admin/logout" \
  -b $COOKIE_JAR)
print_response "$ADMIN_LOGOUT_RESPONSE"

# 清除cookie
rm -f $COOKIE_JAR

# =============================================
# 2. 学生账号测试
# =============================================
print_header "2. 学生账号测试"

# 2.1 使用已存在的学生账号登录
print_test "使用S2023000学生账号登录"
LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/students/login" \
  -c $COOKIE_JAR \
  -H "Content-Type: application/json" \
  -d '{
    "studentId": "S2023000",
    "password": "123456"
  }')
print_response "$LOGIN_RESPONSE"

# 2.2 获取学生信息
print_test "获取登录学生信息"
STUDENT_INFO_RESPONSE=$(curl -s -X GET "$BASE_URL/students/info" \
  -b $COOKIE_JAR)
print_response "$STUDENT_INFO_RESPONSE"

# 2.3 更新学生信息
print_test "更新学生信息"
UPDATE_INFO_RESPONSE=$(curl -s -X POST "$BASE_URL/students/updateInfo" \
  -b $COOKIE_JAR \
  -H "Content-Type: application/json" \
  -d '{
    "email": "updated@example.com",
    "phone": "13911112222"
  }')
print_response "$UPDATE_INFO_RESPONSE"

# =============================================
# 3. 课程系统API测试
# =============================================
print_header "3. 课程系统API测试"

# 3.1 获取所有课程
print_test "获取所有课程"
ALL_COURSES_RESPONSE=$(curl -s -X GET "$BASE_URL/course/list" \
  -b $COOKIE_JAR)
print_response "$ALL_COURSES_RESPONSE"

# 3.2 按院系获取课程
print_test "按院系获取课程"
DEPT_COURSES_RESPONSE=$(curl -s -X GET "$BASE_URL/course/getByDept?deptId=DEPT1" \
  -b $COOKIE_JAR)
print_response "$DEPT_COURSES_RESPONSE"

# 3.3 搜索课程
print_test "搜索课程"
SEARCH_COURSES_RESPONSE=$(curl -s -X GET "$BASE_URL/course/search?keyword=%E4%BF%A1%E6%81%AF" \
  -b $COOKIE_JAR)
print_response "$SEARCH_COURSES_RESPONSE"

# =============================================
# 4. 选课系统API测试
# =============================================
print_header "4. 选课系统API测试"

# 4.1 选课
print_test "选课"
ENROLL_RESPONSE=$(curl -s -X POST "$BASE_URL/enrollment/add" \
  -b $COOKIE_JAR \
  -H "Content-Type: application/json" \
  -d '{
    "courseId": "1"
  }')
print_response "$ENROLL_RESPONSE"

# 4.2 获取我的课程
print_test "获取我的课程"
MY_COURSES_RESPONSE=$(curl -s -X GET "$BASE_URL/enrollment/list" \
  -b $COOKIE_JAR)
print_response "$MY_COURSES_RESPONSE"

# 4.3 退课
print_test "退课"
DROP_RESPONSE=$(curl -s -X GET "$BASE_URL/enrollment/drop?courseId=1" \
  -b $COOKIE_JAR)
print_response "$DROP_RESPONSE"

# 4.4 再次获取我的课程（确认退课成功）
print_test "再次获取我的课程（确认退课成功）"
MY_COURSES_AFTER_DROP_RESPONSE=$(curl -s -X GET "$BASE_URL/enrollment/list" \
  -b $COOKIE_JAR)
print_response "$MY_COURSES_AFTER_DROP_RESPONSE"

# =============================================
# 5. 财务功能API测试
# =============================================
print_header "5. 财务功能API测试"

# 5.1 查询余额
print_test "查询余额"
BALANCE_RESPONSE=$(curl -s -X GET "$BASE_URL/payment/balance" \
  -b $COOKIE_JAR)
print_response "$BALANCE_RESPONSE"

# 5.2 充值
print_test "账户充值"
DEPOSIT_RESPONSE=$(curl -s -X POST "$BASE_URL/payment/deposit" \
  -b $COOKIE_JAR \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 1000.00
  }')
print_response "$DEPOSIT_RESPONSE"

# 5.3 转账
print_test "转账给其他学生"
TRANSFER_RESPONSE=$(curl -s -X POST "$BASE_URL/payment/transfer" \
  -b $COOKIE_JAR \
  -H "Content-Type: application/json" \
  -d '{
    "toStudentId": "S2023001",
    "amount": 100.00,
    "description": "测试转账"
  }')
print_response "$TRANSFER_RESPONSE"

# 5.4 查询我的交易记录
print_test "查询我的交易记录"
TRANSACTIONS_RESPONSE=$(curl -s -X GET "$BASE_URL/payment/transactions" \
  -b $COOKIE_JAR)
print_response "$TRANSACTIONS_RESPONSE"

# =============================================
# 6. 社交功能API测试
# =============================================
print_header "6. 社交功能API测试"

# 6.1 发送好友请求
print_test "发送好友请求"
ADD_FRIEND_RESPONSE=$(curl -s -X POST "$BASE_URL/friendship/sendRequest" \
  -b $COOKIE_JAR \
  -H "Content-Type: application/json" \
  -d '{
    "friendId": "S2023001"
  }')
print_response "$ADD_FRIEND_RESPONSE"

# 6.2 获取我发送的好友请求
print_test "获取我发送的好友请求"
MY_FRIEND_REQUESTS_RESPONSE=$(curl -s -X GET "$BASE_URL/friendship/sent" \
  -b $COOKIE_JAR)
print_response "$MY_FRIEND_REQUESTS_RESPONSE"

# 6.3 获取我收到的好友请求
print_test "获取我收到的好友请求"
RECEIVED_FRIEND_REQUESTS_RESPONSE=$(curl -s -X GET "$BASE_URL/friendship/received" \
  -b $COOKIE_JAR)
print_response "$RECEIVED_FRIEND_REQUESTS_RESPONSE"

# 6.4 获取我的好友列表
print_test "获取我的好友列表"
MY_FRIENDS_RESPONSE=$(curl -s -X GET "$BASE_URL/friendship/list" \
  -b $COOKIE_JAR)
print_response "$MY_FRIENDS_RESPONSE"

# 6.5 发送消息
print_test "发送消息"
SEND_MESSAGE_RESPONSE=$(curl -s -X POST "$BASE_URL/message/send" \
  -b $COOKIE_JAR \
  -H "Content-Type: application/json" \
  -d '{
    "receiverId": "S2023001",
    "content": "你好，这是一条测试消息！"
  }')
print_response "$SEND_MESSAGE_RESPONSE"

# 6.6 获取我的消息会话
print_test "获取我的消息会话"
MY_CONVERSATIONS_RESPONSE=$(curl -s -X GET "$BASE_URL/message/list" \
  -b $COOKIE_JAR)
print_response "$MY_CONVERSATIONS_RESPONSE"

# =============================================
# 7. 管理员功能API测试 - 完整测试
# =============================================
print_header "7. 管理员功能API测试 - 完整测试"

# 清除学生cookie
rm -f $COOKIE_JAR

# 7.1 管理员登录
print_test "管理员登录"
ADMIN_LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/admin/login" \
  -c $COOKIE_JAR \
  -H "Content-Type: application/json" \
  -d '{
    "adminId": "admin",
    "password": "adminpass"
  }')
print_response "$ADMIN_LOGIN_RESPONSE"

# 7.2 获取所有学生
print_test "获取所有学生"
ALL_STUDENTS_RESPONSE=$(curl -s -X GET "$BASE_URL/admin/students" \
  -b $COOKIE_JAR)
print_response "$ALL_STUDENTS_RESPONSE"

# 7.3 添加新课程
print_test "添加新课程"
RANDOM_COURSE_ID="CS$(date +%s)"
ADD_COURSE_RESPONSE=$(curl -s -X POST "$BASE_URL/course/add" \
  -b $COOKIE_JAR \
  -H "Content-Type: application/json" \
  -d '{
    "courseId": "'$RANDOM_COURSE_ID'",
    "courseName": "测试课程",
    "deptId": "DEPT1",
    "credit": 3.0,
    "description": "这是一个测试课程",
    "capacity": 100
  }')
print_response "$ADD_COURSE_RESPONSE"

# 7.4 获取统计数据
print_test "获取系统统计数据"
STATS_RESPONSE=$(curl -s -X GET "$BASE_URL/admin/stats/all" \
  -b $COOKIE_JAR)
print_response "$STATS_RESPONSE"

# 7.5 管理员登出
print_test "管理员登出"
ADMIN_LOGOUT_RESPONSE=$(curl -s -X GET "$BASE_URL/admin/logout" \
  -b $COOKIE_JAR)
print_response "$ADMIN_LOGOUT_RESPONSE"

# 清理
rm -f $COOKIE_JAR
print_header "API测试完成"