package dao;

import model.Friendship;
import model.Student;
import util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 好友关系数据访问对象，处理与Friendship表相关的数据库操作
 */
public class FriendshipDAO {
    
    /**
     * 添加好友关系
     */
    public boolean add(String studentId1, String studentId2) {
        // 确保studentId1 < studentId2，保持一致性
        if (studentId1.compareTo(studentId2) > 0) {
            String temp = studentId1;
            studentId1 = studentId2;
            studentId2 = temp;
        }
        
        Connection conn = null;
        PreparedStatement pstmt = null;
        boolean success = false;
        
        try {
            conn = DBConnection.getConnection();
            String sql = "INSERT INTO Friendship (student_id1, student_id2) VALUES (?, ?)";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, studentId1);
            pstmt.setString(2, studentId2);
            
            int rowsAffected = pstmt.executeUpdate();
            success = rowsAffected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBConnection.close(conn, pstmt, null);
        }
        
        return success;
    }
    
    /**
     * 删除好友关系
     */
    public boolean delete(String studentId1, String studentId2) {
        // 确保studentId1 < studentId2，保持一致性
        if (studentId1.compareTo(studentId2) > 0) {
            String temp = studentId1;
            studentId1 = studentId2;
            studentId2 = temp;
        }
        
        Connection conn = null;
        PreparedStatement pstmt = null;
        boolean success = false;
        
        try {
            conn = DBConnection.getConnection();
            String sql = "DELETE FROM Friendship WHERE student_id1 = ? AND student_id2 = ?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, studentId1);
            pstmt.setString(2, studentId2);
            
            int rowsAffected = pstmt.executeUpdate();
            success = rowsAffected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBConnection.close(conn, pstmt, null);
        }
        
        return success;
    }
    
    /**
     * 检查是否是好友关系
     */
    public boolean isFriend(String studentId1, String studentId2) {
        // 确保studentId1 < studentId2，保持一致性
        if (studentId1.compareTo(studentId2) > 0) {
            String temp = studentId1;
            studentId1 = studentId2;
            studentId2 = temp;
        }
        
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        boolean isFriend = false;
        
        try {
            conn = DBConnection.getConnection();
            String sql = "SELECT 1 FROM Friendship WHERE student_id1 = ? AND student_id2 = ?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, studentId1);
            pstmt.setString(2, studentId2);
            rs = pstmt.executeQuery();
            
            isFriend = rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBConnection.close(conn, pstmt, rs);
        }
        
        return isFriend;
    }
    
    /**
     * 获取学生的所有好友
     */
    public List<Friendship> findByStudentId(String studentId) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        List<Friendship> friendships = new ArrayList<>();
        
        try {
            conn = DBConnection.getConnection();
            String sql = "SELECT f.*, s.name as friend_name, d.dept_name FROM Friendship f " +
                    "JOIN Student s ON (f.student_id1 = ? AND f.student_id2 = s.student_id) " +
                    "OR (f.student_id2 = ? AND f.student_id1 = s.student_id) " +
                    "LEFT JOIN Course c ON c.course_id IN (SELECT course_id FROM Enrollment WHERE student_id = s.student_id) " +
                    "LEFT JOIN Department d ON c.dept_id = d.dept_id " +
                    "GROUP BY f.student_id1, f.student_id2, f.friendship_date, s.name, d.dept_name";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, studentId);
            pstmt.setString(2, studentId);
            rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Friendship friendship = new Friendship();
                friendship.setStudentId1(rs.getString("student_id1"));
                friendship.setStudentId2(rs.getString("student_id2"));
                friendship.setFriendshipDate(rs.getTimestamp("friendship_date"));
                
                // 设置好友ID和姓名
                if (studentId.equals(rs.getString("student_id1"))) {
                    friendship.setFriendName(rs.getString("friend_name"));
                    friendship.setFriendDepartment(rs.getString("dept_name"));
                } else {
                    friendship.setFriendName(rs.getString("friend_name"));
                    friendship.setFriendDepartment(rs.getString("dept_name"));
                }
                
                friendships.add(friendship);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBConnection.close(conn, pstmt, rs);
        }
        
        return friendships;
    }
    
    /**
     * 获取学生的所有好友的详细信息 (Student对象列表)
     */
    public List<Student> getFriendsWithDetails(String studentId) {
        List<Student> friends = new ArrayList<>();
        String sql = "SELECT s.* FROM Student s JOIN Friendship f ON " +
                     "(s.student_id = f.student_id2 AND f.student_id1 = ?) OR " +
                     "(s.student_id = f.student_id1 AND f.student_id2 = ?) " +
                     "WHERE s.student_id != ?"; // 确保不把自己列为好友（尽管好友关系表设计上应该避免这种情况）

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, studentId);
            pstmt.setString(2, studentId);
            pstmt.setString(3, studentId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Student friend = new Student();
                    friend.setStudentId(rs.getString("student_id"));
                    friend.setName(rs.getString("name"));
                    // friend.setPassword(rs.getString("password")); // 通常不返回密码
                    friend.setBirthDate(rs.getDate("birth_date"));
                    friend.setIdCard(rs.getString("id_card"));
                    friend.setAddress(rs.getString("address"));
                    friend.setCreatedAt(rs.getTimestamp("created_at"));
                    friend.setBalance(rs.getDouble("balance")); // 假设Student有setBalance
                    friends.add(friend);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace(); // 更健壮的错误处理
        }
        return friends;
    }

    /**
     * 获取好友数量
     */
    public int getFriendCount(String studentId) {
        String sql = "SELECT COUNT(*) FROM Friendship WHERE student_id1 = ? OR student_id2 = ?";
        int count = 0;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, studentId);
            pstmt.setString(2, studentId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    count = rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return count;
    }
    
    /**
     * 推荐好友（选择与学生选择相同课程的其他学生）
     */
    public List<Student> recommendFriends(String studentId, int limit) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        List<Student> recommendations = new ArrayList<>();
        
        try {
            conn = DBConnection.getConnection();
            String sql = "SELECT s.*, COUNT(e1.course_id) as common_courses FROM Student s " +
                    "JOIN Enrollment e1 ON s.student_id = e1.student_id " +
                    "JOIN Enrollment e2 ON e1.course_id = e2.course_id AND e2.student_id = ? " +
                    "WHERE s.student_id != ? AND s.student_id NOT IN " +
                    "(SELECT student_id1 FROM Friendship WHERE student_id2 = ? " +
                    "UNION SELECT student_id2 FROM Friendship WHERE student_id1 = ?) " +
                    "GROUP BY s.student_id " +
                    "ORDER BY common_courses DESC " +
                    "LIMIT ?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, studentId);
            pstmt.setString(2, studentId);
            pstmt.setString(3, studentId);
            pstmt.setString(4, studentId);
            pstmt.setInt(5, limit);
            rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Student student = new Student();
                student.setStudentId(rs.getString("student_id"));
                student.setName(rs.getString("name"));
                student.setBirthDate(rs.getDate("birth_date"));
                student.setIdCard(rs.getString("id_card"));
                student.setAddress(rs.getString("address"));
                student.setCreatedAt(rs.getTimestamp("created_at"));
                recommendations.add(student);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBConnection.close(conn, pstmt, rs);
        }
        
        return recommendations;
    }
} 