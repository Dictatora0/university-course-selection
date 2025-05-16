package src.dao;

import src.model.Course;
import src.model.Enrollment;
import src.model.Student;
import src.util.DBUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EnrollmentDAO {
    
    // 添加选课记录
    public boolean addEnrollment(Enrollment enrollment) throws SQLException {
        String sql = "INSERT INTO Enrollment(student_id, course_id, enrollment_date) VALUES(?,?,?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, enrollment.getStudentId());
            pstmt.setString(2, enrollment.getCourseId());
            pstmt.setTimestamp(3, new Timestamp(enrollment.getEnrollmentDate().getTime()));
            return pstmt.executeUpdate() > 0;
        }
    }
    
    // 更新成绩
    public boolean updateGrade(String studentId, String courseId, Double grade) throws SQLException {
        String sql = "UPDATE Enrollment SET grade = ? WHERE student_id = ? AND course_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, grade);
            pstmt.setString(2, studentId);
            pstmt.setString(3, courseId);
            return pstmt.executeUpdate() > 0;
        }
    }
    
    // 查询学生选修的课程
    public List<Course> getCoursesForStudent(String studentId) throws SQLException {
        String sql = "SELECT c.* FROM Course c " +
                     "JOIN Enrollment e ON c.course_id = e.course_id " +
                     "WHERE e.student_id = ?";
        List<Course> courses = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, studentId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Course course = new Course();
                    course.setCourseId(rs.getString("course_id"));
                    course.setCourseName(rs.getString("course_name"));
                    course.setDeptId(rs.getString("dept_id"));
                    course.setCredit(rs.getDouble("credit"));
                    courses.add(course);
                }
            }
        }
        return courses;
    }
    
    // 查询选修某课程的学生
    public List<Student> getStudentsForCourse(String courseId) throws SQLException {
        String sql = "SELECT s.* FROM Student s " +
                     "JOIN Enrollment e ON s.student_id = e.student_id " +
                     "WHERE e.course_id = ?";
        List<Student> students = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, courseId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Student student = new Student();
                    student.setStudentId(rs.getString("student_id"));
                    student.setName(rs.getString("name"));
                    student.setBirthDate(rs.getDate("birth_date"));
                    student.setIdCard(rs.getString("id_card"));
                    student.setAddress(rs.getString("address"));
                    student.setCreatedAt(rs.getTimestamp("created_at"));
                    students.add(student);
                }
            }
        }
        return students;
    }
    
    // 查询学生的成绩单
    public List<Enrollment> getStudentGrades(String studentId) throws SQLException {
        String sql = "SELECT e.*, c.course_name FROM Enrollment e " +
                     "JOIN Course c ON e.course_id = c.course_id " +
                     "WHERE e.student_id = ?";
        List<Enrollment> enrollments = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, studentId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Enrollment enrollment = new Enrollment();
                    enrollment.setStudentId(rs.getString("student_id"));
                    enrollment.setCourseId(rs.getString("course_id"));
                    enrollment.setGrade(rs.getDouble("grade"));
                    enrollment.setEnrollmentDate(rs.getTimestamp("enrollment_date"));
                    enrollments.add(enrollment);
                }
            }
        }
        return enrollments;
    }
    
    // 检查学生是否已选修某课程
    public boolean isEnrolled(String studentId, String courseId) throws SQLException {
        String sql = "SELECT 1 FROM Enrollment WHERE student_id = ? AND course_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, studentId);
            pstmt.setString(2, courseId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        }
    }
    
    // 删除选课记录
    public boolean deleteEnrollment(String studentId, String courseId) throws SQLException {
        String sql = "DELETE FROM Enrollment WHERE student_id = ? AND course_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, studentId);
            pstmt.setString(2, courseId);
            return pstmt.executeUpdate() > 0;
        }
    }
} 