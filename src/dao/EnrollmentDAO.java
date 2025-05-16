package dao;

import model.Enrollment;
import util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.math.BigDecimal;

/**
 * 选课数据访问对象，处理与Enrollment表相关的数据库操作
 */
public class EnrollmentDAO {
    
    /**
     * 查询学生已选课程（包含课程和院系信息）
     */
    public List<Enrollment> findByStudentId(String studentId) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        List<Enrollment> enrollments = new ArrayList<>();
        
        try {
            conn = DBConnection.getConnection();
            String sql = "SELECT e.*, c.course_name, c.credit, d.dept_name FROM Enrollment e " +
                    "JOIN Course c ON e.course_id = c.course_id " +
                    "JOIN Department d ON c.dept_id = d.dept_id " +
                    "WHERE e.student_id = ?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, studentId);
            rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Enrollment enrollment = new Enrollment();
                enrollment.setStudentId(rs.getString("student_id"));
                enrollment.setCourseId(rs.getString("course_id"));
                enrollment.setGrade(rs.getBigDecimal("grade"));
                enrollment.setEnrollmentDate(rs.getTimestamp("enrollment_date"));
                enrollment.setCourseName(rs.getString("course_name"));
                enrollment.setCredit(rs.getBigDecimal("credit"));
                enrollment.setDeptName(rs.getString("dept_name"));
                enrollments.add(enrollment);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBConnection.close(conn, pstmt, rs);
        }
        
        return enrollments;
    }
    
    /**
     * 查询选修特定课程的所有学生
     */
    public List<Enrollment> findByCourseId(String courseId) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        List<Enrollment> enrollments = new ArrayList<>();
        
        try {
            conn = DBConnection.getConnection();
            String sql = "SELECT e.* FROM Enrollment e WHERE e.course_id = ?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, courseId);
            rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Enrollment enrollment = new Enrollment();
                enrollment.setStudentId(rs.getString("student_id"));
                enrollment.setCourseId(rs.getString("course_id"));
                enrollment.setGrade(rs.getBigDecimal("grade"));
                enrollment.setEnrollmentDate(rs.getTimestamp("enrollment_date"));
                enrollments.add(enrollment);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBConnection.close(conn, pstmt, rs);
        }
        
        return enrollments;
    }
    
    /**
     * 添加选课记录
     */
    public boolean add(Enrollment enrollment) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        boolean success = false;
        
        try {
            conn = DBConnection.getConnection();
            String sql = "INSERT INTO Enrollment (student_id, course_id, enrollment_date) VALUES (?, ?, ?)";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, enrollment.getStudentId());
            pstmt.setString(2, enrollment.getCourseId());
            
            if (enrollment.getEnrollmentDate() == null) {
                pstmt.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
            } else {
                pstmt.setTimestamp(3, new Timestamp(enrollment.getEnrollmentDate().getTime()));
            }
            
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
     * 更新成绩
     */
    public boolean updateGrade(String studentId, String courseId, BigDecimal grade) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        boolean success = false;
        
        try {
            conn = DBConnection.getConnection();
            String sql = "UPDATE Enrollment SET grade = ? WHERE student_id = ? AND course_id = ?";
            pstmt = conn.prepareStatement(sql);
            
            if (grade == null) {
                pstmt.setNull(1, Types.DECIMAL);
            } else {
                pstmt.setBigDecimal(1, grade);
            }
            
            pstmt.setString(2, studentId);
            pstmt.setString(3, courseId);
            
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
     * 删除选课记录
     */
    public boolean delete(String studentId, String courseId) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        boolean success = false;
        
        try {
            conn = DBConnection.getConnection();
            String sql = "DELETE FROM Enrollment WHERE student_id = ? AND course_id = ?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, studentId);
            pstmt.setString(2, courseId);
            
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
     * 查询学生的GPA
     */
    public double calculateGPA(String studentId) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        double gpa = 0.0;
        
        try {
            conn = DBConnection.getConnection();
            String sql = "SELECT SUM(c.credit * e.grade)/SUM(c.credit) as gpa " +
                    "FROM Enrollment e " +
                    "JOIN Course c ON e.course_id = c.course_id " +
                    "WHERE e.student_id = ? AND e.grade IS NOT NULL";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, studentId);
            rs = pstmt.executeQuery();
            
            if (rs.next()) {
                gpa = rs.getDouble("gpa");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBConnection.close(conn, pstmt, rs);
        }
        
        return gpa;
    }
    
    /**
     * 检查学生是否已选此课程
     */
    public boolean isEnrolled(String studentId, String courseId) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        boolean enrolled = false;
        
        try {
            conn = DBConnection.getConnection();
            String sql = "SELECT 1 FROM Enrollment WHERE student_id = ? AND course_id = ?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, studentId);
            pstmt.setString(2, courseId);
            rs = pstmt.executeQuery();
            
            enrolled = rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBConnection.close(conn, pstmt, rs);
        }
        
        return enrolled;
    }
} 