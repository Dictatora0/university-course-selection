package dao;

import model.Student;
import util.DBConnection;
import util.PasswordUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.math.BigDecimal;

/**
 * 学生数据访问对象，处理与Student表相关的数据库操作
 */
public class StudentDAO {
    
    /**
     * 通过学号查询学生
     */
    public Student findById(String studentId) {
        String sql = "SELECT s.*, d.dept_name FROM student s LEFT JOIN department d ON s.dept_id = d.dept_id WHERE s.student_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, studentId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Student student = new Student();
                    student.setStudentId(rs.getString("student_id"));
                    student.setName(rs.getString("name"));
                    student.setPassword(rs.getString("password"));
                    student.setDeptId(rs.getString("dept_id"));
                    student.setDeptName(rs.getString("dept_name"));
                    student.setBirthDate(rs.getDate("birth_date"));
                    student.setIdCard(rs.getString("id_card"));
                    student.setAddress(rs.getString("address"));
                    student.setCreatedAt(rs.getTimestamp("created_at"));
                    if (hasColumn(rs, "balance")) {
                        student.setBalance(rs.getDouble("balance"));
                    }
                    // 获取email和phone字段
                    if (hasColumn(rs, "email")) {
                        student.setEmail(rs.getString("email"));
                    }
                    if (hasColumn(rs, "phone")) {
                        student.setPhone(rs.getString("phone"));
                    }
                    return student;
                }
            }
        } catch (SQLException e) {
            System.err.println("[StudentDAO.findById] SQLException for studentId " + studentId + ": " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("[StudentDAO.findById] Exception for studentId " + studentId + ": " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
    
    /**
     * 获取所有学生
     */
    public List<Student> findAll() {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        List<Student> students = new ArrayList<>();
        
        try {
            conn = DBConnection.getConnection();
            String sql = "SELECT s.*, d.dept_name FROM student s LEFT JOIN department d ON s.dept_id = d.dept_id";
            pstmt = conn.prepareStatement(sql);
            rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Student student = mapResultSetToStudent(rs);
                if (hasColumn(rs, "dept_name")) {
                    student.setDeptName(rs.getString("dept_name"));
                }
                students.add(student);
            }
        } catch (SQLException e) {
            System.err.println("[StudentDAO.findAll] SQLException: " + e.getMessage());
            e.printStackTrace();
        } finally {
            DBConnection.close(conn, pstmt, rs);
        }
        
        return students;
    }
    
    /**
     * 验证学生登录
     * @param studentId 学号
     * @param rawPassword 用户输入的明文密码
     * @return 如果验证成功返回Student对象，否则返回null
     */
    public Student validateLogin(String studentId, String rawPassword) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        Student student = null;
        
        try {
            conn = DBConnection.getConnection();
            String sql = "SELECT * FROM Student WHERE student_id = ?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, studentId);
            rs = pstmt.executeQuery();
            
            if (rs.next()) {
                String hashedPasswordFromDB = rs.getString("password");
                if (PasswordUtil.checkPassword(rawPassword, hashedPasswordFromDB)) {
                    student = mapResultSetToStudent(rs);
                } else {
                    System.out.println("[StudentDAO.validateLogin] Password mismatch for studentId: " + studentId);
                }
            } else {
                System.out.println("[StudentDAO.validateLogin] Student not found with studentId: " + studentId);
            }
        } catch (SQLException e) {
            System.err.println("[StudentDAO.validateLogin] SQLException: " + e.getMessage());
            e.printStackTrace();
        } finally {
            DBConnection.close(conn, pstmt, rs);
        }
        
        return student;
    }
    
    /**
     * 添加学生 (密码应为明文，DAO内部进行哈希)
     */
    public boolean add(Student student) {
        Connection conn = null;
        boolean success = false;
        
        try {
            // 首先检查传入对象的有效性
            if (student == null) {
                System.err.println("[StudentDAO.add] 传入的Student对象为null");
                return false;
            }
            
            if (student.getStudentId() == null || student.getStudentId().trim().isEmpty()) {
                System.err.println("[StudentDAO.add] Student ID不能为空");
                return false;
            }
            
            if (student.getName() == null || student.getName().trim().isEmpty()) {
                System.err.println("[StudentDAO.add] 姓名不能为空");
                return false;
            }
            
            if (student.getPassword() == null || student.getPassword().trim().isEmpty()) {
                System.err.println("[StudentDAO.add] 密码不能为空");
                return false;
            }
            
            String hashedPassword = PasswordUtil.hashPassword(student.getPassword());
            if (hashedPassword == null) {
                System.err.println("[StudentDAO.add] Password hashing failed for student: " + student.getStudentId());
                return false;
            }
            
            System.out.println("[StudentDAO.add] 详细信息如下:");
            System.out.println("  ID: " + student.getStudentId());
            System.out.println("  Name: " + student.getName());
            System.out.println("  Birth Date: " + student.getBirthDate());
            System.out.println("  ID Card: " + student.getIdCard());
            System.out.println("  Address: " + student.getAddress());
            System.out.println("  Password hash: " + hashedPassword.substring(0, Math.min(10, hashedPassword.length())) + "...");
            
            conn = DBConnection.getConnection();
            
            // 使用一个空的INSERT语句，根据非空字段动态构建
            StringBuilder sqlBuilder = new StringBuilder("INSERT INTO Student (student_id, name, password");
            StringBuilder valuesBuilder = new StringBuilder("VALUES (?, ?, ?");
            
            // 准备参数列表
            List<Object> params = new ArrayList<>();
            params.add(student.getStudentId());
            params.add(student.getName());
            params.add(hashedPassword);
            
            // 检查并添加可选字段
            if (student.getBirthDate() != null) {
                sqlBuilder.append(", birth_date");
                valuesBuilder.append(", ?");
                params.add(student.getBirthDate());
            }
            
            if (student.getIdCard() != null && !student.getIdCard().trim().isEmpty()) {
                sqlBuilder.append(", id_card");
                valuesBuilder.append(", ?");
                params.add(student.getIdCard());
            }
            
            if (student.getAddress() != null && !student.getAddress().trim().isEmpty()) {
                sqlBuilder.append(", address");
                valuesBuilder.append(", ?");
                params.add(student.getAddress());
            }
            
            if (student.getDeptId() != null && !student.getDeptId().trim().isEmpty()) {
                sqlBuilder.append(", dept_id");
                valuesBuilder.append(", ?");
                params.add(student.getDeptId());
            }
            
            // 添加余额字段，默认为0
            sqlBuilder.append(", balance");
            valuesBuilder.append(", ?");
            params.add(0.0); // 默认余额为0
            
            // 完成SQL语句
            sqlBuilder.append(") ");
            valuesBuilder.append(")");
            String sql = sqlBuilder.toString() + valuesBuilder.toString();
            
            System.out.println("[StudentDAO.add] 执行SQL: " + sql);
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                // 设置参数
                int paramIndex = 1;
                for (Object param : params) {
                    if (param instanceof String) {
                        pstmt.setString(paramIndex++, (String) param);
                    } else if (param instanceof java.util.Date) {
                        pstmt.setTimestamp(paramIndex++, new Timestamp(((java.util.Date) param).getTime()));
                    } else if (param instanceof Double) {
                        pstmt.setDouble(paramIndex++, (Double) param);
                    } else if (param instanceof Integer) {
                        pstmt.setInt(paramIndex++, (Integer) param);
                    } else {
                        // 对于其他类型或null，尝试使用setObject
                        pstmt.setObject(paramIndex++, param);
                    }
                }

                System.out.println("[StudentDAO.add] Executing SQL: " + pstmt.toString()); // 打印将要执行的SQL
                int affectedRows = pstmt.executeUpdate();
                success = affectedRows > 0;
                if(success) {
                    System.out.println("[StudentDAO.add] Student added successfully: " + student.getStudentId());
                } else {
                    System.err.println("[StudentDAO.add] Student add failed, no rows affected for: " + student.getStudentId());
                }
            } catch (SQLException e) {
                System.err.println("[StudentDAO.add] SQLException during add for student " + student.getStudentId() + ": " + e.getMessage());
                System.err.println("[StudentDAO.add] SQLState: " + e.getSQLState());
                System.err.println("[StudentDAO.add] Error Code: " + e.getErrorCode());
                e.printStackTrace();
                success = false; // 确保在异常时 success 为 false
            }
        } catch (SQLException e) {
            System.err.println("[StudentDAO.add] SQLException (outer) for student " + (student != null ? student.getStudentId() : "NULL_STUDENT") + ": " + e.getMessage());
            e.printStackTrace();
            success = false;
        } catch (Exception e) { // 添加通用异常捕获
            System.err.println("[StudentDAO.add] Generic Exception for student " + (student != null ? student.getStudentId() : "NULL_STUDENT") + ": " + e.getMessage());
            e.printStackTrace();
            success = false;
        } finally {
            DBConnection.close(conn, null, null); // pstmt 和 rs 在内部 try-with-resources 中关闭
        }
        
        return success;
    }
    
    /**
     * 更新学生信息 (如果密码字段非空，则更新密码)
     */
    public boolean update(Student student) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        boolean success = false;
        
        try {
            System.out.println("[StudentDAO.update] 开始更新学生信息，学号：" + student.getStudentId());
            conn = DBConnection.getConnection();
            // 构建基础的UPDATE语句，包含所有可更新的字段
            StringBuilder sqlBuilder = new StringBuilder("UPDATE Student SET name = ?, birth_date = ?, id_card = ?, address = ?, dept_id = ?, email = ?, phone = ? ");
            
            // 检查是否需要更新密码
            boolean updatePassword = student.getPassword() != null && !student.getPassword().isEmpty();
            System.out.println("[StudentDAO.update] 是否需要更新密码: " + updatePassword);
            if (updatePassword) {
                sqlBuilder.append(", password = ? ");
            }
            sqlBuilder.append("WHERE student_id = ?");
            
            System.out.println("[StudentDAO.update] 构建的SQL语句: " + sqlBuilder.toString());
            
            pstmt = conn.prepareStatement(sqlBuilder.toString());
            int parameterIndex = 1;
            pstmt.setString(parameterIndex++, student.getName());
            System.out.println("[StudentDAO.update] 设置name参数: " + student.getName());
            
            java.sql.Date sqlBirthDate = student.getBirthDate() != null ? new java.sql.Date(student.getBirthDate().getTime()) : null;
            pstmt.setDate(parameterIndex++, sqlBirthDate);
            System.out.println("[StudentDAO.update] 设置birth_date参数: " + sqlBirthDate);
            
            pstmt.setString(parameterIndex++, student.getIdCard());
            System.out.println("[StudentDAO.update] 设置id_card参数: " + student.getIdCard());
            
            pstmt.setString(parameterIndex++, student.getAddress());
            System.out.println("[StudentDAO.update] 设置address参数: " + student.getAddress());
            
            pstmt.setString(parameterIndex++, student.getDeptId());
            System.out.println("[StudentDAO.update] 设置dept_id参数: " + student.getDeptId());
            
            pstmt.setString(parameterIndex++, student.getEmail());
            System.out.println("[StudentDAO.update] 设置email参数: " + student.getEmail());
            
            pstmt.setString(parameterIndex++, student.getPhone());
            System.out.println("[StudentDAO.update] 设置phone参数: " + student.getPhone());
            
            if (updatePassword) {
                String hashedPassword = PasswordUtil.hashPassword(student.getPassword());
                if (hashedPassword == null) {
                     System.err.println("[StudentDAO.update] 密码哈希失败，学号: " + student.getStudentId());
                     return false; 
                }
                pstmt.setString(parameterIndex++, hashedPassword);
                System.out.println("[StudentDAO.update] 设置password参数: " + hashedPassword.substring(0, Math.min(10, hashedPassword.length())) + "...");
            }
            pstmt.setString(parameterIndex, student.getStudentId()); // WHERE子句的student_id
            System.out.println("[StudentDAO.update] 设置WHERE条件student_id: " + student.getStudentId());
            
            System.out.println("[StudentDAO.update] 准备执行SQL: " + pstmt.toString());
            
            try {
                int rowsAffected = pstmt.executeUpdate();
                System.out.println("[StudentDAO.update] 执行结果: 影响的行数 = " + rowsAffected);
                success = rowsAffected > 0;
            } catch (SQLException sqlEx) {
                System.err.println("[StudentDAO.update] SQL执行异常: " + sqlEx.getMessage());
                System.err.println("[StudentDAO.update] SQLState: " + sqlEx.getSQLState());
                System.err.println("[StudentDAO.update] Error Code: " + sqlEx.getErrorCode());
                sqlEx.printStackTrace();
                throw sqlEx; // 重新抛出以便外层捕获
            }
            
            if(success){
                System.out.println("[StudentDAO.update] 学生信息更新成功，学号: " + student.getStudentId());
            } else {
                System.err.println("[StudentDAO.update] 学生信息更新失败，没有影响的行，学号: " + student.getStudentId());
            }
        } catch (SQLException e) {
            System.err.println("[StudentDAO.update] SQLException for student "+ student.getStudentId() + ": " + e.getMessage());
            e.printStackTrace();
        } finally {
            DBConnection.close(conn, pstmt, null);
        }
        
        return success;
    }
    
    /**
     * 删除学生
     */
    public boolean delete(String studentId) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        boolean success = false;
        
        try {
            conn = DBConnection.getConnection();
            String sql = "DELETE FROM Student WHERE student_id = ?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, studentId);
            
            int rowsAffected = pstmt.executeUpdate();
            success = rowsAffected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBConnection.close(conn, pstmt, null);
        }
        
        return success;
    }
    
    /**
     * 将ResultSet映射到Student对象
     */
    private Student mapResultSetToStudent(ResultSet rs) throws SQLException {
        Student student = new Student();
        student.setStudentId(rs.getString("student_id"));
        student.setName(rs.getString("name"));
        student.setBirthDate(rs.getDate("birth_date"));
        student.setIdCard(rs.getString("id_card"));
        student.setAddress(rs.getString("address"));
        student.setPassword(rs.getString("password"));
        student.setCreatedAt(rs.getTimestamp("created_at"));
        
        // 检查并设置dept_id字段
        if (hasColumn(rs, "dept_id")) {
            student.setDeptId(rs.getString("dept_id"));
        }
        
        // 检查并设置balance字段
        if (hasColumn(rs, "balance")) {
            student.setBalance(rs.getDouble("balance"));
        }
        
        // 检查并设置accountStatus字段
        if (hasColumn(rs, "account_status")) {
            student.setAccountStatus(rs.getBoolean("account_status"));
        } else {
            student.setAccountStatus(true); // 默认为启用状态
        }
        
        // 检查并设置email字段
        if (hasColumn(rs, "email")) {
            student.setEmail(rs.getString("email"));
        }
        
        // 检查并设置phone字段
        if (hasColumn(rs, "phone")) {
            student.setPhone(rs.getString("phone"));
        }
        
        return student;
    }

    /**
     * 更新学生余额
     * @param studentId 学生ID
     * @param newBalance 新余额
     * @return 更新是否成功
     */
    public boolean updateBalance(String studentId, double newBalance) {
        String sql = "UPDATE student SET balance = ? WHERE student_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setDouble(1, newBalance);
            pstmt.setString(2, studentId);
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 使用BigDecimal更新学生余额，避免精度问题
     * @param studentId 学生ID
     * @param newBalance 新余额(BigDecimal类型)
     * @return 更新是否成功
     */
    public boolean updateBalance(String studentId, BigDecimal newBalance) {
        String sql = "UPDATE student SET balance = ? WHERE student_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setBigDecimal(1, newBalance);
            pstmt.setString(2, studentId);
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public String getName(String studentId) {
        String sql = "SELECT name FROM student WHERE student_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, studentId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("name");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取学生总数
     * @return 学生总数
     */
    public int getTotalStudentCount() {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        int count = 0;
        
        try {
            conn = DBConnection.getConnection();
            String sql = "SELECT COUNT(*) AS total FROM Student";
            pstmt = conn.prepareStatement(sql);
            rs = pstmt.executeQuery();
            
            if (rs.next()) {
                count = rs.getInt("total");
            }
        } catch (SQLException e) {
            System.err.println("[StudentDAO.getTotalStudentCount] SQLException: " + e.getMessage());
            e.printStackTrace();
        } finally {
            DBConnection.close(conn, pstmt, rs);
        }
        
        return count;
    }
    
    /**
     * 获取最近注册的学生数量
     * @param days 最近的天数
     * @return 在指定天数内注册的学生数量
     */
    public int getRecentRegisteredStudentCount(int days) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        int count = 0;
        
        try {
            conn = DBConnection.getConnection();
            String sql = "SELECT COUNT(*) AS total FROM Student WHERE created_at >= DATE_SUB(NOW(), INTERVAL ? DAY)";
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, days);
            rs = pstmt.executeQuery();
            
            if (rs.next()) {
                count = rs.getInt("total");
            }
        } catch (SQLException e) {
            System.err.println("[StudentDAO.getRecentRegisteredStudentCount] SQLException: " + e.getMessage());
            e.printStackTrace();
        } finally {
            DBConnection.close(conn, pstmt, rs);
        }
        
        return count;
    }
    
    /**
     * 获取学生性别分布
     * @return 包含性别分布的列表，每个元素为一个包含性别和对应数量的对象
     */
    public List<Object[]> getGenderDistribution() {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        List<Object[]> distribution = new ArrayList<>();
        
        try {
            conn = DBConnection.getConnection();
            String sql = "SELECT gender, COUNT(*) AS count FROM Student GROUP BY gender";
            pstmt = conn.prepareStatement(sql);
            rs = pstmt.executeQuery();
            
            while (rs.next()) {
                String gender = rs.getString("gender");
                int count = rs.getInt("count");
                distribution.add(new Object[]{gender, count});
            }
        } catch (SQLException e) {
            System.err.println("[StudentDAO.getGenderDistribution] SQLException: " + e.getMessage());
            e.printStackTrace();
        } finally {
            DBConnection.close(conn, pstmt, rs);
        }
        
        return distribution;
    }

    // Helper method to check if a column exists in ResultSet to avoid SQLException
    private boolean hasColumn(ResultSet rs, String columnName) throws SQLException {
        ResultSetMetaData rsmd = rs.getMetaData();
        int columns = rsmd.getColumnCount();
        for (int x = 1; x <= columns; x++) {
            if (columnName.equals(rsmd.getColumnName(x))) {
                return true;
            }
        }
        return false;
    }

    /**
     * 更新学生状态（启用/禁用）
     * @param studentId 学生ID
     * @param status 状态 (true=启用, false=禁用)
     * @return 是否更新成功
     */
    public boolean updateStatus(String studentId, boolean status) {
        String sql = "UPDATE Student SET account_status = ? WHERE student_id = ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setBoolean(1, status);
            pstmt.setString(2, studentId);
            
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            System.err.println("[StudentDAO.updateStatus] SQLException: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 搜索学生（支持学号、姓名和院系名称的模糊匹配）
     * @param keyword 搜索关键词
     * @return 匹配的学生列表
     */
    public List<Student> searchStudents(String keyword) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        List<Student> students = new ArrayList<>();
        
        try {
            conn = DBConnection.getConnection();
            // 构建SQL查询，支持学号、姓名和院系名称的模糊匹配
            String sql = "SELECT s.*, d.dept_name FROM student s " +
                    "LEFT JOIN department d ON s.dept_id = d.dept_id " +
                    "WHERE s.student_id LIKE ? OR s.name LIKE ? OR d.dept_name LIKE ?";
            
            pstmt = conn.prepareStatement(sql);
            String searchPattern = "%" + keyword + "%";
            pstmt.setString(1, searchPattern);
            pstmt.setString(2, searchPattern);
            pstmt.setString(3, searchPattern);
            
            rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Student student = mapResultSetToStudent(rs);
                students.add(student);
            }
        } catch (SQLException e) {
            System.err.println("[StudentDAO.searchStudents] SQLException: " + e.getMessage());
            e.printStackTrace();
        } finally {
            DBConnection.close(conn, pstmt, rs);
        }
        
        return students;
    }
} 