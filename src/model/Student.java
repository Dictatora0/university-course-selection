package model;

import java.util.Date;

/**
 * 学生实体类
 */
public class Student {
    private String studentId;
    private String name;
    private Date birthDate;
    private String idCard;
    private String address;
    private String password;
    private Date createdAt;
    private double balance;
    private String deptId;
    
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
    
    @Override
    public String toString() {
        return "Student{" +
                "studentId='" + studentId + '\'' +
                ", name='" + name + '\'' +
                ", birthDate=" + birthDate +
                ", idCard='" + idCard + '\'' +
                ", address='" + address + '\'' +
                ", deptId='" + deptId + '\'' +
                ", balance=" + balance +
                ", createdAt=" + createdAt +
                '}';
    }
} 