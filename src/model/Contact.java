package model;

import java.sql.Timestamp;

/**
 * 联系人模型类，用于表示聊天记录的联系人信息
 */
public class Contact {
    private String studentId;   // 联系人学号
    private String name;        // 联系人姓名
    private String deptName;    // 联系人所属院系
    private String lastMessage; // 最后一条消息内容
    private Timestamp lastMessageTime; // 最后一条消息时间
    private int unreadCount;    // 未读消息数量

    public Contact() {
    }

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

    public String getDeptName() {
        return deptName;
    }

    public void setDeptName(String deptName) {
        this.deptName = deptName;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public Timestamp getLastMessageTime() {
        return lastMessageTime;
    }

    public void setLastMessageTime(Timestamp lastMessageTime) {
        this.lastMessageTime = lastMessageTime;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }
} 