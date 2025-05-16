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

# 查找可用端口函数
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

# 精准释放端口函数
release_port() {
    local PORT=$1
    PIDS=$(lsof -ti tcp:$PORT)
    if [ -n "$PIDS" ]; then
        yellow "检测到端口 $PORT 被占用，尝试释放..."
        echo "占用进程: $PIDS"
        kill -9 $PIDS
        sleep 1
        if ! lsof -i tcp:$PORT > /dev/null; then
            green "端口 $PORT 已成功释放。"
        else
            yellow "端口 $PORT 仍然被占用，将尝试使用其他端口..."
        fi
    else
        green "端口 $PORT 未被占用。"
    fi
}

# 广泛释放所有Tomcat相关端口（查找java进程）
kill_tomcat_ports() {
    yellow "尝试关闭所有Tomcat相关进程..."
    PIDS=$(ps aux | grep '[j]ava' | grep -i 'tomcat' | awk '{print $2}')
    if [ -n "$PIDS" ]; then
        kill -9 $PIDS
        green "已终止以下Tomcat进程: $PIDS"
    else
        green "未检测到Tomcat相关的java进程。"
    fi
}

# 设置Tomcat端口的函数
set_tomcat_port() {
    local NEW_PORT=$1
    local SERVER_XML="$TOMCAT_HOME/conf/server.xml"
    local SHUTDOWN_PORT=$((NEW_PORT - 75))  # 通常shutdown端口比HTTP端口小75
    
    # 备份原始配置文件
    cp "$SERVER_XML" "$SERVER_XML.bak"
    
    yellow "正在修改Tomcat配置..."
    # MacOS适用的sed命令
    sed -i '' "s/Connector port=\"[0-9]\+\" protocol=\"HTTP\/1.1\"/Connector port=\"$NEW_PORT\" protocol=\"HTTP\/1.1\"/" "$SERVER_XML"
    sed -i '' "s/Server port=\"[0-9]\+\" shutdown/Server port=\"$SHUTDOWN_PORT\" shutdown/" "$SERVER_XML"
    
    green "已将Tomcat HTTP端口修改为 $NEW_PORT，shutdown端口修改为 $SHUTDOWN_PORT"
}

