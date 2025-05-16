package src.model;

public class Course {
    private String courseId;
    private String courseName;
    private String deptId;
    private double credit;
    
    public Course() {
    }
    
    public Course(String courseId, String courseName, String deptId, double credit) {
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
    
    public double getCredit() {
        return credit;
    }
    
    public void setCredit(double credit) {
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