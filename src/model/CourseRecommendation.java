package model;

import java.util.Date;

/**
 * 课程推荐模型类
 */
public class CourseRecommendation {
    private int recommendationId;          // 推荐ID
    private String studentId;              // 学生ID
    private String courseId;               // 课程ID
    private double recommendScore;         // 推荐分数
    private String reason;                 // 推荐理由
    private Date createdAt;                // 创建时间
    private boolean viewed;                // 是否已查看
    private Date viewedAt;                 // 查看时间
    private boolean accepted;              // 是否已接受（选课）
    private Date acceptedAt;               // 接受时间
    
    // 临时属性，非数据库字段，用于前端展示
    private String studentName;            // 学生姓名
    private String courseName;             // 课程名称
    private String courseTeacher;          // 课程教师
    private String courseTimeInfo;         // 课程时间信息
    
    public CourseRecommendation() {
    }
    
    public CourseRecommendation(String studentId, String courseId, double recommendScore, String reason) {
        this.studentId = studentId;
        this.courseId = courseId;
        this.recommendScore = recommendScore;
        this.reason = reason;
        this.viewed = false;
        this.accepted = false;
    }
    
    /**
     * 课程推荐的类型（基于不同算法或来源）
     */
    public enum RecommendationType {
        FRIEND_SELECTION("好友选择"),
        HISTORY_INTERESTS("历史兴趣"),
        MAJOR_RELEVANT("专业相关"),
        POPULAR_COURSES("热门课程");
        
        private final String description;
        
        RecommendationType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    // Getters and Setters
    public int getRecommendationId() {
        return recommendationId;
    }
    
    public void setRecommendationId(int recommendationId) {
        this.recommendationId = recommendationId;
    }
    
    public String getStudentId() {
        return studentId;
    }
    
    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }
    
    public String getCourseId() {
        return courseId;
    }
    
    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }
    
    public double getRecommendScore() {
        return recommendScore;
    }
    
    public void setRecommendScore(double recommendScore) {
        this.recommendScore = recommendScore;
    }
    
    public String getReason() {
        return reason;
    }
    
    public void setReason(String reason) {
        this.reason = reason;
    }
    
    public Date getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
    
    public boolean isViewed() {
        return viewed;
    }
    
    public void setViewed(boolean viewed) {
        this.viewed = viewed;
    }
    
    public Date getViewedAt() {
        return viewedAt;
    }
    
    public void setViewedAt(Date viewedAt) {
        this.viewedAt = viewedAt;
    }
    
    public boolean isAccepted() {
        return accepted;
    }
    
    public void setAccepted(boolean accepted) {
        this.accepted = accepted;
    }
    
    public Date getAcceptedAt() {
        return acceptedAt;
    }
    
    public void setAcceptedAt(Date acceptedAt) {
        this.acceptedAt = acceptedAt;
    }
    
    public String getStudentName() {
        return studentName;
    }
    
    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }
    
    public String getCourseName() {
        return courseName;
    }
    
    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }
    
    public String getCourseTeacher() {
        return courseTeacher;
    }
    
    public void setCourseTeacher(String courseTeacher) {
        this.courseTeacher = courseTeacher;
    }
    
    public String getCourseTimeInfo() {
        return courseTimeInfo;
    }
    
    public void setCourseTimeInfo(String courseTimeInfo) {
        this.courseTimeInfo = courseTimeInfo;
    }
} 