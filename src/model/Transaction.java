package model;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * 交易记录实体类
 */
public class Transaction {

    public enum TransactionType {
        DEPOSIT,    // 存款
        WITHDRAW,   // 取款
        TRANSFER,   // 转账 (包括转出和转入，方向由 relatedStudentId 和 amount 符号决定，或单独字段)
        PAYMENT,    // 支付 (例如购买课程)
        REFUND,     // 退款
        EXPENSE     // 其他支出
    }

    private String transactionId;     // 将transactionId从Long改为String
    private String studentId;          // 该交易主要关联的学生ID
    private TransactionType type;      // 交易类型
    private BigDecimal amount;         // 交易金额 (对于转出/支付可以为负，或始终为正，由类型决定)
    private Timestamp transactionDate;    // 交易发生时间 (DAO中设置)
    private String description;        // 交易描述
    private String relatedStudentId;   // 关联的另一方学生ID (例如转账目标，或支付对象)
    private String relatedUserId; // 关联用户ID，用于转账等场景
    // 状态字段，例如 PENDING, COMPLETED, FAILED, CANCELLED
    // public enum TransactionStatus { PENDING, COMPLETED, FAILED, CANCELLED }
    // private TransactionStatus status;

    // 扩展属性 (用于显示，由DAO填充)
    private String studentName;        // studentId 对应的姓名
    private String relatedStudentName; // relatedStudentId 对应的姓名

    public Transaction() {
        this.transactionDate = new Timestamp(System.currentTimeMillis()); // Default to now, DAO can override
    }

    // --- Getters and Setters ---

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Timestamp getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(Timestamp transactionDate) {
        this.transactionDate = transactionDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRelatedStudentId() {
        return relatedStudentId;
    }

    public void setRelatedStudentId(String relatedStudentId) {
        this.relatedStudentId = relatedStudentId;
    }

    public String getRelatedUserId() {
        return relatedUserId;
    }

    public void setRelatedUserId(String relatedUserId) {
        this.relatedUserId = relatedUserId;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getRelatedStudentName() {
        return relatedStudentName;
    }

    public void setRelatedStudentName(String relatedStudentName) {
        this.relatedStudentName = relatedStudentName;
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "transactionId=" + transactionId +
                ", studentId='" + studentId + '\'' +
                ", type=" + type +
                ", amount=" + amount +
                ", transactionDate=" + transactionDate +
                ", description='" + description + '\'' +
                ", relatedStudentId='" + relatedStudentId + '\'' +
                ", relatedUserId='" + relatedUserId + '\'' +
                // ", status=" + status +
                ", studentName='" + studentName + '\'' +
                ", relatedStudentName='" + relatedStudentName + '\'' +
                '}';
    }
}