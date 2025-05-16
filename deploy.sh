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

# 配置Tomcat路径，根据实际情况修改
TOMCAT_HOME="/usr/local/tomcat"
APP_NAME="course-selection"

# 标题
clear
green "      在线选课系统部署脚本"
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

# 创建目录结构
yellow "创建必要的目录结构..."
mkdir -p classes
mkdir -p web/WEB-INF/classes
mkdir -p web/WEB-INF/lib

# 编译Java代码
yellow "编译Java代码..."
if [ -d "src" ]; then
    # 设置classpath
    CLASSPATH=""
    
    # 添加lib目录中的jar文件到classpath
    if [ -d "lib" ]; then
        for jar in lib/*.jar; do
            CLASSPATH="$CLASSPATH:$jar"
        done
        yellow "使用classpath: $CLASSPATH"
    fi
    
    javac -d classes -cp "$CLASSPATH" $(find src -name "*.java") 2> compile_errors.log
    
    if [ $? -eq 0 ]; then
        green "Java代码编译成功"
        
        # 复制编译后的类文件到web目录
        cp -r classes/* web/WEB-INF/classes/
        green "已将编译后的类文件复制到web/WEB-INF/classes/"
        
        # 复制配置文件到classes目录
        cp config.properties web/WEB-INF/classes/
        green "已复制配置文件到web/WEB-INF/classes/"
    else
        red "Java代码编译失败，请查看compile_errors.log文件了解详细信息"
        cat compile_errors.log
        exit 1
    fi
else
    red "未找到src目录"
    exit 1
fi

# 复制库文件
if [ -d "lib" ]; then
    yellow "复制库文件到WEB-INF/lib目录..."
    cp -r lib/*.jar web/WEB-INF/lib/
    green "已复制库文件"
fi

# 检查是否要部署到Tomcat
read -p "是否部署到Tomcat? (y/n): " deploy_tomcat

if [ "$deploy_tomcat" = "y" ]; then
    if [ -d "$TOMCAT_HOME" ]; then
        yellow "正在部署到Tomcat..."
        
        # 停止Tomcat
        yellow "停止Tomcat..."
        $TOMCAT_HOME/bin/shutdown.sh
        sleep 2
        
        # 删除旧应用
        rm -rf $TOMCAT_HOME/webapps/$APP_NAME
        rm -f $TOMCAT_HOME/webapps/$APP_NAME.war
        
        # 创建WAR文件
        yellow "创建WAR文件..."
        cd web
        jar -cvf ../$APP_NAME.war *
        cd ..
        
        # 复制WAR文件到Tomcat
        cp $APP_NAME.war $TOMCAT_HOME/webapps/
        
        # 启动Tomcat
        yellow "启动Tomcat..."
        $TOMCAT_HOME/bin/startup.sh
        
        green "========================================"
        green "  部署完成！"
        green "  请在浏览器中访问: http://localhost:8080/$APP_NAME"
        green "  使用以下账号登录系统："
        yellow "  学号: 2021001    密码: 123456"
        yellow "  学号: 2021002    密码: 123456"
        green "========================================"
    else
        red "Tomcat目录不存在，请检查TOMCAT_HOME变量"
    fi
else
    green "========================================"
    green "  编译完成！未部署到Tomcat"
    green "  您可以使用以下命令手动运行简易HTTP服务器："
    yellow "  cd web && python3 -m http.server 8000"
    green "  然后在浏览器中访问: http://localhost:8000"
    green "========================================"
fi

exit 0 