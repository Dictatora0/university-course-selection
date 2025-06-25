#!/bin/bash

# 设置变量
MYSQL_USER="root"
MYSQL_PASSWORD="12345678"
DB_NAME="course_selection"
SQL_FILE="db_init.sql"

# 输出提示信息
echo "准备初始化数据库..."

# 删除已存在的数据库
echo "正在尝试删除已存在的数据库 $DB_NAME..."
mysql -u$MYSQL_USER -p$MYSQL_PASSWORD -e "DROP DATABASE IF EXISTS $DB_NAME;"

if [ $? -eq 0 ]; then
    echo "已成功删除旧数据库（如果存在）"
else
    echo "警告：删除旧数据库操作失败，可能是MySQL连接问题"
fi

# 检查SQL文件是否存在
if [ -f "$SQL_FILE" ]; then
    echo "正在创建数据库和初始化表结构..."
    mysql -u$MYSQL_USER -p$MYSQL_PASSWORD < $SQL_FILE
    
    # 检查执行结果
    if [ $? -eq 0 ]; then
        echo "数据库初始化成功！"
    else
        echo "错误：数据库初始化失败"
        exit 1
    fi
else
    echo "错误：SQL文件 $SQL_FILE 不存在"
    exit 1
fi

echo "数据库初始化完成" 