# 检测Tomcat是否已成功启动（访问当前端口）
check_tomcat_running() {
    local PORT=${APP_PORT:-8080}
    local RETRIES=10
    local SLEEP_TIME=1
    for ((i=1; i<=RETRIES; i++)); do
        if curl -s http://localhost:$PORT > /dev/null; then
            return 0
        fi
        sleep $SLEEP_TIME
    done
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

# 杀死所有Tomcat相关进程
kill_tomcat_ports

# 在启动Tomcat之前添加端口检查
yellow "检查并配置端口..."
HTTP_PORT_AVAILABLE=0
SHUTDOWN_PORT_AVAILABLE=0

# 检查默认HTTP端口
if ! lsof -i tcp:${DEFAULT_HTTP_PORT} > /dev/null; then
    green "默认HTTP端口 ${DEFAULT_HTTP_PORT} 可用"
    HTTP_PORT_AVAILABLE=1
    export APP_PORT=$DEFAULT_HTTP_PORT
else
    yellow "默认HTTP端口 ${DEFAULT_HTTP_PORT} 被占用"
    # 强制终止占用进程
    PIDS=$(lsof -ti tcp:${DEFAULT_HTTP_PORT})
    if [ -n "$PIDS" ]; then
        yellow "尝试强制终止占用进程: $PIDS"
        kill -9 $PIDS
        sleep 2
        if ! lsof -i tcp:${DEFAULT_HTTP_PORT} > /dev/null; then
            green "成功释放端口 ${DEFAULT_HTTP_PORT}"
            HTTP_PORT_AVAILABLE=1
            export APP_PORT=$DEFAULT_HTTP_PORT
        fi
    fi
fi

# 如果默认HTTP端口仍不可用，寻找替代端口
if [ $HTTP_PORT_AVAILABLE -eq 0 ]; then
    yellow "寻找替代HTTP端口..."
    NEW_PORT=$(find_available_port 8081 8090)
    if [ -n "$NEW_PORT" ]; then
        green "找到可用HTTP端口: $NEW_PORT"
        export APP_PORT=$NEW_PORT
        HTTP_PORT_AVAILABLE=1
    else
        red "无法找到可用HTTP端口，部署终止"
        exit 1
    fi
fi

# 检查或计算shutdown端口
if [ "$APP_PORT" = "$DEFAULT_HTTP_PORT" ]; then
    # 使用默认shutdown端口
    if ! lsof -i tcp:${DEFAULT_SHUTDOWN_PORT} > /dev/null; then
        export SHUTDOWN_PORT=$DEFAULT_SHUTDOWN_PORT
        SHUTDOWN_PORT_AVAILABLE=1
    else
        # 尝试释放默认shutdown端口
        PIDS=$(lsof -ti tcp:${DEFAULT_SHUTDOWN_PORT})
        if [ -n "$PIDS" ]; then
            kill -9 $PIDS
            sleep 2
            if ! lsof -i tcp:${DEFAULT_SHUTDOWN_PORT} > /dev/null; then
                export SHUTDOWN_PORT=$DEFAULT_SHUTDOWN_PORT
                SHUTDOWN_PORT_AVAILABLE=1
            fi
        fi
    fi
else
    # 计算新的shutdown端口
    NEW_SHUTDOWN_PORT=$((APP_PORT - 75))
    if ! lsof -i tcp:${NEW_SHUTDOWN_PORT} > /dev/null; then
        export SHUTDOWN_PORT=$NEW_SHUTDOWN_PORT
        SHUTDOWN_PORT_AVAILABLE=1
    else
        # 寻找任何可用端口作为shutdown端口
        SHUTDOWN_PORT=$(find_available_port 8001 8050)
        if [ -n "$SHUTDOWN_PORT" ]; then
            SHUTDOWN_PORT_AVAILABLE=1
        fi
    fi
fi

# 如果任一端口不可用，终止部署
if [ $HTTP_PORT_AVAILABLE -eq 0 ] || [ $SHUTDOWN_PORT_AVAILABLE -eq 0 ]; then
    red "无法配置必要的端口，部署终止"
    exit 1
fi

# 配置Tomcat使用新端口
if [ "$APP_PORT" != "$DEFAULT_HTTP_PORT" ] || [ "$SHUTDOWN_PORT" != "$DEFAULT_SHUTDOWN_PORT" ]; then
    set_tomcat_port $APP_PORT
fi

green "使用HTTP端口: ${APP_PORT}"
green "使用Shutdown端口: ${SHUTDOWN_PORT}"

# 检查端口是否真的释放
check_port_released() {
    local PORT=$1
    local MAX_RETRIES=5
    
    for i in {1..5}; do
        if ! lsof -i tcp:$PORT > /dev/null; then
            green "端口 $PORT 已释放。"
            return 0
        fi
        yellow "等待端口 $PORT 释放... (尝试 $i/$MAX_RETRIES)"
        sleep 2
    done
    
    # 如果是主要端口（8080），尝试切换到其他可用端口
    if [ $PORT -eq 8080 ]; then
        yellow "端口 8080 无法释放，正在寻找其他可用端口..."
        NEW_PORT=$(find_available_port 8081 8090)
        if [ -n "$NEW_PORT" ]; then
            green "找到可用端口: $NEW_PORT"
            set_tomcat_port $NEW_PORT
            export APP_PORT=$NEW_PORT
            # 同时更新shutdown端口
            export SHUTDOWN_PORT=$((NEW_PORT - 75))
            return 0
        fi
    fi
    
    red "无法找到可用端口，部署终止。"
    exit 1
}

check_port_released 8080
check_port_released 8005

# 停止Tomcat时不使用默认端口
yellow "停止Tomcat..."
if [ -f "$TOMCAT_HOME/bin/shutdown.sh" ]; then
    # 确保使用正确的shutdown端口
    CATALINA_OPTS="-Dserver.port=$SHUTDOWN_PORT" $TOMCAT_HOME/bin/shutdown.sh || true
    sleep 2
fi

# 再次确保端口释放
check_port_released 8080
check_port_released 8005

yellow "创建WAR文件..."
rm -rf $TOMCAT_HOME/webapps/$APP_NAME
rm -f $TOMCAT_HOME/webapps/$APP_NAME.war
cd web
jar -cvf ../$APP_NAME.war *
cd ..

cp $APP_NAME.war $TOMCAT_HOME/webapps/

yellow "启动Tomcat..."
$TOMCAT_HOME/bin/startup.sh

yellow "检测Tomcat是否启动成功..."
MAX_RETRIES=15
DEPLOYMENT_STATUS=0
for ((i=1; i<=MAX_RETRIES; i++)); do
    if curl -s "http://localhost:${APP_PORT}" > /dev/null 2>&1; then
        DEPLOYMENT_STATUS=1
        break
    fi
    yellow "等待Tomcat启动... ($i/$MAX_RETRIES)"
    sleep 2
done

if [ $DEPLOYMENT_STATUS -eq 1 ]; then
    green "========================================"
    green "            部署完成！"
    green "----------------------------------------"
    green "  访问地址: http://localhost:${APP_PORT}/${APP_NAME}"
    if [ "$APP_PORT" != "$DEFAULT_HTTP_PORT" ]; then
        yellow "  注意：使用了非默认端口 ${APP_PORT}"
    fi
    green "========================================"
else
    red "Tomcat启动超时，请检查日志文件："
    red "  $TOMCAT_HOME/logs/catalina.out"
    exit 1
fi

exit 0