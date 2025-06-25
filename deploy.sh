#!/bin/bash

# 输出彩色文本的函数
green() {
    echo -e "\033[32m$1\033[0m" >&2
}

red() {
    echo -e "\033[31m$1\033[0m" >&2
}

yellow() {
    echo -e "\033[33m$1\033[0m" >&2
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

# 解析命令行参数
USER_HTTP_PORT=""
USER_SHUTDOWN_PORT=""
ACTION="redeploy" # 默认操作

while [[ "$#" -gt 0 ]]; do
    case $1 in
        --http-port) USER_HTTP_PORT="$2"; shift ;;
        --shutdown-port) USER_SHUTDOWN_PORT="$2"; shift ;;
        redeploy|kill|start|stop|build|clean) ACTION="$1" ;;
        *) echo "未知参数: $1"; exit 1 ;;
    esac
    shift
done

# 添加：尝试恢复 server.xml
restore_server_xml() {
    local SERVER_XML_FILE="$TOMCAT_HOME/conf/server.xml"
    local CLEAN_SERVER_XML_BACKUP="$TOMCAT_HOME/conf/server.xml.original"
    
    yellow "准备部署环境，检查并恢复 server.xml (如果需要)..."
    
    # 检查server.xml是否存在并且是否已损坏
    if grep -q 'port="-e"' "$SERVER_XML_FILE" || grep -q 'port="警告"' "$SERVER_XML_FILE"; then
        red "检测到 $SERVER_XML_FILE 已损坏！"
        
        if [ -f "$CLEAN_SERVER_XML_BACKUP" ]; then
            yellow "使用备份 $CLEAN_SERVER_XML_BACKUP 恢复 $SERVER_XML_FILE"
            cp "$CLEAN_SERVER_XML_BACKUP" "$SERVER_XML_FILE"
            if [ $? -ne 0 ]; then
                red "错误: 无法从 $CLEAN_SERVER_XML_BACKUP 恢复 $SERVER_XML_FILE。请检查文件权限。"
                exit 1
            fi
            green "成功从备份恢复 $SERVER_XML_FILE"
        else
            red "错误: 未找到备份文件 $CLEAN_SERVER_XML_BACKUP"
            yellow "请执行以下步骤手动恢复 server.xml:"
            yellow "1. 下载干净的 server.xml: curl -o ~/Downloads/server.xml https://raw.githubusercontent.com/apache/tomcat/9.0.x/conf/server.xml"
            yellow "2. 复制到Tomcat目录: sudo cp ~/Downloads/server.xml $SERVER_XML_FILE"
            yellow "3. 创建备份: sudo cp ~/Downloads/server.xml $CLEAN_SERVER_XML_BACKUP"
            exit 1
        fi
    else
        yellow "server.xml 文件检查: 未检测到明显损坏。"
        
        # 仍然创建一个备份（如果还没有）
        if [ ! -f "$CLEAN_SERVER_XML_BACKUP" ] && [ -f "$SERVER_XML_FILE" ]; then
            yellow "未找到 server.xml 的备份，正在创建..."
            cp "$SERVER_XML_FILE" "$CLEAN_SERVER_XML_BACKUP"
            if [ $? -eq 0 ]; then
                green "已创建 server.xml 的备份 ($CLEAN_SERVER_XML_BACKUP)"
            else
                yellow "警告: 无法创建 server.xml 的备份"
            fi
        fi
    fi
    
    show_server_xml_status "restore_server_xml - 完成"
}

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
    # 如果用户指定了端口，则优先使用
    if [ -n "$USER_HTTP_PORT" ] && [ -n "$USER_SHUTDOWN_PORT" ]; then
        if ! lsof -i tcp:$USER_HTTP_PORT > /dev/null && ! lsof -i tcp:$USER_SHUTDOWN_PORT > /dev/null; then
            echo "$USER_HTTP_PORT:$USER_SHUTDOWN_PORT"
            return 0
        else
            yellow "警告: 用户指定的端口 $USER_HTTP_PORT 或 $USER_SHUTDOWN_PORT 已被占用，将尝试自动查找。"
        fi
    fi

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

