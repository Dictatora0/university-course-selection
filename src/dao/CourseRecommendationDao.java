package dao;

import model.CourseRecommendation;
import util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 课程推荐数据访问对象
 */
public class CourseRecommendationDao {
    private static final Logger LOGGER = Logger.getLogger(CourseRecommendationDao.class.getName());
    
    /**
     * 添加课程推荐
     * @param recommendation 推荐对象
     * @return 新推荐ID，失败返回-1
     */
    public int addRecommendation(CourseRecommendation recommendation) {
        String sql = "INSERT INTO CourseRecommendation " +
                     "(student_id, course_id, recommend_score, reason, created_at, viewed, accepted) " +
                     "VALUES (?, ?, ?, ?, NOW(), 0, 0)";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, recommendation.getStudentId());
            stmt.setString(2, recommendation.getCourseId());
            stmt.setDouble(3, recommendation.getRecommendScore());
            stmt.setString(4, recommendation.getReason());
            
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "添加课程推荐失败", e);
        }
        
        return -1;
    }
    
    /**
     * 获取学生的课程推荐
     * @param studentId 学生ID
     * @return 推荐列表
     */
    public List<CourseRecommendation> getRecommendationsForStudent(String studentId) {
        List<CourseRecommendation> recommendations = new ArrayList<>();
        
        String sql = "SELECT r.*, s.name as student_name, c.course_name, " +
                     "t.name as course_teacher, c.course_time as course_time_info " +
                     "FROM CourseRecommendation r " +
                     "JOIN Student s ON r.student_id = s.student_id " +
                     "JOIN Course c ON r.course_id = c.course_id " +
                     "JOIN Teacher t ON c.teacher_id = t.teacher_id " +
                     "WHERE r.student_id = ? " +
                     "ORDER BY r.created_at DESC";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, studentId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    recommendations.add(extractRecommendationFromResultSet(rs));
                }
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "获取学生课程推荐失败", e);
        }
        
        return recommendations;
    }
    
    /**
     * 将推荐标记为已查看
     * @param recommendationId 推荐ID
     * @return 是否成功
     */
    public boolean markAsViewed(int recommendationId) {
        String sql = "UPDATE CourseRecommendation SET viewed = 1, viewed_at = NOW() " +
                     "WHERE recommendation_id = ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, recommendationId);
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "标记课程推荐为已查看失败", e);
            return false;
        }
    }
    
    /**
     * 将推荐标记为已接受（学生选择了该课程）
     * @param recommendationId 推荐ID
     * @return 是否成功
     */
    public boolean markAsAccepted(int recommendationId) {
        String sql = "UPDATE CourseRecommendation SET accepted = 1, accepted_at = NOW() " +
                     "WHERE recommendation_id = ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, recommendationId);
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "标记课程推荐为已接受失败", e);
            return false;
        }
    }
    
    /**
     * 获取指定课程的推荐情况（用于统计分析）
     * @param courseId 课程ID
     * @return 推荐列表
     */
    public List<CourseRecommendation> getRecommendationsForCourse(String courseId) {
        List<CourseRecommendation> recommendations = new ArrayList<>();
        
        String sql = "SELECT r.*, s.name as student_name, c.course_name, " +
                     "t.name as course_teacher, c.course_time as course_time_info " +
                     "FROM CourseRecommendation r " +
                     "JOIN Student s ON r.student_id = s.student_id " +
                     "JOIN Course c ON r.course_id = c.course_id " +
                     "JOIN Teacher t ON c.teacher_id = t.teacher_id " +
                     "WHERE r.course_id = ? " +
                     "ORDER BY r.created_at DESC";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, courseId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    recommendations.add(extractRecommendationFromResultSet(rs));
                }
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "获取课程推荐统计失败", e);
        }
        
        return recommendations;
    }
    
    /**
     * 获取推荐接受率最高的课程IDs
     * @param limit 返回数量
     * @return 课程ID列表
     */
    public List<String> getMostAcceptedCourseIds(int limit) {
        List<String> courseIds = new ArrayList<>();
        
        String sql = "SELECT course_id, COUNT(*) as total_recommendations, " +
                     "SUM(CASE WHEN accepted = 1 THEN 1 ELSE 0 END) as accepted_count, " +
                     "(SUM(CASE WHEN accepted = 1 THEN 1 ELSE 0 END) / COUNT(*)) as acceptance_rate " +
                     "FROM CourseRecommendation " +
                     "GROUP BY course_id " +
                     "HAVING COUNT(*) >= 5 " + // 至少有5个推荐记录
                     "ORDER BY acceptance_rate DESC " +
                     "LIMIT ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, limit);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    courseIds.add(rs.getString("course_id"));
                }
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "获取推荐接受率最高的课程失败", e);
        }
        
        return courseIds;
    }
    
    /**
     * 根据专业和学生已选课程生成推荐
     * @param studentId 学生ID
     * @param majorId 专业ID
     * @param limit 推荐数量
     * @return 成功生成的推荐数量
     */
    public int generateRecommendationsForStudent(String studentId, String majorId, int limit) {
        int count = 0;
        
        // 1. 根据专业相关课程进行推荐
        String majorSql = "SELECT c.course_id, c.course_name, " +
                          "0.8 as recommend_score, " + // 设置专业相关课程的推荐分数
                          "'基于您的专业（' || d.dept_name || '）推荐' as reason " +
                          "FROM Course c " +
                          "JOIN Department d ON c.dept_id = d.dept_id " +
                          "WHERE d.dept_id = ? " +
                          "AND c.course_id NOT IN (SELECT course_id FROM Enrollment WHERE student_id = ?) " +
                          "AND c.course_id NOT IN (SELECT course_id FROM CourseRecommendation WHERE student_id = ?) " +
                          "LIMIT ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(majorSql)) {
            
            stmt.setString(1, majorId);
            stmt.setString(2, studentId);
            stmt.setString(3, studentId);
            stmt.setInt(4, limit / 2); // 一半配额给专业相关课程
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    CourseRecommendation rec = new CourseRecommendation();
                    rec.setStudentId(studentId);
                    rec.setCourseId(rs.getString("course_id"));
                    rec.setRecommendScore(rs.getDouble("recommend_score"));
                    rec.setReason(rs.getString("reason"));
                    
                    if (addRecommendation(rec) > 0) {
                        count++;
                    }
                }
            }
            
            // 2. 根据热门课程进行推荐
            String popularSql = "SELECT c.course_id, c.course_name, " +
                               "0.6 as recommend_score, " + // 设置热门课程的推荐分数
                               "'热门课程，已有' || COUNT(e.student_id) || '名学生选择' as reason " +
                               "FROM Course c " +
                               "JOIN Enrollment e ON c.course_id = e.course_id " +
                               "WHERE c.course_id NOT IN (SELECT course_id FROM Enrollment WHERE student_id = ?) " +
                               "AND c.course_id NOT IN (SELECT course_id FROM CourseRecommendation WHERE student_id = ?) " +
                               "GROUP BY c.course_id, c.course_name " +
                               "ORDER BY COUNT(e.student_id) DESC " +
                               "LIMIT ?";
            
            try (PreparedStatement popStmt = conn.prepareStatement(popularSql)) {
                popStmt.setString(1, studentId);
                popStmt.setString(2, studentId);
                popStmt.setInt(3, limit - count); // 剩余配额给热门课程
                
                try (ResultSet rs = popStmt.executeQuery()) {
                    while (rs.next()) {
                        CourseRecommendation rec = new CourseRecommendation();
                        rec.setStudentId(studentId);
                        rec.setCourseId(rs.getString("course_id"));
                        rec.setRecommendScore(rs.getDouble("recommend_score"));
                        rec.setReason(rs.getString("reason"));
                        
                        if (addRecommendation(rec) > 0) {
                            count++;
                        }
                    }
                }
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "为学生生成课程推荐失败", e);
        }
        
        return count;
    }
    
    /**
     * 从ResultSet中提取推荐对象的辅助方法
     * @param rs 结果集
     * @return 推荐对象
     * @throws SQLException 如果数据库访问出错
     */
    private CourseRecommendation extractRecommendationFromResultSet(ResultSet rs) throws SQLException {
        CourseRecommendation rec = new CourseRecommendation();
        rec.setRecommendationId(rs.getInt("recommendation_id"));
        rec.setStudentId(rs.getString("student_id"));
        rec.setCourseId(rs.getString("course_id"));
        rec.setRecommendScore(rs.getDouble("recommend_score"));
        rec.setReason(rs.getString("reason"));
        rec.setCreatedAt(rs.getTimestamp("created_at"));
        rec.setViewed(rs.getBoolean("viewed"));
        rec.setViewedAt(rs.getTimestamp("viewed_at"));
        rec.setAccepted(rs.getBoolean("accepted"));
        rec.setAcceptedAt(rs.getTimestamp("accepted_at"));
        
        // 临时属性
        rec.setStudentName(rs.getString("student_name"));
        rec.setCourseName(rs.getString("course_name"));
        rec.setCourseTeacher(rs.getString("course_teacher"));
        rec.setCourseTimeInfo(rs.getString("course_time_info"));
        
        return rec;
    }
} 