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
    private String deptName;   // 院系名称
    private int capacity;      // 课程容量
    private int enrollmentCount; // 已选人数
    private String description; // 课程描述
    
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
    
    public String getDeptName() {
        return deptName;
    }
    
    public void setDeptName(String deptName) {
        this.deptName = deptName;
    }
    
    public int getCapacity() {
        return capacity;
    }
    
    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }
    
    public int getEnrollmentCount() {
        return enrollmentCount;
    }
    
    public void setEnrollmentCount(int enrollmentCount) {
        this.enrollmentCount = enrollmentCount;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    @Override
    public String toString() {
        return "Course{" +
                "courseId='" + courseId + '\'' +
                ", courseName='" + courseName + '\'' +
                ", deptId='" + deptId + '\'' +
                ", credit=" + credit +
                ", deptName='" + deptName + '\'' +
                ", capacity=" + capacity +
                ", enrollmentCount=" + enrollmentCount +
                ", description='" + description + '\'' +
                '}';
    }
} 