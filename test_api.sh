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
    HTTP_PORT=${HTTP_PORT:-8081} # Default to 8081 if not set in .port_config
else
    HTTP_PORT=8081 # Default if .port_config doesn't exist
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
    
    # 简单验证响应格式 (如果状态码是2xx)
    if [[ "$STATUS_CODE" =~ ^2[0-9]{2}$ ]]; then
        if echo "$BODY" | grep -q '"success":true'; then
            print_success "响应格式正确 (success:true)"
        else
            print_warning "响应格式可能不正确 (success:true 未找到)"
        fi
    elif [[ "$STATUS_CODE" =~ ^[45][0-9]{2}$ ]]; then # 4xx or 5xx errors
        if echo "$BODY" | grep -q '"success":false'; then
            print_success "错误响应格式正确 (success:false)"
        else
            print_warning "错误响应格式可能不正确 (success:false 未找到)"
        fi
    fi
    
    echo "----------------------------------------"
    
    # 返回响应内容，供后续测试使用
    echo "$BODY"
}

# 0. 准备工作 - 注册两个测试用户
print_header "准备测试用户"
USER1_ID="testuser1_$(date +%s)"
USER2_ID="testuser2_$(date +%s)"

test_api "POST" "/student/register" "{\"studentId\":\"$USER1_ID\",\"name\":\"测试用户1\",\"password\":\"123456\"}" "200" "注册测试用户1"
test_api "POST" "/student/register" "{\"studentId\":\"$USER2_ID\",\"name\":\"测试用户2\",\"password\":\"123456\"}" "200" "注册测试用户2"


# 1. 测试学生相关API
print_header "学生相关API"
# 清空cookie，确保从干净状态开始
rm -f $COOKIE_JAR

# 使用错误密码登录 (用户1)
test_api "POST" "/student/login" "{\"studentId\":\"$USER1_ID\",\"password\":\"wrongpassword\"}" "400" "用户1: 使用错误密码登录"

# 正确登录 (用户1)
LOGIN_RESP_USER1=$(test_api "POST" "/student/login" "{\"studentId\":\"$USER1_ID\",\"password\":\"123456\"}" "200" "用户1: 使用正确密码登录")

# 获取学生信息 (用户1)
test_api "GET" "/student/getInfo" "" "200" "用户1: 获取登录用户信息"

# 2. 测试课程相关API (用户1已登录)
print_header "课程相关API"

# 导入课程
test_api "POST" "/student/importCourses" "" "200" "用户1: 导入课程"

# 获取课程列表
test_api "GET" "/course/list" "" "200" "用户1: 获取课程列表"

# 3. 测试选课相关API (用户1已登录)
print_header "选课相关API"

# 假设有一个课程ID
COURSE_ID="C001"
test_api "POST" "/enrollment/add" "{\"courseId\":\"$COURSE_ID\"}" "200" "用户1: 选修课程 C001"

# 获取已选课程
test_api "GET" "/enrollment/list" "" "200" "用户1: 获取已选课程列表"

# 退选课程
test_api "POST" "/enrollment/drop?courseId=$COURSE_ID" "" "200" "用户1: 退选课程 C001"


# 4. 测试支付相关API (用户1已登录)
print_header "支付相关API"

# 用户1充值
test_api "POST" "/payment/deposit" "{\"amount\":100}" "200" "用户1: 充值100元"

# 用户1获取余额，检查是否为100
USER1_INFO_AFTER_DEPOSIT=$(test_api "GET" "/student/getInfo" "" "200" "用户1: 获取充值后用户信息")
BALANCE_USER1=$(echo "$USER1_INFO_AFTER_DEPOSIT" | grep -oE '\"balance\":[0-9]+(\.[0-9]+)?' | cut -d':' -f2)
if [ "$(echo "$BALANCE_USER1 == 100.0" | bc -l)" -eq 1 ]; then
    print_success "用户1余额正确 (100.0)"
else
    print_error "用户1余额不正确, 实际为: $BALANCE_USER1"
    FAILED_TESTS=$((FAILED_TESTS+1)) # 额外标记失败
