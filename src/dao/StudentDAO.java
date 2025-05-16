package src.dao;

import src.model.Student;
import src.util.DBUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class StudentDAO {
    
    // 添加学生
    public boolean addStudent(Student student) throws SQLException {
        String sql = "INSERT INTO Student(student_id, name, birth_date, id_card, address, password) VALUES(?,?,?,?,?,?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, student.getStudentId());
            pstmt.setString(2, student.getName());
            pstmt.setDate(3, new java.sql.Date(student.getBirthDate().getTime()));
            pstmt.setString(4, student.getIdCard());
            pstmt.setString(5, student.getAddress());
            pstmt.setString(6, student.getPassword());
            return pstmt.executeUpdate() > 0;
        }
    }
    
    // 根据学号查询学生
    public Student getStudentById(String studentId) throws SQLException {
        String sql = "SELECT * FROM Student WHERE student_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, studentId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToStudent(rs);
                }
            }
        }
        return null;
    }
    
    // 验证学生登录
    public Student validateLogin(String studentId, String password) throws SQLException {
        String sql = "SELECT * FROM Student WHERE student_id = ? AND password = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, studentId);
            pstmt.setString(2, password);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToStudent(rs);
                }
            }
        }
        return null;
    }
    
    // 查询所有学生
    public List<Student> getAllStudents() throws SQLException {
        String sql = "SELECT * FROM Student";
        List<Student> students = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                students.add(mapResultSetToStudent(rs));
            }
        }
        return students;
    }
    
    // 更新学生信息
    public boolean updateStudent(Student student) throws SQLException {
        String sql = "UPDATE Student SET name = ?, birth_date = ?, id_card = ?, address = ?, password = ? WHERE student_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, student.getName());
            pstmt.setDate(2, new java.sql.Date(student.getBirthDate().getTime()));
            pstmt.setString(3, student.getIdCard());
            pstmt.setString(4, student.getAddress());
            pstmt.setString(5, student.getPassword());
            pstmt.setString(6, student.getStudentId());
            return pstmt.executeUpdate() > 0;
        }
    }
    
    // 删除学生
    public boolean deleteStudent(String studentId) throws SQLException {
        String sql = "DELETE FROM Student WHERE student_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, studentId);
            return pstmt.executeUpdate() > 0;
        }
    }
    
    // 将ResultSet映射为Student对象
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