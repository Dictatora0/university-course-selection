#!/bin/bash

# 在线选课系统 - 字段一致性检查工具
# 作者：Claude AI
# 日期：2024-05-18
# 功能：检查Java模型类中的字段名与前端JavaScript API调用中的字段名是否一致

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 项目根路径
PROJECT_ROOT="/Users/lifulin/Desktop/dbLab/Pt"
SRC_DIR="$PROJECT_ROOT/src"
WEB_DIR="$PROJECT_ROOT/web"
MODEL_DIR="$SRC_DIR/model"
JS_DIR="$WEB_DIR/js"

# 创建报告目录
REPORT_DIR="$PROJECT_ROOT/reports"
mkdir -p $REPORT_DIR
REPORT_FILE="$REPORT_DIR/field_consistency_report_$(date +%Y%m%d%H%M%S).md"

echo "# 字段一致性检查报告" > $REPORT_FILE
echo "生成时间: $(date)" >> $REPORT_FILE
echo "" >> $REPORT_FILE

# 函数：检查Java模型类中的字段
check_java_model_fields() {
    echo -e "${BLUE}正在检查Java模型类中的字段...${NC}"
    echo "## Java模型类字段" >> $REPORT_FILE
    echo "" >> $REPORT_FILE
    
    for java_file in $(find $MODEL_DIR -name "*.java"); do
        class_name=$(basename $java_file .java)
        echo -e "${YELLOW}检查 $class_name...${NC}"
        echo "### $class_name" >> $REPORT_FILE
        echo "" >> $REPORT_FILE
        echo "| Java字段名 | SerializedName | 数据类型 |" >> $REPORT_FILE
        echo "|------------|----------------|----------|" >> $REPORT_FILE
        
        # 使用grep和sed提取字段信息
        grep -E '@SerializedName|private.*[;=]' $java_file | sed 'N;s/\n/ /' | \
        while read line; do
            # 提取SerializedName值和字段名
            serialized_name=$(echo $line | grep -oE '@SerializedName\("\w+"\)' | grep -oE '"\w+"' | tr -d '"')
            field_name=$(echo $line | grep -oE 'private\s+\w+(\[\])?(\s+\w+);' | awk '{print $NF}' | sed 's/;//')
            field_type=$(echo $line | grep -oE 'private\s+\w+(\[\])?' | awk '{print $2}')
            
            if [ -n "$field_name" ]; then
                if [ -n "$serialized_name" ]; then
                    echo "| $field_name | $serialized_name | $field_type |" >> $REPORT_FILE
                else
                    echo "| $field_name | *未指定* | $field_type |" >> $REPORT_FILE
                fi
            fi
        done
        
        echo "" >> $REPORT_FILE
    done
}

# 函数：检查JavaScript API调用中的字段
check_js_api_fields() {
    echo -e "${BLUE}正在检查JavaScript API调用中的字段...${NC}"
    echo "## JavaScript API字段" >> $REPORT_FILE
    echo "" >> $REPORT_FILE
    
    for js_file in $(find $JS_DIR -name "*.js"); do
        file_name=$(basename $js_file)
        echo -e "${YELLOW}检查 $file_name...${NC}"
        echo "### $file_name" >> $REPORT_FILE
        echo "" >> $REPORT_FILE
        echo "| API路径 | 字段名 | 使用位置 |" >> $REPORT_FILE
        echo "|---------|--------|----------|" >> $REPORT_FILE
        
        # 使用grep查找JSON.stringify调用和body定义
        grep -n "JSON.stringify" $js_file | while read -r line; do
            line_number=$(echo $line | cut -d: -f1)
            
            # 获取上下文
            context=$(sed -n "$((line_number-5)),$((line_number+5))p" $js_file)
            api_path=$(echo "$context" | grep -oE "fetch\(.*\)" | grep -oE '/api/[^,\)]*')
            
            # 检查JSON.stringify参数中的字段
            fields=$(echo "$context" | grep -A 3 "JSON.stringify" | grep -oE "\w+:\s*\w+" | sed 's/:\s*/: /')
            
            if [ -n "$fields" ]; then
                echo "$fields" | while read -r field; do
                    field_name=$(echo $field | cut -d: -f1 | xargs)
                    echo "| $api_path | $field_name | 第$line_number行 |" >> $REPORT_FILE
                done
            fi
        done
        
        echo "" >> $REPORT_FILE
    done
}