fi

# 用户1提现50
test_api "POST" "/payment/withdraw" "{\"amount\":50}" "200" "用户1: 提现50元"

# 用户1获取余额，检查是否为50
USER1_INFO_AFTER_WITHDRAW=$(test_api "GET" "/student/getInfo" "" "200" "用户1: 获取提现后用户信息")
BALANCE_USER1_AFTER_WITHDRAW=$(echo "$USER1_INFO_AFTER_WITHDRAW" | grep -oE '\"balance\":[0-9]+(\.[0-9]+)?' | cut -d':' -f2)
if [ "$(echo "$BALANCE_USER1_AFTER_WITHDRAW == 50.0" | bc -l)" -eq 1 ]; then
    print_success "用户1余额正确 (50.0)"
else
    print_error "用户1余额不正确, 实际为: $BALANCE_USER1_AFTER_WITHDRAW"
    FAILED_TESTS=$((FAILED_TESTS+1)) # 额外标记失败
fi

# 用户1提现100 (余额不足)
test_api "POST" "/payment/withdraw" "{\"amount\":100}" "400" "用户1: 提现100元 (余额不足)"


# 5. 测试交易相关API (用户1已登录)
print_header "交易相关API"

# 获取用户1的交易记录 (应包含充值和提现)
test_api "GET" "/transactions/list" "" "200" "用户1: 获取交易记录"


# 6. 测试好友相关API (用户1已登录)
print_header "好友相关API"

# 用户1添加用户2为好友
test_api "POST" "/friendship/add" "{\"friendId\":\"$USER2_ID\"}" "200" "用户1: 添加用户2为好友"

# 用户1获取好友列表 (应包含用户2)
test_api "GET" "/friendship/list" "" "200" "用户1: 获取好友列表"

# 用户1检查与用户2的好友关系
test_api "GET" "/friendship/check/$USER2_ID" "" "200" "用户1: 检查与用户2的好友关系"

# 用户1获取好友数量 (应为1)
FRIEND_COUNT_RESP_USER1=$(test_api "GET" "/friendship/count" "" "200" "用户1: 获取好友数量")
FRIEND_COUNT_USER1=$(echo "$FRIEND_COUNT_RESP_USER1" | grep -oE '\"count\":[0-9]+' | cut -d':' -f2)
if [ "$FRIEND_COUNT_USER1" == "1" ]; then
    print_success "用户1好友数量正确 (1)"
else
    print_error "用户1好友数量不正确, 实际为: $FRIEND_COUNT_USER1"
    FAILED_TESTS=$((FAILED_TESTS+1))
fi

# 用户1尝试添加一个不存在的好友
test_api "POST" "/friendship/add" "{\"friendId\":\"nonexistentuser999\"}" "404" "用户1: 添加不存在的好友"

# 用户1登出
print_header "用户1登出"
test_api "GET" "/student/logout" "" "200" "用户1: 登出"

# 清空cookie
rm -f $COOKIE_JAR

# 用户2登录
print_header "用户2登录"
LOGIN_RESP_USER2=$(test_api "POST" "/student/login" "{\"studentId\":\"$USER2_ID\",\"password\":\"123456\"}" "200" "用户2: 使用正确密码登录")

# 用户2获取好友列表 (用户1添加了用户2，所以用户2的好友列表也应包含用户1)
test_api "GET" "/friendship/list" "" "200" "用户2: 获取好友列表 (应包含用户1)"


# 7. 测试消息相关API
print_header "消息相关API"
# 用户1重新登录，以便发送消息
print_header "用户1重新登录"
rm -f $COOKIE_JAR
test_api "POST" "/student/login" "{\"studentId\":\"$USER1_ID\",\"password\":\"123456\"}" "200" "用户1: 重新登录"

# 用户1发送消息给用户2
MSG_CONTENT="你好，$USER2_ID！来自$USER1_ID的问候。"
test_api "POST" "/messages/send" "{\"receiverId\":\"$USER2_ID\", \"content\":\"$MSG_CONTENT\"}" "200" "用户1: 发送消息给用户2"

