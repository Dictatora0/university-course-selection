package model;

import java.util.Date;

/**
 * 消息实体类
 */
public class Message {
    private Long messageId;
    private String fromStudentId;
    private String toStudentId;
    private String content;
    private Date sendTime;
    private boolean isRead;
    
    // 用于连接查询的扩展属性
    private String fromStudentName;
    private String toStudentName;
    
    public Message() {
    }
    
    public Message(Long messageId, String fromStudentId, String toStudentId, String content, Date sendTime, boolean isRead) {
        this.messageId = messageId;
        this.fromStudentId = fromStudentId;
        this.toStudentId = toStudentId;
        this.content = content;
        this.sendTime = sendTime;
        this.isRead = isRead;
    }
    
    public Long getMessageId() {
        return messageId;
    }
    
    public void setMessageId(Long messageId) {
        this.messageId = messageId;
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
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public Date getSendTime() {
        return sendTime;
    }
    
    public void setSendTime(Date sendTime) {
        this.sendTime = sendTime;
    }
    
    public boolean isRead() {
        return isRead;
    }
    
    public void setRead(boolean read) {
        isRead = read;
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
        return "Message{" +
                "messageId=" + messageId +
                ", fromStudentId='" + fromStudentId + '\'' +
                ", toStudentId='" + toStudentId + '\'' +
                ", content='" + content + '\'' +
                ", sendTime=" + sendTime +
                ", isRead=" + isRead +
                ", fromStudentName='" + fromStudentName + '\'' +
                ", toStudentName='" + toStudentName + '\'' +
                '}';
    }
} 