package model;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 交易控制模型类，用于设置系统交易限制
 */
public class TransactionControl {
    private int controlId;                    // 控制ID
    private BigDecimal maxSingleAmount;       // 单笔最大金额
    private BigDecimal dailyLimit;            // 日累计限额
    private int maxDailyTransactions;         // 日最大交易次数
    private boolean enabled;                  // 是否启用
    private Date lastUpdated;                 // 最后更新时间
    private String updatedBy;                 // 更新人ID
    
    // 临时属性，非数据库字段
    private String updaterName;               // 更新人姓名
    
    public TransactionControl() {
    }
    
    public TransactionControl(BigDecimal maxSingleAmount, BigDecimal dailyLimit, int maxDailyTransactions) {
        this.maxSingleAmount = maxSingleAmount;
        this.dailyLimit = dailyLimit;
        this.maxDailyTransactions = maxDailyTransactions;
        this.enabled = true;
    }
    
    /**
     * 检查交易金额是否超过单笔限制
     * @param amount 交易金额
     * @return 是否超限
     */
    public boolean isExceedSingleLimit(BigDecimal amount) {
        if (!enabled) return false;
        return amount.compareTo(maxSingleAmount) > 0;
    }
    
    /**
     * 检查是否超过日累计限额
     * @param dailySum 当日已累计金额
     * @param amount 本次交易金额
     * @return 是否超限
     */
    public boolean isExceedDailyLimit(BigDecimal dailySum, BigDecimal amount) {
        if (!enabled) return false;
        return dailySum.add(amount).compareTo(dailyLimit) > 0;
    }
    
    /**
     * 检查是否超过日交易次数
     * @param dailyCount 当日已交易次数
     * @return 是否超限
     */
    public boolean isExceedDailyCount(int dailyCount) {
        if (!enabled) return false;
        return dailyCount >= maxDailyTransactions;
    }
    
    // Getters and Setters
    public int getControlId() {
        return controlId;
    }
    
    public void setControlId(int controlId) {
        this.controlId = controlId;
    }
    
    public BigDecimal getMaxSingleAmount() {
        return maxSingleAmount;
    }
    
    public void setMaxSingleAmount(BigDecimal maxSingleAmount) {
        this.maxSingleAmount = maxSingleAmount;
    }
    
    public BigDecimal getDailyLimit() {
        return dailyLimit;
    }
    
    public void setDailyLimit(BigDecimal dailyLimit) {
        this.dailyLimit = dailyLimit;
    }
    
    public int getMaxDailyTransactions() {
        return maxDailyTransactions;
    }
    
    public void setMaxDailyTransactions(int maxDailyTransactions) {
        this.maxDailyTransactions = maxDailyTransactions;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public Date getLastUpdated() {
        return lastUpdated;
    }
    
    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
    
    public String getUpdatedBy() {
        return updatedBy;
    }
    
    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }
    
    public String getUpdaterName() {
        return updaterName;
    }
    
    public void setUpdaterName(String updaterName) {
        this.updaterName = updaterName;
    }
} 