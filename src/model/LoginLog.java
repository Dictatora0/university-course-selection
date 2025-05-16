package model;

import java.util.Date;

/**
 * 登录日志实体类
 */
public class LoginLog {
    private Long logId;
    private String studentId;
    private Date loginTime;
    private String ipAddress;
    
    // 用于连接查询的扩展属性
    private String studentName;
    
    public LoginLog() {
    }
    
    public LoginLog(Long logId, String studentId, Date loginTime, String ipAddress) {
        this.logId = logId;
        this.studentId = studentId;
        this.loginTime = loginTime;
        this.ipAddress = ipAddress;
    }
    
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
    
    public String getStudentName() {
        return studentName;
    }
    
    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }
    
    @Override
    public String toString() {
        return "LoginLog{" +
                "logId=" + logId +
                ", studentId='" + studentId + '\'' +
                ", loginTime=" + loginTime +
                ", ipAddress='" + ipAddress + '\'' +
                ", studentName='" + studentName + '\'' +
                '}';
    }
} 