#!/bin/bash

# 输出彩色文本的函数
green() {
    echo -e "\033[32m$1\033[0m"
}

red() {
    echo -e "\033[31m$1\033[0m"
}

yellow() {
    echo -e "\033[33m$1\033[0m"
}

# 配置Tomcat路径和初始端口
TOMCAT_HOME="/opt/homebrew/opt/tomcat@9/libexec"
APP_NAME="course-selection"
DEFAULT_HTTP_PORT=8081
DEFAULT_SHUTDOWN_PORT=8006
HTTP_PORT_RANGE_START=8081
HTTP_PORT_RANGE_END=8100
SHUTDOWN_PORT_RANGE_START=8001
SHUTDOWN_PORT_RANGE_END=8050

# 改进：更详细地显示被占用端口的情况
show_port_usage() {
    local PORT=$1
    local PIDS=$(lsof -ti tcp:$PORT)
    if [ -n "$PIDS" ]; then
        yellow "端口 $PORT 被以下进程占用:"
        for PID in $PIDS; do
            local CMD=$(ps -p $PID -o comm=)
            local USER=$(ps -p $PID -o user=)
            yellow "  PID: $PID, 命令: $CMD, 用户: $USER"
        done
    else
        green "端口 $PORT 当前未被占用"
    fi
}

# 改进：查找可用端口对 (HTTP端口和Shutdown端口)
find_available_port_pair() {
    for ((http_port=HTTP_PORT_RANGE_START; http_port<=HTTP_PORT_RANGE_END; http_port++)); do
        if ! lsof -i tcp:$http_port > /dev/null; then
            # 默认的shutdown端口偏移
            local shutdown_port=$((http_port - 75))
            
            # 如果默认偏移的shutdown端口可用，直接使用
            if ! lsof -i tcp:$shutdown_port > /dev/null; then
                echo "$http_port:$shutdown_port"
                return 0
            fi
            
            # 否则查找一个可用的shutdown端口
            for ((s_port=SHUTDOWN_PORT_RANGE_START; s_port<=SHUTDOWN_PORT_RANGE_END; s_port++)); do
                if ! lsof -i tcp:$s_port > /dev/null; then
                    echo "$http_port:$s_port"
                    return 0
                fi
            done
        fi
    done
    
    # 如果找不到可用的端口对，返回空
    echo ""
    return 1
}

# 改进：查找可用端口函数 (单一端口)
find_available_port() {
    local START_PORT=$1
    local END_PORT=$2
    for ((port=START_PORT; port<=END_PORT; port++)); do
        if ! lsof -i tcp:$port > /dev/null; then
            echo $port
            return 0
        fi
    done
    return 1
}

# 改进：更彻底地释放指定端口，最多尝试3次
force_release_port() {
    local PORT=$1
    local MAX_ATTEMPTS=3
    local ATTEMPT=1
    
    yellow "尝试释放端口 $PORT..."
    show_port_usage $PORT
    
    while [ $ATTEMPT -le $MAX_ATTEMPTS ]; do
        PIDS=$(lsof -ti tcp:$PORT)
        if [ -z "$PIDS" ]; then
            green "端口 $PORT 已成功释放。"
            return 0
        fi
        
        yellow "尝试 $ATTEMPT/$MAX_ATTEMPTS: 强制终止占用端口 $PORT 的进程 (PID: $PIDS)"
        kill -9 $PIDS 2>/dev/null
        sleep 2
        
        if ! lsof -i tcp:$PORT > /dev/null; then
            green "端口 $PORT 已成功释放。"
            return 0
        fi
        
        ATTEMPT=$((ATTEMPT+1))
    done
    
    yellow "警告: 尝试 $MAX_ATTEMPTS 次后，仍无法释放端口 $PORT。"
    return 1
}

