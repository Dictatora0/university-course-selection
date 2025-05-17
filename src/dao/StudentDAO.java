package dao;

import model.Student;
import util.DBConnection;
import util.PasswordUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 学生数据访问对象，处理与Student表相关的数据库操作
 */
public class StudentDAO {
    
    /**
     * 通过学号查询学生
     */
    public Student findById(String studentId) {
        String sql = "SELECT * FROM student WHERE student_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, studentId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Student student = new Student();
                    student.setStudentId(rs.getString("student_id"));
                    student.setName(rs.getString("name"));
                    student.setPassword(rs.getString("password"));
                    if (hasColumn(rs, "balance")) {
                        student.setBalance(rs.getDouble("balance"));
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
            String sql = "SELECT * FROM Student";
            pstmt = conn.prepareStatement(sql);
            rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Student student = mapResultSetToStudent(rs);
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
            conn = DBConnection.getConnection();
            StringBuilder sqlBuilder = new StringBuilder("UPDATE Student SET name = ?, birth_date = ?, id_card = ?, address = ? ");
            
            // 检查是否需要更新密码
            boolean updatePassword = student.getPassword() != null && !student.getPassword().isEmpty();
            if (updatePassword) {
                sqlBuilder.append(", password = ? ");
            }
            sqlBuilder.append("WHERE student_id = ?");
            
            pstmt = conn.prepareStatement(sqlBuilder.toString());
            pstmt.setString(1, student.getName());
            pstmt.setDate(2, student.getBirthDate() != null ? new java.sql.Date(student.getBirthDate().getTime()) : null);
            pstmt.setString(3, student.getIdCard());
            pstmt.setString(4, student.getAddress());
            
            int parameterIndex = 5;
            if (updatePassword) {
                String hashedPassword = PasswordUtil.hashPassword(student.getPassword());
                if (hashedPassword == null) {
                     System.err.println("Password hashing failed during update for student: " + student.getStudentId());
                     return false; 
                }
                pstmt.setString(parameterIndex++, hashedPassword);
            }
            pstmt.setString(parameterIndex, student.getStudentId());
            
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
        return student;
    }

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
} 