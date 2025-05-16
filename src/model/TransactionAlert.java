package model;

import java.util.Date;

/**
 * 交易风控警报模型类
 */
public class TransactionAlert {
    private int alertId;            // 警报ID
    private String studentId;       // 学生ID
    private Date alertTime;         // 警报时间
    private String alertType;       // 警报类型：FREQUENCY(频率), AMOUNT(金额), BOTH(两者)
    private String description;     // 警报描述
    private boolean resolved;       // 是否已处理
    private String resolvedBy;      //, 处理人ID
    private Date resolvedAt;        // 处理时间
    
    // 以下为临时属性，非数据库字段，用于前端展示
    private String studentName;     // 学生姓名
    private String resolverName;    // 处理人姓名
    
    public TransactionAlert() {
    }
    
    public TransactionAlert(String studentId, String alertType, String description) {
        this.studentId = studentId;
        this.alertType = alertType;
        this.description = description;
        this.resolved = false;
    }
    
    // Getters and Setters
    public int getAlertId() {
        return alertId;
    }
    
    public void setAlertId(int alertId) {
        this.alertId = alertId;
    }
    
    public String getStudentId() {
        return studentId;
    }
    
    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }
    
    public Date getAlertTime() {
        return alertTime;
    }
    
    public void setAlertTime(Date alertTime) {
        this.alertTime = alertTime;
    }
    
    public String getAlertType() {
        return alertType;
    }
    
    public void setAlertType(String alertType) {
        this.alertType = alertType;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public boolean isResolved() {
        return resolved;
    }
    
    public void setResolved(boolean resolved) {
        this.resolved = resolved;
    }
    
    public String getResolvedBy() {
        return resolvedBy;
    }
    
    public void setResolvedBy(String resolvedBy) {
        this.resolvedBy = resolvedBy;
    }
    
    public Date getResolvedAt() {
        return resolvedAt;
    }
    
    public void setResolvedAt(Date resolvedAt) {
        this.resolvedAt = resolvedAt;
    }
    
    public String getStudentName() {
        return studentName;
    }
    
    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }
    
    public String getResolverName() {
        return resolverName;
    }
    
    public void setResolverName(String resolverName) {
        this.resolverName = resolverName;
    }
} 