# 用户1获取与用户2的对话
test_api "GET" "/messages/conversation/$USER2_ID" "" "200" "用户1: 获取与用户2的对话"

# 用户1登出
test_api "GET" "/student/logout" "" "200" "用户1: 再次登出"

# 用户2登录以检查消息
rm -f $COOKIE_JAR
test_api "POST" "/student/login" "{\"studentId\":\"$USER2_ID\",\"password\":\"123456\"}" "200" "用户2: 再次登录"

# 用户2获取未读消息 (应包含用户1发送的消息)
UNREAD_MSG_RESP_USER2=$(test_api "GET" "/messages/unread" "" "200" "用户2: 获取未读消息")
# 假设返回的消息ID是1 (简单起见，实际应用中需要解析JSON获取ID)
# 提取消息ID，假设返回的JSON数组中第一个消息的messageId
# 注意：这个提取方式非常脆弱，仅用于简单演示
MESSAGE_ID_USER2=$(echo "$UNREAD_MSG_RESP_USER2" | grep -oE '\"messageId\":[0-9]+' | head -1 | cut -d':' -f2)

if [ -z "$MESSAGE_ID_USER2" ]; then
    print_warning "无法从用户2的未读消息中提取MESSAGE_ID，后续标记已读和删除测试可能失败"
else
    print_success "用户2收到消息，消息ID: $MESSAGE_ID_USER2"
fi


# 用户2获取未读消息数量 (应为1)
UNREAD_COUNT_RESP_USER2=$(test_api "GET" "/messages/unread_count" "" "200" "用户2: 获取未读消息数量")
UNREAD_COUNT_USER2=$(echo "$UNREAD_COUNT_RESP_USER2" | grep -oE '\"count\":[0-9]+' | cut -d':' -f2)
if [ "$UNREAD_COUNT_USER2" == "1" ]; then
    print_success "用户2未读消息数量正确 (1)"
else
    print_error "用户2未读消息数量不正确, 实际为: $UNREAD_COUNT_USER2"
    FAILED_TESTS=$((FAILED_TESTS+1))
fi


# 用户2标记消息为已读 (使用上面获取的MESSAGE_ID)
if [ ! -z "$MESSAGE_ID_USER2" ]; then
    test_api "PUT" "/messages/read/$MESSAGE_ID_USER2" "" "200" "用户2: 标记消息 $MESSAGE_ID_USER2 为已读"

    # 用户2获取未读消息数量 (应为0)
    UNREAD_COUNT_RESP_AFTER_READ_USER2=$(test_api "GET" "/messages/unread_count" "" "200" "用户2: 再次获取未读消息数量")
    UNREAD_COUNT_AFTER_READ_USER2=$(echo "$UNREAD_COUNT_RESP_AFTER_READ_USER2" | grep -oE '\"count\":[0-9]+' | cut -d':' -f2)
    if [ "$UNREAD_COUNT_AFTER_READ_USER2" == "0" ]; then
        print_success "用户2未读消息数量正确 (0)"
    else
        print_error "用户2未读消息数量不正确 (标记已读后), 实际为: $UNREAD_COUNT_AFTER_READ_USER2"
        FAILED_TESTS=$((FAILED_TESTS+1))
    fi

    # 用户2删除消息
    test_api "DELETE" "/messages/delete/$MESSAGE_ID_USER2" "" "200" "用户2: 删除消息 $MESSAGE_ID_USER2"
else
    print_warning "跳过用户2标记已读和删除消息测试，因为无法获取消息ID"
fi


# 用户2删除与用户1的好友关系
test_api "DELETE" "/friendship/delete/$USER1_ID" "" "200" "用户2: 删除与用户1的好友关系"

# 8. 最终登出
print_header "最终登出"
# 用户2登出
test_api "GET" "/student/logout" "" "200" "用户2: 登出"


# 打印测试结果摘要
print_header "测试结果摘要"
echo "总测试数: $TOTAL_TESTS"
echo "通过测试: $PASSED_TESTS"
echo "失败测试: $FAILED_TESTS"

# 清理
rm -f $COOKIE_JAR 