# 改进：清理所有Tomcat和Java相关进程
kill_all_tomcat_processes() {
    yellow "尝试清理所有Tomcat和相关Java进程..."
    
    # 先尝试使用shutdown脚本正常关闭
    if [ -f "$TOMCAT_HOME/bin/shutdown.sh" ]; then
        yellow "尝试使用shutdown.sh优雅关闭Tomcat..."
        $TOMCAT_HOME/bin/shutdown.sh >/dev/null 2>&1
        sleep 3
    fi
    
    # 杀死所有明确标记为tomcat的Java进程
    local TOMCAT_PIDS=$(ps aux | grep '[j]ava' | grep -i 'tomcat' | awk '{print $2}')
    if [ -n "$TOMCAT_PIDS" ]; then
        yellow "发现Tomcat进程，正在终止: $TOMCAT_PIDS"
        kill -9 $TOMCAT_PIDS 2>/dev/null
        sleep 1
    fi
    
    # 杀死所有监听常见Tomcat端口的进程
    local PORT_PIDS=$(lsof -ti tcp:8005,tcp:8006,tcp:8007,tcp:8008,tcp:8009,tcp:8080,tcp:8081,tcp:8082,tcp:8083,tcp:8084,tcp:8085)
    if [ -n "$PORT_PIDS" ]; then
        yellow "发现监听Tomcat常用端口的进程，正在终止: $PORT_PIDS"
        kill -9 $PORT_PIDS 2>/dev/null
        sleep 1
    fi
    
    # 验证是否还有tomcat进程
    if ps aux | grep '[j]ava' | grep -i 'tomcat' > /dev/null; then
        red "警告: 仍有Tomcat进程在运行。"
        ps aux | grep '[j]ava' | grep -i 'tomcat'
    else
        green "所有Tomcat进程已成功终止。"
    fi
    
    # 验证常用端口是否已释放
    local BLOCKED_PORTS=""
    for port in 8005 8006 8007 8008 8009 8080 8081 8082 8083 8084 8085; do
        if lsof -i tcp:$port > /dev/null; then
            BLOCKED_PORTS="$BLOCKED_PORTS $port"
        fi
    done
    
    if [ -n "$BLOCKED_PORTS" ]; then
        yellow "以下Tomcat常用端口仍被占用:$BLOCKED_PORTS"
        for port in $BLOCKED_PORTS; do
            force_release_port $port
        done
    else
        green "所有Tomcat常用端口已释放。"
    fi
}

# 改进：设置Tomcat端口，使其支持任意HTTP和Shutdown端口组合
set_tomcat_ports() {
    local HTTP_PORT=$1
    local SHUTDOWN_PORT=$2
    local SERVER_XML="$TOMCAT_HOME/conf/server.xml"
    
    # 备份原始配置文件
    cp "$SERVER_XML" "$SERVER_XML.bak.$(date +%Y%m%d%H%M%S)"
    
    yellow "正在修改Tomcat配置..."
    # 修改HTTP连接器端口
    sed -i '' "s/Connector port=\"[0-9]\+\" protocol=\"HTTP\/1.1\"/Connector port=\"$HTTP_PORT\" protocol=\"HTTP\/1.1\"/" "$SERVER_XML"
    # 修改shutdown端口
    sed -i '' "s/Server port=\"[0-9]\+\" shutdown/Server port=\"$SHUTDOWN_PORT\" shutdown/" "$SERVER_XML"
    
    green "已设置Tomcat HTTP端口为 $HTTP_PORT，shutdown端口为 $SHUTDOWN_PORT"
}

# 改进：最后确认端口是否可用
confirm_port_available() {
    local PORT=$1
    local PORT_NAME=$2
    
    if lsof -i tcp:$PORT > /dev/null; then
        red "错误: $PORT_NAME端口 $PORT 仍被占用，无法启动Tomcat。"
        show_port_usage $PORT
        return 1
    fi
    
    green "$PORT_NAME端口 $PORT 可用，可以安全启动Tomcat。"
    return 0
}

# 检测Tomcat是否已成功启动
check_tomcat_running() {
    local PORT=$1
    local CONTEXT_PATH=$2
    local URL="http://localhost:$PORT/$CONTEXT_PATH"
    local MAX_RETRIES=20
    local RETRY_INTERVAL=3
    
    yellow "等待Tomcat在端口 $PORT 上启动并部署 $CONTEXT_PATH..."
    for ((i=1; i<=MAX_RETRIES; i++)); do
        if curl -s "$URL" > /dev/null 2>&1; then
            green "Tomcat成功启动，应用 $CONTEXT_PATH 已部署 (尝试 $i/$MAX_RETRIES)"
            return 0
        fi
        
        # 检查Tomcat是否仍在运行
        if ! ps aux | grep '[j]ava' | grep -i 'tomcat' > /dev/null; then
            red "错误: Tomcat进程已终止，启动失败。"
            return 1
        fi
        
        yellow "等待Tomcat启动... (尝试 $i/$MAX_RETRIES)"
        sleep $RETRY_INTERVAL
    done
    
    red "错误: 等待超时，Tomcat可能未成功启动或应用未成功部署。"
    return 1
}

# 标题
clear
green "===================================="
green "      在线选课系统部署脚本"
green "===================================="
echo ""

# 检查Java环境
yellow "检查Java环境..."
if type -p java > /dev/null; then
    JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')
    green "Java已安装，版本: $JAVA_VERSION"
