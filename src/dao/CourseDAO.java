package src.dao;

import src.model.Course;
import src.util.DBUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CourseDAO {
    
    // 添加课程
    public boolean addCourse(Course course) throws SQLException {
        String sql = "INSERT INTO Course(course_id, course_name, dept_id, credit) VALUES(?,?,?,?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, course.getCourseId());
            pstmt.setString(2, course.getCourseName());
            pstmt.setString(3, course.getDeptId());
            pstmt.setDouble(4, course.getCredit());
            return pstmt.executeUpdate() > 0;
        }
    }
    
    // 根据课程号查询课程
    public Course getCourseById(String courseId) throws SQLException {
        String sql = "SELECT * FROM Course WHERE course_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, courseId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToCourse(rs);
                }
            }
        }
        return null;
    }
    
    // 查询所有课程
    public List<Course> getAllCourses() throws SQLException {
        String sql = "SELECT * FROM Course";
        List<Course> courses = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                courses.add(mapResultSetToCourse(rs));
            }
        }
        return courses;
    }
    
    // 根据院系ID查询课程
    public List<Course> getCoursesByDepartment(String deptId) throws SQLException {
        String sql = "SELECT * FROM Course WHERE dept_id = ?";
        List<Course> courses = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, deptId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    courses.add(mapResultSetToCourse(rs));
                }
            }
        }
        return courses;
    }
    
    // 更新课程信息
    public boolean updateCourse(Course course) throws SQLException {
        String sql = "UPDATE Course SET course_name = ?, dept_id = ?, credit = ? WHERE course_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, course.getCourseName());
            pstmt.setString(2, course.getDeptId());
            pstmt.setDouble(3, course.getCredit());
            pstmt.setString(4, course.getCourseId());
            return pstmt.executeUpdate() > 0;
        }
    }
    
    // 删除课程
    public boolean deleteCourse(String courseId) throws SQLException {
        String sql = "DELETE FROM Course WHERE course_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, courseId);
            return pstmt.executeUpdate() > 0;
        }
    }
    
    // 将ResultSet映射为Course对象
    private Course mapResultSetToCourse(ResultSet rs) throws SQLException {
        Course course = new Course();
        course.setCourseId(rs.getString("course_id"));
        course.setCourseName(rs.getString("course_name"));
        course.setDeptId(rs.getString("dept_id"));
        course.setCredit(rs.getDouble("credit"));
        return course;
    }
} 