package src.model;

import java.util.Date;

public class Friendship {
    private String studentId1;
    private String studentId2;
    private Date friendshipDate;
    
    public Friendship() {
    }
    
    public Friendship(String studentId1, String studentId2) {
        // 确保studentId1 < studentId2，保持一致性
        if (studentId1.compareTo(studentId2) < 0) {
            this.studentId1 = studentId1;
            this.studentId2 = studentId2;
        } else {
            this.studentId1 = studentId2;
            this.studentId2 = studentId1;
        }
        this.friendshipDate = new Date();
    }
    
    // Getters and Setters
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
    
    @Override
    public String toString() {
        return "Friendship{" +
                "studentId1='" + studentId1 + '\'' +
                ", studentId2='" + studentId2 + '\'' +
                ", friendshipDate=" + friendshipDate +
                '}';
    }
} 