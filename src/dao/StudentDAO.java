package dao;

import model.Student;
import util.DBConnection;

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
            e.printStackTrace();
        } finally {
            DBConnection.close(conn, pstmt, rs);
        }
        
        return students;
    }
    
    /**
     * 验证学生登录
     */
    public Student validateLogin(String studentId, String password) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        Student student = null;
        
        try {
            conn = DBConnection.getConnection();
            String sql = "SELECT * FROM Student WHERE student_id = ? AND password = ?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, studentId);
            pstmt.setString(2, password);
            rs = pstmt.executeQuery();
            
            if (rs.next()) {
                student = mapResultSetToStudent(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBConnection.close(conn, pstmt, rs);
        }
        
        return student;
    }
    
    /**
     * 添加学生
     */
    public boolean add(Student student) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        boolean success = false;
        
        try {
            conn = DBConnection.getConnection();
            String sql = "INSERT INTO Student (student_id, name, birth_date, id_card, address, password) " +
                    "VALUES (?, ?, ?, ?, ?, ?)";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, student.getStudentId());
            pstmt.setString(2, student.getName());
            pstmt.setDate(3, student.getBirthDate() != null ? new java.sql.Date(student.getBirthDate().getTime()) : null);
            pstmt.setString(4, student.getIdCard());
            pstmt.setString(5, student.getAddress());
            pstmt.setString(6, student.getPassword());
            
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
     * 更新学生信息
     */
    public boolean update(Student student) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        boolean success = false;
        
        try {
            conn = DBConnection.getConnection();
            String sql = "UPDATE Student SET name = ?, birth_date = ?, id_card = ?, address = ?, password = ? " +
                    "WHERE student_id = ?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, student.getName());
            pstmt.setDate(2, student.getBirthDate() != null ? new java.sql.Date(student.getBirthDate().getTime()) : null);
            pstmt.setString(3, student.getIdCard());
            pstmt.setString(4, student.getAddress());
            pstmt.setString(5, student.getPassword());
            pstmt.setString(6, student.getStudentId());
            
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