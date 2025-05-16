import csv
import mysql.connector
import hashlib

# --- 数据库连接配置 ---
DB_CONFIG = {
    'host': 'localhost',
    'user': 'root',
    'password': '12345678', #! MySQL密码
    'database': 'course_selection'
}

# --- CSV文件路径 ---
CSV_FILE_PATH = 'course.csv'  # 确保此文件与脚本在同一目录，或提供完整路径

def get_db_connection():
    """建立数据库连接"""
    try:
        conn = mysql.connector.connect(**DB_CONFIG)
        return conn
    except mysql.connector.Error as err:
        print(f"数据库连接失败: {err}")
        return None

def generate_dept_id(dept_name):
    """根据院系名称生成一个相对稳定的院系ID (示例：使用部分哈希值)
       在实际应用中，院系ID最好由用户指定或通过其他方式确保其意义和唯一性。
    """
    # 使用SHA256哈希的前8个字符作为ID的一部分，确保唯一性，同时避免过长
    # 注意：如果两个不同的院系名称哈希碰撞（概率极低），可能会导致问题。
    # 更好的做法是有一个专门的院系管理界面来创建院系并分配ID。
    hasher = hashlib.sha256()
    hasher.update(dept_name.encode('utf-8'))
    return "DEPT_" + hasher.hexdigest()[:8].upper()

def import_data():
    """从CSV文件导入课程和院系数据到数据库"""
    conn = get_db_connection()
    if not conn:
        return

    cursor = conn.cursor()
    imported_courses_count = 0
    processed_rows_count = 0
    created_departments_count = 0
    department_cache = {} # 用于缓存已处理的院系 {dept_name: dept_id}

    try:
        # 先加载所有已存在的院系到缓存
        cursor.execute("SELECT dept_name, dept_id FROM Department")
        for row in cursor.fetchall():
            department_cache[row[0]] = row[1]

        with open(CSV_FILE_PATH, mode='r', encoding='utf-8') as csvfile:
            csv_reader = csv.reader(csvfile)
            next(csv_reader)  # 跳过表头

            for row in csv_reader:
                processed_rows_count += 1
                if len(row) < 3:
                    print(f"跳过格式不正确的行: {row}")
                    continue

                course_id_csv = row[0].strip()
                course_name_csv = row[1].strip()
                dept_name_csv = row[2].strip()

                if not course_id_csv or not course_name_csv or not dept_name_csv:
                    print(f"跳过包含空值的行 (课程ID, 名称或院系名): {row}")
                    continue

                # 1. 处理院系数据
                dept_id_db = department_cache.get(dept_name_csv)
                if not dept_id_db:
                    # 院系不存在，创建新院系
                    dept_id_new = generate_dept_id(dept_name_csv)
                    try:
                        sql_insert_dept = "INSERT INTO Department (dept_id, dept_name) VALUES (%s, %s)"
                        cursor.execute(sql_insert_dept, (dept_id_new, dept_name_csv))
                        # conn.commit() # 考虑在循环外一次性提交或分批提交
                        department_cache[dept_name_csv] = dept_id_new
                        dept_id_db = dept_id_new
                        created_departments_count +=1
                        print(f"创建新院系: ID={dept_id_db}, 名称='{dept_name_csv}'")
                    except mysql.connector.Error as err:
                        # 检查是否是因为dept_id已存在（如果generate_dept_id不够唯一）或dept_name已存在（UNIQUE约束）
                        if err.errno == 1062: # Duplicate entry
                            print(f"警告: 尝试插入重复院系 '{dept_name_csv}' (ID: {dept_id_new}) 或其ID已存在。尝试获取现有ID。")
                            # 尝试再次从数据库获取（可能由并发操作或其他方式创建）
                            cursor.execute("SELECT dept_id FROM Department WHERE dept_name = %s", (dept_name_csv,))
                            existing = cursor.fetchone()
                            if existing:
                                dept_id_db = existing[0]
                                department_cache[dept_name_csv] = dept_id_db
                            else:
                                print(f"错误: 无法为院系 '{dept_name_csv}' 创建或找到ID。跳过相关课程。")
                                continue
                        else:
                            print(f"数据库错误 (插入院系 '{dept_name_csv}'): {err}")
                            continue # 跳过此课程
                
                # 2. 处理课程数据 (确保dept_id_db有效)
                if not dept_id_db:
                    print(f"错误: 院系 '{dept_name_csv}' 的ID无法确定，跳过课程 '{course_name_csv}'")
                    continue

                try:
                    # 检查课程是否已存在
                    sql_check_course = "SELECT course_id FROM Course WHERE course_id = %s"
                    cursor.execute(sql_check_course, (course_id_csv,))
                    if cursor.fetchone():
                        # print(f"课程 '{course_name_csv}' (ID: {course_id_csv}) 已存在，跳过。")
                        continue
                    
                    # 插入课程 (假设学分默认为3.0，如果CSV中有，可以读取)
                    sql_insert_course = "INSERT INTO Course (course_id, course_name, dept_id, credit) VALUES (%s, %s, %s, %s)"
                    # 假设CSV中没有学分列，默认为3.0，如果CSV有学分列，应从row[x]获取
                    credit_csv = 3.0 
                    try:
                        if len(row) > 3 and row[3].strip(): # 检查是否有第四列且不为空
                           credit_csv = float(row[3].strip())
                    except ValueError:
                        print(f"警告: 课程'{course_name_csv}'的学分格式无效 ('{row[3]}')，将使用默认值3.0。")
                        credit_csv = 3.0

                    cursor.execute(sql_insert_course, (course_id_csv, course_name_csv, dept_id_db, credit_csv))
                    imported_courses_count += 1
                except mysql.connector.Error as err:
                    print(f"数据库错误 (插入课程 '{course_name_csv}'): {err}")
        
        conn.commit() # 所有操作完成后提交一次
        print(f"\n导入完成!")
        print(f"总共处理行数: {processed_rows_count}")
        print(f"新创建院系数量: {created_departments_count}")
        print(f"新导入课程数量: {imported_courses_count}")

    except FileNotFoundError:
        print(f"错误: CSV文件 '{CSV_FILE_PATH}' 未找到。")
    except mysql.connector.Error as err:
        print(f"数据库操作错误: {err}")
        conn.rollback()
    except Exception as e:
        print(f"发生未知错误: {e}")
        if conn: conn.rollback()
    finally:
        if cursor:
            cursor.close()
        if conn:
            conn.close()

if __name__ == '__main__':
    print("开始导入课程数据...")
    # 确保在运行此脚本前，数据库表结构 (db_setup.sql) 已被正确应用。
    import_data() 