package dao;

import model.Course;
import util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.math.BigDecimal;

/**
 * 课程数据访问对象，处理与Course表相关的数据库操作
 */
public class CourseDAO {
    
    /**
     * 查询所有课程（带院系名称）
     */
    public List<Course> findAllWithDeptName() {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        List<Course> courses = new ArrayList<>();
        
        try {
            conn = DBConnection.getConnection();
            String sql = "SELECT c.*, d.dept_name, " +
                    "(SELECT COUNT(*) FROM Enrollment e WHERE e.course_id = c.course_id) AS enrollment_count " +
                    "FROM Course c " +
                    "JOIN Department d ON c.dept_id = d.dept_id";
            pstmt = conn.prepareStatement(sql);
            rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Course course = new Course();
                course.setCourseId(rs.getString("course_id"));
                course.setCourseName(rs.getString("course_name"));
                course.setDeptId(rs.getString("dept_id"));
                course.setCredit(rs.getBigDecimal("credit"));
                course.setDeptName(rs.getString("dept_name"));
                course.setCapacity(rs.getInt("capacity"));
                course.setEnrollmentCount(rs.getInt("enrollment_count"));
                courses.add(course);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBConnection.close(conn, pstmt, rs);
        }
        
        return courses;
    }
    
    /**
     * 根据ID查询课程
     */
    public Course findById(String courseId) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        Course course = null;
        
        try {
            conn = DBConnection.getConnection();
            String sql = "SELECT * FROM Course WHERE course_id = ?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, courseId);
            rs = pstmt.executeQuery();
            
            if (rs.next()) {
                course = new Course();
                course.setCourseId(rs.getString("course_id"));
                course.setCourseName(rs.getString("course_name"));
                course.setDeptId(rs.getString("dept_id"));
                course.setCredit(rs.getBigDecimal("credit"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBConnection.close(conn, pstmt, rs);
        }
        
        return course;
    }
    
    /**
     * 根据ID查询课程（使用传入的数据库连接）
     * @param courseId 课程ID
     * @param conn 数据库连接
     * @return 如果找到则返回Course对象，否则返回null
     * @throws SQLException 如果数据库操作失败
     */
    public Course findById(String courseId, Connection conn) throws SQLException {
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        Course course = null;
        
        try {
            String sql = "SELECT c.*, d.dept_name, " +
                    "(SELECT COUNT(*) FROM Enrollment e WHERE e.course_id = c.course_id) AS current_enrollment " +
                    "FROM Course c LEFT JOIN Department d ON c.dept_id = d.dept_id " +
                    "WHERE c.course_id = ?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, courseId);
            rs = pstmt.executeQuery();
            
            if (rs.next()) {
                course = new Course();
                course.setCourseId(rs.getString("course_id"));
                course.setCourseName(rs.getString("course_name"));
                course.setDeptId(rs.getString("dept_id"));
                course.setDeptName(rs.getString("dept_name"));
                course.setCredit(rs.getBigDecimal("credit"));
                course.setDescription(rs.getString("description"));
                course.setCapacity(rs.getInt("capacity"));
                course.setEnrollmentCount(rs.getInt("current_enrollment"));
            }
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) { e.printStackTrace(); }
            if (pstmt != null) try { pstmt.close(); } catch (SQLException e) { e.printStackTrace(); }
            // 不关闭连接，由调用者负责
        }
        
        return course;
    }
    
    /**
     * 根据院系ID查询课程
     */
    public List<Course> findByDeptId(String deptId) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        List<Course> courses = new ArrayList<>();
        
        try {
            conn = DBConnection.getConnection();
            String sql = "SELECT * FROM Course WHERE dept_id = ?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, deptId);
            rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Course course = new Course();
                course.setCourseId(rs.getString("course_id"));
                course.setCourseName(rs.getString("course_name"));
                course.setDeptId(rs.getString("dept_id"));
                course.setCredit(rs.getBigDecimal("credit"));
                courses.add(course);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBConnection.close(conn, pstmt, rs);
        }
        
        return courses;
    }
    
    /**
     * 添加课程
     */
    public boolean add(Course course) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        boolean success = false;
        
        try {
            conn = DBConnection.getConnection();
            String sql = "INSERT INTO Course (course_id, course_name, dept_id, credit) VALUES (?, ?, ?, ?)";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, course.getCourseId());
            pstmt.setString(2, course.getCourseName());
            pstmt.setString(3, course.getDeptId());
            pstmt.setBigDecimal(4, course.getCredit());
            
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
     * 更新课程
     */
    public boolean update(Course course) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        boolean success = false;
        
