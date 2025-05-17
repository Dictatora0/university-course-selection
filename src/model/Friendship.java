package model;

import java.sql.Timestamp;

/**
 * 好友关系模型类
 */
public class Friendship {
    private String studentId1;
    private String studentId2;
    private Timestamp friendshipDate;
    private String friendName;
    private String friendDepartment;
    // 添加好友状态
    private FriendshipStatus status;
    // 添加请求发送时间
    private Timestamp requestTime;
    // 添加请求确认时间
    private Timestamp confirmTime;
    
    // 定义好友关系状态枚举
    public enum FriendshipStatus {
        PENDING, // 等待确认
        ACCEPTED, // 已接受
        REJECTED, // 已拒绝
        BLOCKED // 已屏蔽
    }
    
    public Friendship() {
    }
    
    public String getStudentId1() {
        return studentId1;
    }
    
    public void setStudentId1(String studentId1) {
        this.studentId1 = studentId1;
    }
    
    public String getStudentId2() {
        return studentId2;
    }
    
    public void setStudentId2(String studentId2) {
        this.studentId2 = studentId2;
    }
    
    public Timestamp getFriendshipDate() {
        return friendshipDate;
    }
    
    public void setFriendshipDate(Timestamp friendshipDate) {
        this.friendshipDate = friendshipDate;
    }
    
    public String getFriendName() {
        return friendName;
    }
    
    public void setFriendName(String friendName) {
        this.friendName = friendName;
    }
    
    public String getFriendDepartment() {
        return friendDepartment;
    }
    
    public void setFriendDepartment(String friendDepartment) {
        this.friendDepartment = friendDepartment;
    }
    
    public FriendshipStatus getStatus() {
        return status;
    }
    
    public void setStatus(FriendshipStatus status) {
        this.status = status;
    }
    
    public Timestamp getRequestTime() {
        return requestTime;
    }
    
    public void setRequestTime(Timestamp requestTime) {
        this.requestTime = requestTime;
    }
    
    public Timestamp getConfirmTime() {
        return confirmTime;
    }
    
    public void setConfirmTime(Timestamp confirmTime) {
        this.confirmTime = confirmTime;
    }
    
    @Override
    public String toString() {
        return "Friendship{" +
                "studentId1='" + studentId1 + '\'' +
                ", studentId2='" + studentId2 + '\'' +
                ", friendshipDate=" + friendshipDate +
                ", friendName='" + friendName + '\'' +
                ", friendDepartment='" + friendDepartment + '\'' +
                ", status=" + status +
                ", requestTime=" + requestTime +
                ", confirmTime=" + confirmTime +
                '}';
    }
} 