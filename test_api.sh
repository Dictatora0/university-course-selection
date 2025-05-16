#!/bin/bash

# 颜色输出函数
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[0;33m'
NC='\033[0m' # No Color

print_success() {
    echo -e "${GREEN}[成功]${NC} $1"
}

print_error() {
    echo -e "${RED}[失败]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[警告]${NC} $1"
}

print_header() {
    echo -e "\n${YELLOW}========== $1 ==========${NC}"
}

# 获取部署端口
if [ -f .port_config ]; then
    source .port_config
    HTTP_PORT=${HTTP_PORT:-8081}
else
    HTTP_PORT=8081
fi

# API基础URL
BASE_URL="http://localhost:${HTTP_PORT}/course-selection/api"

# 存储会话Cookie
COOKIE_JAR="cookies.txt"
rm -f $COOKIE_JAR

# 测试结果计数
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# 测试函数
test_api() {
    METHOD=$1
    ENDPOINT=$2
    DATA=$3
    EXPECTED_STATUS=$4
    DESCRIPTION=$5
    
    TOTAL_TESTS=$((TOTAL_TESTS+1))
    
    echo -e "${YELLOW}测试: $DESCRIPTION${NC}"
    echo "请求: $METHOD $ENDPOINT"
    if [ ! -z "$DATA" ]; then
        echo "数据: $DATA"
    fi
    
    # 构建curl命令
    CURL_CMD="curl -s -X $METHOD -w '\nStatus: %{http_code}' -b $COOKIE_JAR -c $COOKIE_JAR"
    
    if [ "$METHOD" == "POST" ] || [ "$METHOD" == "PUT" ]; then
        CURL_CMD="$CURL_CMD -H 'Content-Type: application/json'"
        if [ ! -z "$DATA" ]; then
            CURL_CMD="$CURL_CMD -d '$DATA'"
        fi
    fi
    
    CURL_CMD="$CURL_CMD $BASE_URL$ENDPOINT"
    
    # 执行请求
    RESPONSE=$(eval $CURL_CMD)
    
    # 提取状态码
    STATUS_CODE=$(echo "$RESPONSE" | grep -oE 'Status: [0-9]+' | cut -d' ' -f2)
    
    # 提取响应体 (去掉状态行)
    BODY=$(echo "$RESPONSE" | sed '$d')
    
    echo "响应状态: $STATUS_CODE (预期: $EXPECTED_STATUS)"
    echo "响应正文: $BODY"
    
    # 验证状态码
    if [ "$STATUS_CODE" == "$EXPECTED_STATUS" ]; then
        print_success "状态码符合预期"
        PASSED_TESTS=$((PASSED_TESTS+1))
    else
        print_error "状态码不符合预期"
        FAILED_TESTS=$((FAILED_TESTS+1))
    fi
    
    # 简单验证响应格式
    if echo "$BODY" | grep -q '"success":'; then
        print_success "响应格式正确"
    else
        print_error "响应格式不正确"
    fi
    
    echo "----------------------------------------"
    
    # 返回响应内容，供后续测试使用
    echo "$BODY"
}

# 1. 测试学生相关API
print_header "学生相关API"

# 注册 - 生成一个随机学号以确保测试成功
STUDENT_ID="test$(date +%s)"
test_api "POST" "/student/register" "{\"studentId\":\"$STUDENT_ID\",\"name\":\"测试用户\",\"password\":\"123456\"}" "200" "注册新用户"

# 使用错误的登录信息
test_api "POST" "/student/login" "{\"studentId\":\"$STUDENT_ID\",\"password\":\"wrong_password\"}" "400" "使用错误密码登录"

# 正确登录
LOGIN_RESP=$(test_api "POST" "/student/login" "{\"studentId\":\"$STUDENT_ID\",\"password\":\"123456\"}" "200" "使用正确密码登录")

# 获取学生信息
test_api "GET" "/student/getInfo" "" "200" "获取登录用户信息"

# 2. 测试课程相关API
print_header "课程相关API"

# 导入课程
test_api "POST" "/student/importCourses" "" "200" "导入课程"

# 获取课程列表
test_api "GET" "/course/list" "" "200" "获取课程列表"

# 3. 测试选课相关API
print_header "选课相关API"

# 假设有一个课程ID
COURSE_ID="C001"
test_api "POST" "/enrollment/add" "{\"courseId\":\"$COURSE_ID\"}" "200" "选修课程"

# 获取已选课程
test_api "GET" "/enrollment/list" "" "200" "获取已选课程列表"

# 退选课程
test_api "POST" "/enrollment/drop?courseId=$COURSE_ID" "" "200" "退选课程"

# 4. 测试好友相关API
print_header "好友相关API"

# 获取好友列表
test_api "GET" "/friendship/list" "" "400" "获取好友列表（应该返回未登录错误）"

# 获取好友数量
test_api "GET" "/friendship/count" "" "400" "获取好友数量（应该返回未登录错误）"

# 添加好友 (假设有一个用户ID)
test_api "POST" "/friendship/add" "{\"friendId\":\"friend123\"}" "400" "添加不存在的好友"

# 5. 测试消息相关API
print_header "消息相关API"

# 获取未读消息
test_api "GET" "/messages/unread" "" "400" "获取未读消息（应该返回未登录错误）"

# 获取未读消息数量
test_api "GET" "/messages/unread_count" "" "400" "获取未读消息数量（应该返回未登录错误）"

# 6. 测试支付相关API
print_header "支付相关API"

# 获取余额
test_api "GET" "/student/getInfo" "" "200" "获取用户余额"

# 充值
test_api "POST" "/payment/deposit" "{\"amount\":100}" "401" "用户充值（应该返回未登录错误）"

# 提现
test_api "POST" "/payment/withdraw" "{\"amount\":50}" "401" "用户提现（应该返回未登录错误）"

# 7. 测试交易相关API
print_header "交易相关API"

# 获取交易记录
test_api "GET" "/transactions/list" "" "400" "获取交易记录（应该返回未登录错误）"

# 登出
print_header "登出测试"
test_api "GET" "/student/logout" "" "200" "用户登出"

# 打印测试结果摘要
print_header "测试结果摘要"
echo "总测试数: $TOTAL_TESTS"
echo "通过测试: $PASSED_TESTS"
echo "失败测试: $FAILED_TESTS"

# 清理
rm -f $COOKIE_JAR 