        try {
            conn = DBConnection.getConnection();
            String sql = "UPDATE Course SET course_name = ?, dept_id = ?, credit = ? WHERE course_id = ?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, course.getCourseName());
            pstmt.setString(2, course.getDeptId());
            pstmt.setBigDecimal(3, course.getCredit());
            pstmt.setString(4, course.getCourseId());
            
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
     * 删除课程
     */
    public boolean delete(String courseId) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        boolean success = false;
        
        try {
            conn = DBConnection.getConnection();
            String sql = "DELETE FROM Course WHERE course_id = ?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, courseId);
            
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
     * 获取课程总数
     * @return 课程总数
     */
    public int getTotalCourseCount() {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        int count = 0;
        
        try {
            conn = DBConnection.getConnection();
            String sql = "SELECT COUNT(*) AS total FROM Course";
            pstmt = conn.prepareStatement(sql);
            rs = pstmt.executeQuery();
            
            if (rs.next()) {
                count = rs.getInt("total");
            }
        } catch (SQLException e) {
            System.err.println("[CourseDAO.getTotalCourseCount] SQLException: " + e.getMessage());
            e.printStackTrace();
        } finally {
            DBConnection.close(conn, pstmt, rs);
        }
        
        return count;
    }
    
    /**
     * 获取各院系的课程数量分布
     * @return 包含院系ID、院系名称和课程数量的列表
     */
    public List<Object[]> getCoursesCountByDepartment() {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        List<Object[]> distribution = new ArrayList<>();
        
        try {
            conn = DBConnection.getConnection();
            String sql = "SELECT d.dept_id, d.dept_name, COUNT(c.course_id) AS course_count " +
                     "FROM Department d " +
                     "LEFT JOIN Course c ON d.dept_id = c.dept_id " +
                     "GROUP BY d.dept_id, d.dept_name " +
                     "ORDER BY course_count DESC";
            pstmt = conn.prepareStatement(sql);
            rs = pstmt.executeQuery();
            
            while (rs.next()) {
                String deptId = rs.getString("dept_id");
                String deptName = rs.getString("dept_name");
                int courseCount = rs.getInt("course_count");
                distribution.add(new Object[]{deptId, deptName, courseCount});
            }
        } catch (SQLException e) {
            System.err.println("[CourseDAO.getCoursesCountByDepartment] SQLException: " + e.getMessage());
            e.printStackTrace();
        } finally {
            DBConnection.close(conn, pstmt, rs);
        }
        
        return distribution;
    }
    
    /**
     * 获取课程平均学分
     * @return 所有课程的平均学分
     */
    public BigDecimal getAverageCourseCredit() {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        BigDecimal avgCredit = BigDecimal.ZERO;
        
        try {
            conn = DBConnection.getConnection();
            String sql = "SELECT AVG(credit) AS avg_credit FROM Course";
            pstmt = conn.prepareStatement(sql);
            rs = pstmt.executeQuery();
            
            if (rs.next()) {
                avgCredit = rs.getBigDecimal("avg_credit");
                if (avgCredit == null) {
                    avgCredit = BigDecimal.ZERO;
                }
            }
        } catch (SQLException e) {
            System.err.println("[CourseDAO.getAverageCourseCredit] SQLException: " + e.getMessage());
            e.printStackTrace();
        } finally {
            DBConnection.close(conn, pstmt, rs);
        }
        
        return avgCredit;
    }
    
    /**
     * 获取选课人数最多的课程
     * @param limit 返回的课程数量
     * @return 包含课程信息和选课人数的列表
     */
    public List<Object[]> getMostPopularCourses(int limit) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        List<Object[]> popularCourses = new ArrayList<>();
        
        try {
            conn = DBConnection.getConnection();
            String sql = "SELECT c.course_id, c.course_name, COUNT(e.student_id) AS enrollment_count " +
                     "FROM Course c " +
                     "LEFT JOIN Enrollment e ON c.course_id = e.course_id " +
                     "GROUP BY c.course_id, c.course_name " +
                     "ORDER BY enrollment_count DESC " +
                     "LIMIT ?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, limit);
            rs = pstmt.executeQuery();
            
            while (rs.next()) {
                String courseId = rs.getString("course_id");
                String courseName = rs.getString("course_name");
                int enrollmentCount = rs.getInt("enrollment_count");
                popularCourses.add(new Object[]{courseId, courseName, enrollmentCount});
            }
        } catch (SQLException e) {
            System.err.println("[CourseDAO.getMostPopularCourses] SQLException: " + e.getMessage());
            e.printStackTrace();
        } finally {
            DBConnection.close(conn, pstmt, rs);
        }
        
        return popularCourses;
    }
    
    /**
     * 搜索课程（支持课程ID、课程名称和院系名称的模糊匹配）
     * @param keyword 搜索关键词
     * @return 匹配的课程列表
     */
    public List<Course> searchCourses(String keyword) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        List<Course> courses = new ArrayList<>();
        
        try {
            conn = DBConnection.getConnection();
            // 构建SQL查询，支持课程ID、课程名称和院系名称的模糊匹配
            String sql = "SELECT c.*, d.dept_name, " +
                    "(SELECT COUNT(*) FROM Enrollment e WHERE e.course_id = c.course_id) AS enrollment_count " +
                    "FROM Course c " +
                    "JOIN Department d ON c.dept_id = d.dept_id " +
                    "WHERE c.course_id LIKE ? OR c.course_name LIKE ? OR d.dept_name LIKE ?";
            
            pstmt = conn.prepareStatement(sql);
            String searchPattern = "%" + keyword + "%";
            pstmt.setString(1, searchPattern);
            pstmt.setString(2, searchPattern);
            pstmt.setString(3, searchPattern);
            
            rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Course course = new Course();
                course.setCourseId(rs.getString("course_id"));
                course.setCourseName(rs.getString("course_name"));
                course.setDeptId(rs.getString("dept_id"));
                course.setCredit(rs.getBigDecimal("credit"));
                course.setDeptName(rs.getString("dept_name"));
                course.setCapacity(rs.getInt("capacity"));
                course.setEnrollmentCount(rs.getInt("enrollment_count"));
                courses.add(course);
            }
        } catch (SQLException e) {
            System.err.println("[CourseDAO.searchCourses] SQLException: " + e.getMessage());
            e.printStackTrace();
        } finally {
            DBConnection.close(conn, pstmt, rs);
        }
        
        return courses;
    }
} 