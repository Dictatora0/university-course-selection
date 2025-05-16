package model;

import java.util.Date;

/**
 * 好友关系实体类
 */
public class Friendship {
    private String studentId1;
    private String studentId2;
    private Date friendshipDate;
    
    // 用于连接查询的扩展属性
    private String friendName;
    private String friendDepartment;
    
    public Friendship() {
    }
    
    public Friendship(String studentId1, String studentId2, Date friendshipDate) {
        this.studentId1 = studentId1;
        this.studentId2 = studentId2;
        this.friendshipDate = friendshipDate;
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
    
    public Date getFriendshipDate() {
        return friendshipDate;
    }
    
    public void setFriendshipDate(Date friendshipDate) {
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
    
    @Override
    public String toString() {
        return "Friendship{" +
                "studentId1='" + studentId1 + '\'' +
                ", studentId2='" + studentId2 + '\'' +
                ", friendshipDate=" + friendshipDate +
                ", friendName='" + friendName + '\'' +
                ", friendDepartment='" + friendDepartment + '\'' +
                '}';
    }
} 