show_server_xml_status() {
    local stage_name="$1"
    yellow "---- START server.xml status at stage: $stage_name ----"
    yellow "Connector Lines in $TOMCAT_HOME/conf/server.xml:"
    grep "Connector port=" "$TOMCAT_HOME/conf/server.xml" || yellow "  (No Connector port lines found)"
    yellow "Server Port Lines in $TOMCAT_HOME/conf/server.xml:"
    grep "Server port=" "$TOMCAT_HOME/conf/server.xml" || yellow "  (No Server port lines found)"
    yellow "---- END server.xml status at stage: $stage_name ----"
}

# 改进：清理所有Tomcat和Java相关进程
kill_all_tomcat_processes() {
    show_server_xml_status "kill_all_tomcat_processes - Entry"
    yellow "尝试清理所有Tomcat和相关Java进程..."
    
    # 先尝试使用shutdown脚本正常关闭
    if [ -f "$TOMCAT_HOME/bin/shutdown.sh" ]; then
        yellow "尝试使用shutdown.sh优雅关闭Tomcat..."
        CURRENT_SHUTDOWN_PORT=$(grep '<Server port="' "$TOMCAT_HOME/conf/server.xml" | sed -n 's/.*<Server port=\\"\\([0-9]*\\)\\".*/\\1/p')
        if [ -n "$CURRENT_SHUTDOWN_PORT" ] && [[ "$CURRENT_SHUTDOWN_PORT" =~ ^[0-9]+$ ]]; then # Ensure it's a number
            yellow " shutdown.sh using discovered port: $CURRENT_SHUTDOWN_PORT"
            "$TOMCAT_HOME/bin/shutdown.sh" -config "$TOMCAT_HOME/conf/server.xml" -port "$CURRENT_SHUTDOWN_PORT" >/dev/null 2>&1
            sleep 3
        else
            yellow " shutdown.sh: Could not reliably determine current shutdown port or it was invalid ($CURRENT_SHUTDOWN_PORT). Attempting default shutdown."
            "$TOMCAT_HOME/bin/shutdown.sh" >/dev/null 2>&1 
            sleep 3
        fi
    fi
    show_server_xml_status "kill_all_tomcat_processes - After shutdown attempt"
    
    # 杀死所有明确标记为tomcat的Java进程
    local TOMCAT_PIDS=$(ps aux | grep '[j]ava' | grep -i 'tomcat' | awk '{print $2}')
    if [ -n "$TOMCAT_PIDS" ]; then
        yellow "发现Tomcat进程，正在终止: $TOMCAT_PIDS"
        kill -9 $TOMCAT_PIDS 2>/dev/null
        sleep 1
    fi
    
    # 杀死所有监听常见Tomcat端口的进程
    # Corrected lsof command for macOS compatibility and to suppress errors
    local PORT_PIDS=$(lsof -ti tcp:8005 -ti tcp:8006 -ti tcp:8007 -ti tcp:8008 -ti tcp:8009 -ti tcp:8080 -ti tcp:8081 -ti tcp:8082 -ti tcp:8083 -ti tcp:8084 -ti tcp:8085 2>/dev/null)
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
    for port_num in 8005 8006 8007 8008 8009 8080 8081 8082 8083 8084 8085; do # Renamed port to port_num
        if lsof -i tcp:$port_num > /dev/null; then
            BLOCKED_PORTS="$BLOCKED_PORTS $port_num"
        fi
    done
    
    if [ -n "$BLOCKED_PORTS" ]; then
        yellow "以下Tomcat常用端口仍被占用:$BLOCKED_PORTS"
        for port_to_release in $BLOCKED_PORTS; do # Renamed port to port_to_release
            force_release_port $port_to_release
        done
    else
        green "所有Tomcat常用端口已释放。"
    fi
    show_server_xml_status "kill_all_tomcat_processes - Exit"
}