# 函数：检查HTML表单字段
check_html_form_fields() {
    echo -e "${BLUE}正在检查HTML表单字段...${NC}"
    echo "## HTML表单字段" >> $REPORT_FILE
    echo "" >> $REPORT_FILE
    
    for html_file in $(find $WEB_DIR -name "*.html"); do
        file_name=$(basename $html_file)
        echo -e "${YELLOW}检查 $file_name...${NC}"
        echo "### $file_name" >> $REPORT_FILE
        echo "" >> $REPORT_FILE
        echo "| 表单ID | 输入字段ID | 字段名称 |" >> $REPORT_FILE
        echo "|--------|------------|----------|" >> $REPORT_FILE
        
        # 使用grep提取表单和输入字段
        forms=$(grep -n "<form" $html_file | cut -d: -f1)
        
        for form_line in $forms; do
            # 提取表单ID
            form_id=$(sed -n "${form_line}p" $html_file | grep -oE 'id="[^"]*"' | cut -d'"' -f2)
            
            # 查找表单结束行
            end_line=$(sed -n "${form_line},\$p" $html_file | grep -n "</form>" | head -1 | cut -d: -f1)
            end_line=$((form_line + end_line - 1))
            
            # 提取所有输入字段
            sed -n "${form_line},${end_line}p" $html_file | grep -oE '<input[^>]*>' | while read -r input; do
                input_id=$(echo "$input" | grep -oE 'id="[^"]*"' | cut -d'"' -f2)
                input_name=$(echo "$input" | grep -oE 'name="[^"]*"' | cut -d'"' -f2)
                
                if [ -n "$input_id" ]; then
                    if [ -n "$input_name" ]; then
                        echo "| $form_id | $input_id | $input_name |" >> $REPORT_FILE
                    else
                        echo "| $form_id | $input_id | *未指定* |" >> $REPORT_FILE
                    fi
                fi
            done
        done
        
        echo "" >> $REPORT_FILE
    done
}

# 函数：检查潜在的不一致
check_inconsistencies() {
    echo -e "${BLUE}正在分析字段不一致...${NC}"
    echo "## 潜在的字段不一致" >> $REPORT_FILE
    echo "" >> $REPORT_FILE
    
    # 提取Java字段和SerializedName
    java_fields=$(grep -r "@SerializedName" $MODEL_DIR --include="*.java" | grep -oE '@SerializedName\("[^"]+"\)' | sed 's/@SerializedName("\([^"]*\)")/\1/')
    
    # 提取JavaScript字段
    js_fields=$(grep -r "JSON.stringify" $JS_DIR --include="*.js" | grep -oE '{[^}]*}' | grep -oE '\w+:' | sed 's/://' | sort | uniq)
    
    # 比较并找出不一致
    echo "### Java中存在但JavaScript中可能不同名的字段" >> $REPORT_FILE
    echo "" >> $REPORT_FILE
    echo "| Java序列化名 | JavaScript中可能的对应字段 |" >> $REPORT_FILE
    echo "|--------------|----------------------------|" >> $REPORT_FILE
    
    for java_field in $java_fields; do
        # 转换命名约定（snake_case到camelCase）
        camel_case=$(echo $java_field | sed 's/_\([a-z]\)/\U\1/g')
        
        # 检查JavaScript中是否存在相应字段（可能使用不同命名约定）
        js_match=$(echo "$js_fields" | grep -i "$camel_case\|$java_field")
        
        if [ -n "$js_match" ] && [ "$js_match" != "$java_field" ]; then
            echo "| $java_field | $js_match |" >> $REPORT_FILE
        fi
    done
    
    echo "" >> $REPORT_FILE
}

# 主执行函数
main() {
    echo -e "${GREEN}开始字段一致性检查...${NC}"
    
    # 执行各检查函数
    check_java_model_fields
    check_js_api_fields
    check_html_form_fields
    check_inconsistencies
    
    echo -e "${GREEN}检查完成！报告已生成: ${REPORT_FILE}${NC}"
    echo "## 建议" >> $REPORT_FILE
    echo "" >> $REPORT_FILE
    echo "1. 确保前后端字段命名一致，特别是API请求和响应中的字段。" >> $REPORT_FILE
    echo "2. 优先使用后端定义的字段名（通过@SerializedName指定）。" >> $REPORT_FILE
    echo "3. 在JavaScript中使用与后端一致的字段名，避免额外的映射工作。" >> $REPORT_FILE
    echo "4. 对于新功能，确保从一开始就保持字段名的一致性。" >> $REPORT_FILE
}

# 启动主函数
main 