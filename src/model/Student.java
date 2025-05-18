package model;

import com.google.gson.annotations.SerializedName;
import java.util.Date;
import java.sql.Timestamp;

/**
 * 学生实体类
 */
public class Student {
    @SerializedName("student_id")
    private String studentId;
    private String name;
    @SerializedName("birth_date")
    private Date birthDate;
    @SerializedName("id_card")
    private String idCard;
    private String address;
    private String password;
    @SerializedName("created_at")
    private Date createdAt;
    private double balance;
    @SerializedName("department_id")
    private String deptId;
    @SerializedName("dept_name")
    private String deptName;
    @SerializedName("status")
    private String status; // 额外字段，用于标识好友关系状态
    @SerializedName("recommend_reason")
    private String recommendReason; // 推荐理由
    @SerializedName("reject_time")
    private Timestamp rejectTime; // 拒绝时间
    
    public Student() {
    }
    
    public Student(String studentId, String name, Date birthDate, String idCard, String address, String password, Date createdAt) {
        this.studentId = studentId;
        this.name = name;
        this.birthDate = birthDate;
        this.idCard = idCard;
        this.address = address;
        this.password = password;
        this.createdAt = createdAt;
    }
    
    // Getters and Setters
    public String getStudentId() {
        return studentId;
    }
    
    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public Date getBirthDate() {
        return birthDate;
    }
    
    public void setBirthDate(Date birthDate) {
        this.birthDate = birthDate;
    }
    
    public String getIdCard() {
        return idCard;
    }
    
    public void setIdCard(String idCard) {
        this.idCard = idCard;
    }
    
    public String getAddress() {
        return address;
    }
    
    public void setAddress(String address) {
        this.address = address;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public Date getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
    
    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }
    
    public String getDeptId() {
        return deptId;
    }
    
    public void setDeptId(String deptId) {
        this.deptId = deptId;
    }
    
    public String getDeptName() {
        return deptName;
    }
    
    public void setDeptName(String deptName) {
        this.deptName = deptName;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getRecommendReason() {
        return recommendReason;
    }
    
    public void setRecommendReason(String recommendReason) {
        this.recommendReason = recommendReason;
    }
    
    public Timestamp getRejectTime() {
        return rejectTime;
    }
    
    public void setRejectTime(Timestamp rejectTime) {
        this.rejectTime = rejectTime;
    }
    
    @Override
    public String toString() {
        return "Student{" +
                "studentId='" + studentId + '\'' +
                ", name='" + name + '\'' +
                ", birthDate=" + birthDate +
                ", idCard='" + idCard + '\'' +
                ", address='" + address + '\'' +
                ", deptId='" + deptId + '\'' +
                ", deptName='" + deptName + '\'' +
                ", status='" + status + '\'' +
                ", balance=" + balance +
                ", recommendReason='" + recommendReason + '\'' +
                ", rejectTime=" + rejectTime +
                ", createdAt=" + createdAt +
                '}';
    }
} 