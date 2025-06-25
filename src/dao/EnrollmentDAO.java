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
    
    /**
     * 获取选课总数
     * @return 选课记录总数
     */
    public int getTotalEnrollmentCount() {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        int count = 0;
        
        try {
            conn = DBConnection.getConnection();
            String sql = "SELECT COUNT(*) AS total FROM Enrollment";
            pstmt = conn.prepareStatement(sql);
            rs = pstmt.executeQuery();
            
            if (rs.next()) {
                count = rs.getInt("total");
            }
        } catch (SQLException e) {
            System.err.println("[EnrollmentDAO.getTotalEnrollmentCount] SQLException: " + e.getMessage());
            e.printStackTrace();
        } finally {
            DBConnection.close(conn, pstmt, rs);
        }
        
        return count;
    }
    
    /**
     * 获取每日选课数量统计
     * @param days 最近的天数
     * @return 包含日期和选课数量的列表
     */
    public List<Object[]> getDailyEnrollmentStats(int days) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        List<Object[]> stats = new ArrayList<>();
        
        try {
            conn = DBConnection.getConnection();
            String sql = "SELECT DATE(enrollment_date) AS day, COUNT(*) AS count " +
                     "FROM Enrollment " +
                     "WHERE enrollment_date >= DATE_SUB(CURRENT_DATE(), INTERVAL ? DAY) " +
                     "GROUP BY DATE(enrollment_date) " +
                     "ORDER BY day";
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, days);
            rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Date day = rs.getDate("day");
                int count = rs.getInt("count");
                stats.add(new Object[]{day, count});
            }
        } catch (SQLException e) {
            System.err.println("[EnrollmentDAO.getDailyEnrollmentStats] SQLException: " + e.getMessage());
            e.printStackTrace();
        } finally {
            DBConnection.close(conn, pstmt, rs);
        }
        
        return stats;
    }
    
    /**
     * 获取课程的平均分数
     * @return 包含课程ID、课程名称和平均分数的列表
     */
    public List<Object[]> getAverageGradesByCourse() {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        List<Object[]> averageGrades = new ArrayList<>();
        
        try {
            conn = DBConnection.getConnection();
            String sql = "SELECT c.course_id, c.course_name, AVG(e.grade) AS avg_grade " +
                     "FROM Course c " +
                     "LEFT JOIN Enrollment e ON c.course_id = e.course_id " +
                     "WHERE e.grade IS NOT NULL " +
                     "GROUP BY c.course_id, c.course_name " +
                     "ORDER BY avg_grade DESC";
            pstmt = conn.prepareStatement(sql);
            rs = pstmt.executeQuery();
            
            while (rs.next()) {
                String courseId = rs.getString("course_id");
                String courseName = rs.getString("course_name");
                BigDecimal avgGrade = rs.getBigDecimal("avg_grade");
                averageGrades.add(new Object[]{courseId, courseName, avgGrade});
            }
        } catch (SQLException e) {
            System.err.println("[EnrollmentDAO.getAverageGradesByCourse] SQLException: " + e.getMessage());
            e.printStackTrace();
        } finally {
            DBConnection.close(conn, pstmt, rs);
        }
        
        return averageGrades;
    }
    
    /**
     * 获取成绩分布统计
     * @return 包含成绩范围和对应学生数量的列表
     */
    public List<Object[]> getGradeDistribution() {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        List<Object[]> distribution = new ArrayList<>();
        
        try {
            conn = DBConnection.getConnection();
            String sql = "SELECT " +
                     "  CASE " +
                     "    WHEN grade >= 90 THEN 'A (90-100)' " +
                     "    WHEN grade >= 80 THEN 'B (80-89)' " +
                     "    WHEN grade >= 70 THEN 'C (70-79)' " +
                     "    WHEN grade >= 60 THEN 'D (60-69)' " +
                     "    ELSE 'F (0-59)' " +
                     "  END AS grade_range, " +
                     "  COUNT(*) AS count " +
                     "FROM Enrollment " +
                     "WHERE grade IS NOT NULL " +
                     "GROUP BY grade_range " +
                     "ORDER BY MIN(grade) DESC";
            pstmt = conn.prepareStatement(sql);
            rs = pstmt.executeQuery();
            
            while (rs.next()) {
                String gradeRange = rs.getString("grade_range");
                int count = rs.getInt("count");
                distribution.add(new Object[]{gradeRange, count});
            }
        } catch (SQLException e) {
            System.err.println("[EnrollmentDAO.getGradeDistribution] SQLException: " + e.getMessage());
            e.printStackTrace();
        } finally {
            DBConnection.close(conn, pstmt, rs);
        }
        
        return distribution;
    }
} 