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

# 标题
clear

green "启动脚本"
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

# 创建目录结构（如果不存在）
yellow "创建必要的目录结构..."
mkdir -p classes
mkdir -p web/WEB-INF/classes
mkdir -p web/WEB-INF/lib

# 编译Java代码
yellow "编译Java代码..."
if [ -d "src" ]; then
    # 检查是否有外部JAR依赖
    if [ -d "lib" ]; then
        CLASSPATH=$(find lib -name "*.jar" | tr '\n' ':')
    else
        CLASSPATH=""
    fi
    
    javac -d classes -cp "$CLASSPATH" $(find src -name "*.java") 2> compile_errors.log
    
    if [ $? -eq 0 ]; then
        green "Java代码编译成功"
        # 复制编译后的类文件到web目录
        cp -r classes/* web/WEB-INF/classes/
    else
        red "Java代码编译失败，请查看compile_errors.log文件了解详细信息"
        exit 1
    fi
else
    red "未找到src目录"
    exit 1
fi

# 复制库文件
if [ -d "lib" ]; then
    yellow "复制库文件到WEB-INF/lib目录..."
    cp lib/*.jar web/WEB-INF/lib/ 2>/dev/null || true
fi

# 启动简易HTTP服务器（用于演示前端）
yellow "启动HTTP服务器..."
yellow "请在浏览器中访问: http://localhost:8000"
yellow "使用以下账号登录系统："
yellow "学号: 2021001    密码: 123456"
yellow "学号: 2021002    密码: 123456"
yellow "按Ctrl+C停止服务器"
echo ""

cd web
python3 -m http.server 8000 2>/dev/null || python -m SimpleHTTPServer 8000

exit 0 