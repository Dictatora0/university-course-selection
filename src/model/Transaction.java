package src.model;

import java.util.Date;

public class Transaction {
    private Long transactionId;
    private String fromStudentId;
    private String toStudentId;
    private Double amount;
    private Date transactionTime;
    
    public Transaction() {
    }
    
    public Transaction(String fromStudentId, String toStudentId, Double amount) {
        this.fromStudentId = fromStudentId;
        this.toStudentId = toStudentId;
        this.amount = amount;
        this.transactionTime = new Date();
    }
    
    // Getters and Setters
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
    
    public Double getAmount() {
        return amount;
    }
    
    public void setAmount(Double amount) {
        this.amount = amount;
    }
    
    public Date getTransactionTime() {
        return transactionTime;
    }
    
    public void setTransactionTime(Date transactionTime) {
        this.transactionTime = transactionTime;
    }
    
    @Override
    public String toString() {
        return "Transaction{" +
                "transactionId=" + transactionId +
                ", fromStudentId='" + fromStudentId + '\'' +
                ", toStudentId='" + toStudentId + '\'' +
                ", amount=" + amount +
                ", transactionTime=" + transactionTime +
                '}';
    }
} 