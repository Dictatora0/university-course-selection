#!/bin/bash

# 颜色设置
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[0;33m'
NC='\033[0m'

# 输出函数
print_success() {
    echo -e "${GREEN}[成功]${NC} $1"
}

print_error() {
    echo -e "${RED}[失败]${NC} $1"
}

print_header() {
    echo -e "\n${YELLOW}========== $1 ==========${NC}"
}

# 基础URL
BASE_URL="http://localhost:8081/course-selection/api"

# 保存cookies的文件
COOKIE_JAR="test_cookies.txt"

# 清理旧的cookie文件
rm -f $COOKIE_JAR

# 测试用户ID（使用时间戳确保唯一性）
TIMESTAMP=$(date +%s)
USER1_ID="testuser1_$TIMESTAMP"
USER2_ID="testuser2_$TIMESTAMP"
USER3_ID="testuser3_$TIMESTAMP"

print_header "准备测试用户"
# 1. 注册测试用户1
echo "测试: 注册测试用户1"
RESP=$(curl -s -X POST -H "Content-Type: application/json" \
  -d "{\"studentId\":\"$USER1_ID\",\"name\":\"测试用户1\",\"password\":\"123456\"}" \
  -c $COOKIE_JAR -b $COOKIE_JAR \
  "$BASE_URL/student/register")
echo "$RESP"
if echo "$RESP" | grep -q '"success":true'; then
    print_success "测试用户1注册成功"
else
    print_error "测试用户1注册失败"
fi

# 2. 注册测试用户2
echo "测试: 注册测试用户2"
RESP=$(curl -s -X POST -H "Content-Type: application/json" \
  -d "{\"studentId\":\"$USER2_ID\",\"name\":\"测试用户2\",\"password\":\"123456\"}" \
  -c $COOKIE_JAR -b $COOKIE_JAR \
  "$BASE_URL/student/register")
echo "$RESP"
if echo "$RESP" | grep -q '"success":true'; then
    print_success "测试用户2注册成功"
else
    print_error "测试用户2注册失败"
fi

# 3. 注册测试用户3
echo "测试: 注册测试用户3"
RESP=$(curl -s -X POST -H "Content-Type: application/json" \
  -d "{\"studentId\":\"$USER3_ID\",\"name\":\"测试用户3\",\"password\":\"123456\"}" \
  -c $COOKIE_JAR -b $COOKIE_JAR \
  "$BASE_URL/student/register")
echo "$RESP"
if echo "$RESP" | grep -q '"success":true'; then
    print_success "测试用户3注册成功"
else
    print_error "测试用户3注册失败"
fi

print_header "测试好友关系和消息发送"
# 4. 用户1登录
echo "测试: 用户1登录"
RESP=$(curl -s -X POST -H "Content-Type: application/json" \
  -d "{\"studentId\":\"$USER1_ID\",\"password\":\"123456\"}" \
  -c $COOKIE_JAR -b $COOKIE_JAR \
  "$BASE_URL/student/login")
echo "$RESP"
if echo "$RESP" | grep -q '"success":true'; then
    print_success "用户1登录成功"
else
    print_error "用户1登录失败"
fi

# 5. 尝试向非好友（用户2）发送消息
echo "测试: 尝试向非好友发送消息"
RESP=$(curl -s -X POST -H "Content-Type: application/json" \
  -d "{\"receiverId\":\"$USER2_ID\", \"content\":\"这条消息应该发送失败，因为我们不是好友\"}" \
  -c $COOKIE_JAR -b $COOKIE_JAR \
  "$BASE_URL/messages/send")
echo "$RESP"
if echo "$RESP" | grep -q '"success":false'; then
    print_success "向非好友发送消息被正确拒绝"
else
    print_error "向非好友发送消息未被拒绝"
fi

# 6. 添加好友关系（用户1添加用户2为好友）
echo "测试: 用户1添加用户2为好友"
RESP=$(curl -s -X POST -H "Content-Type: application/json" \
  -d "{\"friendId\":\"$USER2_ID\"}" \
  -c $COOKIE_JAR -b $COOKIE_JAR \
  "$BASE_URL/friendship/add")
echo "$RESP"
if echo "$RESP" | grep -q '"success":true'; then
    print_success "用户1成功添加用户2为好友"
else
    print_error "用户1添加用户2为好友失败"
fi

# 7. 向好友（用户2）发送消息
echo "测试: 向好友发送英文消息"
RESP=$(curl -s -X POST -H "Content-Type: application/json" \
  -d "{\"receiverId\":\"$USER2_ID\", \"content\":\"Hello, this is a test message.\"}" \
  -c $COOKIE_JAR -b $COOKIE_JAR \
  "$BASE_URL/messages/send")
echo "$RESP"
if echo "$RESP" | grep -q '"success":true'; then
    print_success "向好友发送英文消息成功"
else
    print_error "向好友发送英文消息失败"
fi

# 8. 向好友（用户2）发送中文消息
echo "测试: 向好友发送中文消息"
RESP=$(curl -s -X POST -H "Content-Type: application/json" \
  -d "{\"receiverId\":\"$USER2_ID\", \"content\":\"你好，这是一条中文测试消息。\"}" \
  -c $COOKIE_JAR -b $COOKIE_JAR \
  "$BASE_URL/messages/send")
echo "$RESP"
if echo "$RESP" | grep -q '"success":true'; then
    print_success "向好友发送中文消息成功"
else
    print_error "向好友发送中文消息失败"
fi

# 9. 获取与好友的会话
echo "测试: 获取与好友的会话"
RESP=$(curl -s -X GET -H "Content-Type: application/json" \
  -c $COOKIE_JAR -b $COOKIE_JAR \
  "$BASE_URL/messages/conversation/$USER2_ID")
echo "$RESP"
if echo "$RESP" | grep -q '"success":true'; then
    print_success "获取与好友的会话成功"
else
    print_error "获取与好友的会话失败"
fi

# 10. 尝试获取与非好友（用户3）的会话
echo "测试: 尝试获取与非好友的会话"
RESP=$(curl -s -X GET -H "Content-Type: application/json" \
  -c $COOKIE_JAR -b $COOKIE_JAR \
  "$BASE_URL/messages/conversation/$USER3_ID")
echo "$RESP"
if echo "$RESP" | grep -q '"success":false'; then
    print_success "获取与非好友的会话被正确拒绝"
else
    print_error "获取与非好友的会话未被拒绝"
fi

# 11. 用户1登出
echo "测试: 用户1登出"
RESP=$(curl -s -X GET -H "Content-Type: application/json" \
  -c $COOKIE_JAR -b $COOKIE_JAR \
  "$BASE_URL/student/logout")
echo "$RESP"
if echo "$RESP" | grep -q '"success":true'; then
    print_success "用户1登出成功"
else
    print_error "用户1登出失败"
fi

# 12. 用户2登录
echo "测试: 用户2登录"
RESP=$(curl -s -X POST -H "Content-Type: application/json" \
  -d "{\"studentId\":\"$USER2_ID\",\"password\":\"123456\"}" \
  -c $COOKIE_JAR -b $COOKIE_JAR \
  "$BASE_URL/student/login")
echo "$RESP"
if echo "$RESP" | grep -q '"success":true'; then
    print_success "用户2登录成功"
else
    print_error "用户2登录失败"
fi

# 13. 获取未读消息
echo "测试: 获取未读消息"
RESP=$(curl -s -X GET -H "Content-Type: application/json" \
  -c $COOKIE_JAR -b $COOKIE_JAR \
  "$BASE_URL/messages/unread")
echo "$RESP"
if echo "$RESP" | grep -q '"success":true'; then
    print_success "获取未读消息成功"
else
    print_error "获取未读消息失败"
fi

# 14. 用户2登出
echo "测试: 用户2登出"
RESP=$(curl -s -X GET -H "Content-Type: application/json" \
  -c $COOKIE_JAR -b $COOKIE_JAR \
  "$BASE_URL/student/logout")
echo "$RESP"
if echo "$RESP" | grep -q '"success":true'; then
    print_success "用户2登出成功"
else
    print_error "用户2登出失败"
fi

print_header "测试充值提现功能"
# 15. 用户1登录
echo "测试: 用户1重新登录"
RESP=$(curl -s -X POST -H "Content-Type: application/json" \
  -d "{\"studentId\":\"$USER1_ID\",\"password\":\"123456\"}" \
  -c $COOKIE_JAR -b $COOKIE_JAR \
  "$BASE_URL/student/login")
echo "$RESP"
if echo "$RESP" | grep -q '"success":true'; then
    print_success "用户1重新登录成功"
else
    print_error "用户1重新登录失败"
fi

# 16. 充值
echo "测试: 充值100元"
RESP=$(curl -s -X POST -H "Content-Type: application/json" \
  -d "{\"amount\":100}" \
  -c $COOKIE_JAR -b $COOKIE_JAR \
  "$BASE_URL/payment/deposit")
echo "$RESP"
if echo "$RESP" | grep -q '"success":true'; then
    print_success "充值100元成功"
else
    print_error "充值100元失败"
fi

# 17. 获取用户信息，查看余额
echo "测试: 查看充值后余额"
RESP=$(curl -s -X GET -H "Content-Type: application/json" \
  -c $COOKIE_JAR -b $COOKIE_JAR \
  "$BASE_URL/student/getInfo")
echo "$RESP"
BALANCE=$(echo "$RESP" | grep -o '"balance":[0-9.]*' | cut -d':' -f2)
if [ "$BALANCE" = "100.0" ]; then
    print_success "充值后余额正确：$BALANCE"
else
    print_error "充值后余额不正确：$BALANCE"
fi

# 18. 提现50元
echo "测试: 提现50元"
RESP=$(curl -s -X POST -H "Content-Type: application/json" \
  -d "{\"amount\":50}" \
  -c $COOKIE_JAR -b $COOKIE_JAR \
  "$BASE_URL/payment/withdraw")
echo "$RESP"
if echo "$RESP" | grep -q '"success":true'; then
    print_success "提现50元成功"
else
    print_error "提现50元失败"
fi

# 19. 获取用户信息，查看余额
echo "测试: 查看提现后余额"
RESP=$(curl -s -X GET -H "Content-Type: application/json" \
  -c $COOKIE_JAR -b $COOKIE_JAR \
  "$BASE_URL/student/getInfo")
echo "$RESP"
BALANCE=$(echo "$RESP" | grep -o '"balance":[0-9.]*' | cut -d':' -f2)
if [ "$BALANCE" = "50.0" ]; then
    print_success "提现后余额正确：$BALANCE"
else
    print_error "提现后余额不正确：$BALANCE"
fi

# 20. 用户1登出
echo "测试: 用户1最后登出"
RESP=$(curl -s -X GET -H "Content-Type: application/json" \
  -c $COOKIE_JAR -b $COOKIE_JAR \
  "$BASE_URL/student/logout")
echo "$RESP"
if echo "$RESP" | grep -q '"success":true'; then
    print_success "用户1最后登出成功"
else
    print_error "用户1最后登出失败"
fi

print_header "测试完成"
echo "测试用户1: $USER1_ID"
echo "测试用户2: $USER2_ID"
echo "测试用户3: $USER3_ID"

# 清理临时文件
rm -f $COOKIE_JAR 