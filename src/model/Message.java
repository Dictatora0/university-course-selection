package src.model;

import java.util.Date;

public class Message {
    private Long messageId;
    private String fromStudentId;
    private String toStudentId;
    private String content;
    private Date sendTime;
    private Boolean isRead;
    
    public Message() {
        this.isRead = false;
    }
    
    public Message(String fromStudentId, String toStudentId, String content) {
        this.fromStudentId = fromStudentId;
        this.toStudentId = toStudentId;
        this.content = content;
        this.sendTime = new Date();
        this.isRead = false;
    }
    
    // Getters and Setters
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
    
    public Boolean getIsRead() {
        return isRead;
    }
    
    public void setIsRead(Boolean isRead) {
        this.isRead = isRead;
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
                '}';
    }
} 