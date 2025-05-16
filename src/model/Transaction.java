package model;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 交易记录实体类
 */
public class Transaction {
    private Long transactionId;
    private String fromStudentId;
    private String toStudentId;
    private BigDecimal amount;
    private Date transactionTime;
    
    // 用于连接查询的扩展属性
    private String fromStudentName;
    private String toStudentName;
    
    public Transaction() {
    }
    
    public Transaction(Long transactionId, String fromStudentId, String toStudentId, BigDecimal amount, Date transactionTime) {
        this.transactionId = transactionId;
        this.fromStudentId = fromStudentId;
        this.toStudentId = toStudentId;
        this.amount = amount;
        this.transactionTime = transactionTime;
    }
    
    public Long getTransactionId() {
        return transactionId;
    }
    
    public void setTransactionId(Long transactionId) {
        this.transactionId = transactionId;
    }
    
    public String getFromStudentId() {
        return fromStudentId;
    }
    
    public void setFromStudentId(String fromStudentId) {
        this.fromStudentId = fromStudentId;
    }
    
    public String getToStudentId() {
        return toStudentId;
    }
    
    public void setToStudentId(String toStudentId) {
        this.toStudentId = toStudentId;
    }
    
    public BigDecimal getAmount() {
        return amount;
    }
    
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
    
    public Date getTransactionTime() {
        return transactionTime;
    }
    
    public void setTransactionTime(Date transactionTime) {
        this.transactionTime = transactionTime;
    }
    
    public String getFromStudentName() {
        return fromStudentName;
    }
    
    public void setFromStudentName(String fromStudentName) {
        this.fromStudentName = fromStudentName;
    }
    
    public String getToStudentName() {
        return toStudentName;
    }
    
    public void setToStudentName(String toStudentName) {
        this.toStudentName = toStudentName;
    }
    
    @Override
    public String toString() {
        return "Transaction{" +
                "transactionId=" + transactionId +
                ", fromStudentId='" + fromStudentId + '\'' +
                ", toStudentId='" + toStudentId + '\'' +
                ", amount=" + amount +
                ", transactionTime=" + transactionTime +
                ", fromStudentName='" + fromStudentName + '\'' +
                ", toStudentName='" + toStudentName + '\'' +
                '}';
    }
} 