# 改进：设置Tomcat端口，使其支持任意HTTP和Shutdown端口组合
set_tomcat_ports() {
    local HTTP_PORT_ARG=$1
    local SHUTDOWN_PORT_ARG=$2
    local SERVER_XML="$TOMCAT_HOME/conf/server.xml"

    show_server_xml_status "set_tomcat_ports - Entry (HTTP: $HTTP_PORT_ARG, Shutdown: $SHUTDOWN_PORT_ARG)"

    local HTTP_PORT=${HTTP_PORT_ARG}
    local SHUTDOWN_PORT=${SHUTDOWN_PORT_ARG}
    
    yellow "备份原始配置文件 $SERVER_XML to $SERVER_XML.bak.$(date +%Y%m%d%H%M%S)"
    # It's critical server.xml is pristine *before* this backup if it's used for recovery
    cp "$SERVER_XML" "$SERVER_XML.bak.$(date +%Y%m%d%H%M%S)"
    
    yellow "正在修改Tomcat配置..."
    green "目标HTTP端口: $HTTP_PORT, 目标Shutdown端口: $SHUTDOWN_PORT"

    # Ensure these variables are purely numeric before use in sed, even if already cleaned by caller
    HTTP_PORT=$(echo "$HTTP_PORT" | tr -cd '0-9')
    SHUTDOWN_PORT=$(echo "$SHUTDOWN_PORT" | tr -cd '0-9')
    if [ -z "$HTTP_PORT" ] || [ -z "$SHUTDOWN_PORT" ]; then # Final safety check
        red "错误 (set_tomcat_ports): 清理后的端口号为空。HTTP: [$HTTP_PORT], Shutdown: [$SHUTDOWN_PORT]"
        exit 1
    fi

    echo "DEBUG: Before sed HTTP, HTTP_PORT is: [$HTTP_PORT]" >&2
    local sed_http_cmd="s/Connector port=\"[0-9][0-9]*\" protocol=\"HTTP\/1.1\"/Connector port=\"${HTTP_PORT}\" protocol=\"HTTP\/1.1\"/g"
    yellow "将执行 SED HTTP命令: sed -i.bak '$sed_http_cmd' \"$SERVER_XML\""
    sed -i.bak "$sed_http_cmd" "$SERVER_XML"
    local sed_http_exit_code=$?
    yellow "SED HTTP命令退出码: $sed_http_exit_code"
    yellow "修改后 server.xml 中相关的 Connector 行:"
    grep "Connector port=" "$SERVER_XML" || yellow "  (修改后未找到 Connector port 行)"

    echo "DEBUG: Before sed Shutdown, SHUTDOWN_PORT is: [$SHUTDOWN_PORT]" >&2
    local sed_shutdown_cmd="s/Server port=\"[0-9][0-9]*\" shutdown/Server port=\"${SHUTDOWN_PORT}\" shutdown/g"
    yellow "将执行 SED Shutdown命令: sed -i.bak '$sed_shutdown_cmd' \"$SERVER_XML\""
    sed -i.bak "$sed_shutdown_cmd" "$SERVER_XML"
    local sed_shutdown_exit_code=$?
    yellow "SED Shutdown命令退出码: $sed_shutdown_exit_code"
    yellow "修改后 server.xml 中相关的 Server port 行:"
    grep "Server port=" "$SERVER_XML" || yellow "  (修改后未找到 Server port 行)"
    
    yellow "开始验证 Tomcat端口修改..."
    local grep_http_cmd="grep \"Connector port=\\\"${HTTP_PORT}\\\"\" \"$SERVER_XML\""
    yellow "将执行 Grep HTTP验证命令: $grep_http_cmd"
    eval "$grep_http_cmd" > /dev/null
    local grep_http_exit_code=$?
    yellow "Grep HTTP验证命令退出码: $grep_http_exit_code"

    local grep_shutdown_cmd="grep \"Server port=\\\"${SHUTDOWN_PORT}\\\"\" \"$SERVER_XML\""
    yellow "将执行 Grep Shutdown验证命令: $grep_shutdown_cmd"
    eval "$grep_shutdown_cmd" > /dev/null
    local grep_shutdown_exit_code=$?
    yellow "Grep Shutdown命令退出码: $grep_shutdown_exit_code"

    if [ $grep_http_exit_code -eq 0 ] && [ $grep_shutdown_exit_code -eq 0 ]; then
        green "已成功设置并验证Tomcat HTTP端口为 $HTTP_PORT，shutdown端口为 $SHUTDOWN_PORT"
    else
        red "错误: 修改或验证Tomcat端口配置失败。请检查 $SERVER_XML 文件及上述命令输出。"
        exit 1
    fi
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
        if curl -s --connect-timeout 2 "$URL" > /dev/null 2>&1; then
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

# 部署主函数
deploy() {
    green "===================================="
    green "       在线选课系统部署脚本       "
    green "===================================="
    
    # 恢复 server.xml（如果需要）
    restore_server_xml
    
    # 清理旧的编译文件
    yellow "清理旧的编译文件..."
    rm -rf classes/*
    rm -rf web/WEB-INF/classes/*
    mkdir -p classes
    mkdir -p web/WEB-INF/classes
    mkdir -p web/WEB-INF/lib

    yellow "复制资源文件到classes目录..."
    # 从项目根目录复制
    if [ -f "course.csv" ]; then
        cp "course.csv" web/WEB-INF/classes/ && green "已复制 course.csv 到 web/WEB-INF/classes/"
    else
        yellow "警告: course.csv 未在项目根目录找到"
    fi
    if [ -f "config.properties" ]; then
        cp "config.properties" web/WEB-INF/classes/ && green "已复制 config.properties 到 web/WEB-INF/classes/"
    else
        red "错误: config.properties 未在项目根目录找到，这是运行所必需的！"
        # exit 1 # 如果config.properties是必需的，可以选择在这里退出
    fi

    yellow "编译Java代码..."
    # 确保所有需要的库都在编译路径中
    CLASSPATH="src"
    # 添加所有需要的jar包到CLASSPATH
    for jar_file in web/WEB-INF/lib/*.jar lib/*.jar "$TOMCAT_HOME/lib/"*.jar; do
      if [ -f "$jar_file" ]; then
        CLASSPATH="$CLASSPATH:$jar_file"
      fi
    done

    JAVAC_COMMAND="javac -Xlint:all -cp \"$CLASSPATH\" -d classes $(find src -name '*.java')"

    echo "执行编译命令: $JAVAC_COMMAND"
    if eval $JAVAC_COMMAND; then
        green "Java代码编译成功"
        cp -r classes/* web/WEB-INF/classes/
        green "已将编译后的类文件复制到web/WEB-INF/classes/"
    else
        red "Java代码编译失败，请检查错误信息。"
        exit 1
    fi

    yellow "复制库文件到WEB-INF/lib目录..."
    # 确保从项目的lib目录复制
    if [ -d "lib" ] && [ "$(ls -A lib)" ]; then
        cp lib/*.jar web/WEB-INF/lib/ && green "已复制库文件"
    else
        yellow "警告: lib目录为空或不存在，或复制失败"
    fi

    yellow "创建WAR文件..."
    cd web || exit
    if jar -cvf "../$APP_NAME.war" *; then
        green "WAR文件创建成功: ../$APP_NAME.war"
    else
        red "WAR文件创建失败。"
        cd ..
        exit 1
    fi
    cd ..
    green "构建完成。"

    # 清理所有Tomcat进程和释放端口
    kill_all_tomcat_processes

    # 查找可用的端口对
    yellow "查找可用的端口对..."
    PORT_PAIR=$(find_available_port_pair)
    if [ -z "$PORT_PAIR" ]; then
        red "错误：无法找到可用的HTTP/Shutdown端口对。请检查端口范围或手动指定端口。"
        exit 1
    fi
    SELECTED_HTTP_PORT=$(echo "$PORT_PAIR" | cut -d: -f1 | tr -cd '0-9')
    SELECTED_SHUTDOWN_PORT=$(echo "$PORT_PAIR" | cut -d: -f2 | tr -cd '0-9')

    # 再次验证清理后的端口号是否有效
    if [ -z "$SELECTED_HTTP_PORT" ] || [ -z "$SELECTED_SHUTDOWN_PORT" ]; then
        red "错误：清理后的端口号为空，无法继续。原始PORT_PAIR: $PORT_PAIR"
        exit 1
    fi

    green "使用HTTP端口: $SELECTED_HTTP_PORT"
    green "使用Shutdown端口: $SELECTED_SHUTDOWN_PORT"

    set_tomcat_ports $SELECTED_HTTP_PORT $SELECTED_SHUTDOWN_PORT

    # 将选定的端口保存到文件
    echo "HTTP_PORT=$SELECTED_HTTP_PORT" > .port_config
    echo "SHUTDOWN_PORT=$SELECTED_SHUTDOWN_PORT" >> .port_config
    echo "APP_NAME=$APP_NAME" >> .port_config

    # 部署WAR文件
    yellow "部署WAR文件到Tomcat..."
    rm -rf "$TOMCAT_HOME/webapps/$APP_NAME" # 删除旧的展开目录
    rm -f "$TOMCAT_HOME/webapps/$APP_NAME.war" # 删除旧的WAR包
    cp "$APP_NAME.war" "$TOMCAT_HOME/webapps/"

    # 启动前最后确认端口可用
    yellow "启动前最后确认端口可用..."
    if ! confirm_port_available $SELECTED_HTTP_PORT "HTTP" || ! confirm_port_available $SELECTED_SHUTDOWN_PORT "Shutdown"; then
        exit 1
    fi

    # 启动Tomcat
    yellow "启动Tomcat..."
    if [ -f "$TOMCAT_HOME/bin/startup.sh" ]; then
        "$TOMCAT_HOME/bin/startup.sh"
    else
        red "错误: 未找到Tomcat启动脚本 $TOMCAT_HOME/bin/startup.sh"
        exit 1
    fi

    # 检测Tomcat是否启动成功
    if check_tomcat_running $SELECTED_HTTP_PORT $APP_NAME; then
        green "========================================"
        green "            部署完成！"
        green "----------------------------------------"
        yellow "  访问地址: http://localhost:$SELECTED_HTTP_PORT/$APP_NAME"
        green "========================================"
        yellow "端口配置已保存到 .port_config 文件。"
    else
        red "Tomcat启动超时或启动失败，请检查日志文件："
        red "  $TOMCAT_HOME/logs/catalina.out"
        exit 1
    fi
}

# 根据ACTION执行不同操作
case $ACTION in
    clean)
        yellow "清理旧的编译文件和WAR包..."
        rm -rf classes/*
        rm -rf web/WEB-INF/classes/*
        rm -rf web/WEB-INF/lib/*
        rm -f "$APP_NAME.war"
        rm -f ".port_config"
        green "清理完成。"
        exit 0
        ;;
    build)
        deploy
        exit 0
        ;;
    kill)
        kill_all_tomcat_processes
        exit 0
        ;;
    start)
        # 启动Tomcat的逻辑，可以考虑从redeploy中抽取
        echo "启动Tomcat..."
        # ...
        ;;
    stop)
        # 停止Tomcat的逻辑，可以考虑从redeploy中抽取
        echo "停止Tomcat..."
        # ...
        ;;
    redeploy)
        deploy
        exit 0
        ;;
    *)
        echo "未知操作: $ACTION"
        exit 1
        ;;
esac