else
    red "Java未安装，请安装JDK 8或更高版本"
    exit 1
fi

# 清空类文件
yellow "清理旧的编译文件..."
rm -rf classes/*
rm -rf web/WEB-INF/classes/*
rm -rf web/WEB-INF/lib/*

# 创建目录结构
yellow "创建必要的目录结构..."
mkdir -p classes
mkdir -p web/WEB-INF/classes
mkdir -p web/WEB-INF/lib

# 复制资源文件
yellow "复制资源文件到classes目录..."
[ -f "course.csv" ] && cp course.csv web/WEB-INF/classes/ && green "已复制 course.csv 到 web/WEB-INF/classes/" || yellow "警告: 未找到 course.csv"
[ -f "config.properties" ] && cp config.properties web/WEB-INF/classes/ && green "已复制 config.properties 到 web/WEB-INF/classes/" || red "错误: 未找到 config.properties"

# 编译Java代码
yellow "编译Java代码..."
CLASSPATH="."
if [ -d "lib" ]; then
    for jar_file in lib/*.jar; do
        CLASSPATH="$CLASSPATH:$jar_file"
    done
fi

java_files=$(find src -name "*.java")
if [ -z "$java_files" ]; then
    red "未找到任何Java源文件。"
    exit 1
fi

javac -d classes -cp "$CLASSPATH" $java_files 2> compile_errors.log
if [ $? -eq 0 ]; then
    green "Java代码编译成功"
    cp -r classes/* web/WEB-INF/classes/
    green "已将编译后的类文件复制到web/WEB-INF/classes/"
else
    red "Java代码编译失败，请查看 compile_errors.log"
    cat compile_errors.log
    exit 1
fi

# 复制jar包
if [ -d "lib" ]; then
    yellow "复制库文件到WEB-INF/lib目录..."
    cp lib/*.jar web/WEB-INF/lib/
    green "已复制库文件"
fi

# 改进：清理所有Tomcat及相关进程，释放所有可能的端口
yellow "清理所有Tomcat进程和释放端口..."
kill_all_tomcat_processes

# 改进：查找可用的端口对
yellow "查找可用的端口对..."
PORT_PAIR=$(find_available_port_pair)

if [ -z "$PORT_PAIR" ]; then
    red "无法找到可用的端口对，部署终止。"
    exit 1
fi

# 解析端口对
HTTP_PORT=$(echo $PORT_PAIR | cut -d':' -f1)
SHUTDOWN_PORT=$(echo $PORT_PAIR | cut -d':' -f2)

green "使用HTTP端口: $HTTP_PORT"
green "使用Shutdown端口: $SHUTDOWN_PORT"

# 设置Tomcat端口
yellow "配置Tomcat端口..."
set_tomcat_ports $HTTP_PORT $SHUTDOWN_PORT

# 创建WAR文件
yellow "创建WAR文件..."
rm -rf $TOMCAT_HOME/webapps/$APP_NAME
rm -f $TOMCAT_HOME/webapps/$APP_NAME.war
cd web
jar -cvf ../$APP_NAME.war *
cd ..

cp $APP_NAME.war $TOMCAT_HOME/webapps/

# 改进：启动前最后确认端口可用
yellow "启动前最后确认端口可用..."
confirm_port_available $HTTP_PORT "HTTP" || exit 1
confirm_port_available $SHUTDOWN_PORT "Shutdown" || exit 1

# 启动Tomcat
yellow "启动Tomcat..."
$TOMCAT_HOME/bin/startup.sh

# 检测Tomcat是否成功启动
yellow "检测Tomcat是否启动成功..."
if check_tomcat_running $HTTP_PORT $APP_NAME; then
    green "========================================"
    green "            部署完成！"
    green "----------------------------------------"
    green "  访问地址: http://localhost:${HTTP_PORT}/${APP_NAME}"
    if [ "$HTTP_PORT" != "$DEFAULT_HTTP_PORT" ]; then
        yellow "  注意：使用了非默认端口 ${HTTP_PORT}"
    fi
    green "========================================"
else
    red "Tomcat启动超时或启动失败，请检查日志文件："
    red "  $TOMCAT_HOME/logs/catalina.out"
    exit 1
fi

# 记录当前使用的端口到临时文件，方便后续脚本复用
echo "HTTP_PORT=$HTTP_PORT" > .port_config
echo "SHUTDOWN_PORT=$SHUTDOWN_PORT" >> .port_config
green "端口配置已保存到 .port_config 文件。"

exit 0