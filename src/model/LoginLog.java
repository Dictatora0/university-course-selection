package src.model;

import java.util.Date;

public class LoginLog {
    private Long logId;
    private String studentId;
    private Date loginTime;
    private String ipAddress;
    
    public LoginLog() {
    }
    
    public LoginLog(String studentId, String ipAddress) {
        this.studentId = studentId;
        this.ipAddress = ipAddress;
        this.loginTime = new Date();
    }
    
    // Getters and Setters
    public Long getLogId() {
        return logId;
    }
    
    public void setLogId(Long logId) {
        this.logId = logId;
    }
    
    public String getStudentId() {
        return studentId;
    }
    
    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }
    
    public Date getLoginTime() {
        return loginTime;
    }
    
    public void setLoginTime(Date loginTime) {
        this.loginTime = loginTime;
    }
    
    public String getIpAddress() {
        return ipAddress;
    }
    
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
    
    @Override
    public String toString() {
        return "LoginLog{" +
                "logId=" + logId +
                ", studentId='" + studentId + '\'' +
                ", loginTime=" + loginTime +
                ", ipAddress='" + ipAddress + '\'' +
                '}';
    }
} 