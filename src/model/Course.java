package model;

import java.math.BigDecimal;

/**
 * 课程实体类
 */
public class Course {
    private String courseId;
    private String courseName;
    private String deptId;
    private BigDecimal credit;
    
    public Course() {
    }
    
    public Course(String courseId, String courseName, String deptId, BigDecimal credit) {
        this.courseId = courseId;
        this.courseName = courseName;
        this.deptId = deptId;
        this.credit = credit;
    }
    
    // Getters and Setters
    public String getCourseId() {
        return courseId;
    }
    
    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }
    
    public String getCourseName() {
        return courseName;
    }
    
    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }
    
    public String getDeptId() {
        return deptId;
    }
    
    public void setDeptId(String deptId) {
        this.deptId = deptId;
    }
    
    public BigDecimal getCredit() {
        return credit;
    }
    
    public void setCredit(BigDecimal credit) {
        this.credit = credit;
    }
    
    @Override
    public String toString() {
        return "Course{" +
                "courseId='" + courseId + '\'' +
                ", courseName='" + courseName + '\'' +
                ", deptId='" + deptId + '\'' +
                ", credit=" + credit +
                '}';
    }
} 