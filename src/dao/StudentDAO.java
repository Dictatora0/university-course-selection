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
                student = mapResultSetToStudent(rs);
            }
        } catch (SQLException e) {
            System.err.println("[StudentDAO.findById] SQLException: " + e.getMessage());
            e.printStackTrace();
        } finally {
            DBConnection.close(conn, pstmt, rs);
        }
        
        return student;
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
        PreparedStatement pstmt = null;
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
            StringBuilder valuesBuilder = new StringBuilder("(?, ?, ?");
            
            // 构建参数列表
            List<Object> params = new ArrayList<>();
            params.add(student.getStudentId());
            params.add(student.getName());
            params.add(hashedPassword);
            
            // 添加可选字段
            if (student.getBirthDate() != null) {
                sqlBuilder.append(", birth_date");
                valuesBuilder.append(", ?");
                params.add(new java.sql.Date(student.getBirthDate().getTime()));
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
            
            // 完成SQL语句
            sqlBuilder.append(") VALUES ").append(valuesBuilder).append(")");
            String sql = sqlBuilder.toString();
            
            System.out.println("[StudentDAO.add] 执行SQL: " + sql);
            
            pstmt = conn.prepareStatement(sql);
            
            // 设置参数
            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }
            
            int rowsAffected = pstmt.executeUpdate();
            success = rowsAffected > 0;
            
            if (success) {
                System.out.println("[StudentDAO.add] 添加学生成功: " + student.getStudentId());
            } else {
                System.err.println("[StudentDAO.add] 添加学生失败 (0行受影响): " + student.getStudentId());
            }
        } catch (SQLException e) {
            System.err.println("[StudentDAO.add] SQL异常: " + e.getMessage());
            System.err.println("  SQL状态: " + e.getSQLState());
            System.err.println("  错误代码: " + e.getErrorCode());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("[StudentDAO.add] 未知异常: " + e.getMessage());
            e.printStackTrace();
        } finally {
            DBConnection.close(conn, pstmt, null);
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
} 