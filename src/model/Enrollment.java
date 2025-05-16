package model;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 选课记录实体类
 */
public class Enrollment {
    private String studentId;
    private String courseId;
    private BigDecimal grade;
    private Date enrollmentDate;
    
    // 用于连接查询的扩展属性
    private String courseName;
    private BigDecimal credit;
    private String deptName;
    
    public Enrollment() {
    }
    
    public Enrollment(String studentId, String courseId, BigDecimal grade, Date enrollmentDate) {
        this.studentId = studentId;
        this.courseId = courseId;
        this.grade = grade;
        this.enrollmentDate = enrollmentDate;
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
    
    public BigDecimal getGrade() {
        return grade;
    }
    
    public void setGrade(BigDecimal grade) {
        this.grade = grade;
    }
    
    public Date getEnrollmentDate() {
        return enrollmentDate;
    }
    
    public void setEnrollmentDate(Date enrollmentDate) {
        this.enrollmentDate = enrollmentDate;
    }
    
    public String getCourseName() {
        return courseName;
    }
    
    public void setCourseName(String courseName) {
        this.courseName = courseName;
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
    
    @Override
    public String toString() {
        return "Enrollment{" +
                "studentId='" + studentId + '\'' +
                ", courseId='" + courseId + '\'' +
                ", grade=" + grade +
                ", enrollmentDate=" + enrollmentDate +
                ", courseName='" + courseName + '\'' +
                ", credit=" + credit +
                ", deptName='" + deptName + '\'' +
                '}';
    }
} 