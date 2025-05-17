package dao;

import model.Friendship;
import model.Friendship.FriendshipStatus;
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
     * 发送好友请求
     * @param requesterId 请求者ID
     * @param targetId 目标ID
     * @return 成功返回true，失败返回false
     */
    public boolean sendFriendRequest(String requesterId, String targetId) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        boolean success = false;
        
        try {
            conn = DBConnection.getConnection();
            // 检查是否已存在请求
            if (checkFriendshipExists(conn, requesterId, targetId)) {
                return false;
            }
            
            String sql = "INSERT INTO Friendship (student_id, friend_id, status, request_time) VALUES (?, ?, ?, ?)";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, requesterId);
            pstmt.setString(2, targetId);
            pstmt.setString(3, FriendshipStatus.PENDING.name());
            pstmt.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
            
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
     * 接受好友请求
     * @param targetId 目标ID(被请求者)
     * @param requesterId 请求者ID
     * @return 成功返回true，失败返回false
     */
    public boolean acceptFriendRequest(String targetId, String requesterId) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        boolean success = false;
        
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false); // 开启事务
            
            // 更新请求状态为已接受
            String updateSql = "UPDATE Friendship SET status = ?, confirm_time = ? WHERE student_id = ? AND friend_id = ? AND status = ?";
            pstmt = conn.prepareStatement(updateSql);
            pstmt.setString(1, FriendshipStatus.ACCEPTED.name());
            pstmt.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
            pstmt.setString(3, requesterId);
            pstmt.setString(4, targetId);
            pstmt.setString(5, FriendshipStatus.PENDING.name());
            
            int rowsAffected = pstmt.executeUpdate();
            
            if (rowsAffected > 0) {
                // 创建反向的好友关系
                String insertSql = "INSERT INTO Friendship (student_id, friend_id, status, request_time, confirm_time) VALUES (?, ?, ?, ?, ?)";
                pstmt = conn.prepareStatement(insertSql);
                pstmt.setString(1, targetId);
                pstmt.setString(2, requesterId);
                pstmt.setString(3, FriendshipStatus.ACCEPTED.name());
                pstmt.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
                pstmt.setTimestamp(5, new Timestamp(System.currentTimeMillis()));
                
                rowsAffected = pstmt.executeUpdate();
                if (rowsAffected > 0) {
                    conn.commit();
                    success = true;
                } else {
                    conn.rollback();
                }
            } else {
                conn.rollback();
            }
        } catch (SQLException e) {
            try {
                if (conn != null) {
                    conn.rollback();
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
        } finally {
            try {
                if (conn != null) {
                    conn.setAutoCommit(true);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            DBConnection.close(conn, pstmt, null);
        }
        
        return success;
    }
    
    /**
     * 拒绝好友请求
     * @param targetId 目标ID(被请求者)
     * @param requesterId 请求者ID
     * @return 成功返回true，失败返回false
     */
    public boolean rejectFriendRequest(String targetId, String requesterId) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        boolean success = false;
        
        try {
            conn = DBConnection.getConnection();
            String sql = "UPDATE Friendship SET status = ?, confirm_time = ? WHERE student_id = ? AND friend_id = ? AND status = ?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, FriendshipStatus.REJECTED.name());
            pstmt.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
            pstmt.setString(3, requesterId);
            pstmt.setString(4, targetId);
            pstmt.setString(5, FriendshipStatus.PENDING.name());
            
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
     * 获取好友请求列表
     * @param studentId 学生ID
     * @return 好友请求列表
     */
    public List<Friendship> getFriendRequests(String studentId) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        List<Friendship> requests = new ArrayList<>();
        
        try {
            conn = DBConnection.getConnection();
            String sql = "SELECT f.*, s.name FROM Friendship f " +
                    "JOIN Student s ON f.student_id = s.student_id " +
                    "WHERE f.friend_id = ? AND f.status = ?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, studentId);
            pstmt.setString(2, FriendshipStatus.PENDING.name());
            rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Friendship request = new Friendship();
                request.setStudentId1(rs.getString("student_id"));
                request.setStudentId2(rs.getString("friend_id"));
                request.setStatus(FriendshipStatus.valueOf(rs.getString("status")));
                request.setRequestTime(rs.getTimestamp("request_time"));
                request.setConfirmTime(rs.getTimestamp("confirm_time"));
                request.setFriendName(rs.getString("name"));
                requests.add(request);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBConnection.close(conn, pstmt, rs);
        }
        
        return requests;
    }
    
    /**
     * 检查好友关系是否已存在
     */
    private boolean checkFriendshipExists(Connection conn, String studentId1, String studentId2) throws SQLException {
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        boolean exists = false;
        
        try {
            String sql = "SELECT 1 FROM Friendship WHERE student_id = ? AND friend_id = ?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, studentId1);
            pstmt.setString(2, studentId2);
            rs = pstmt.executeQuery();
            
            exists = rs.next();
        } finally {
            if (rs != null) {
                rs.close();
            }
            if (pstmt != null) {
                pstmt.close();
            }
        }
        
        return exists;
    }
    
    /**
     * 添加好友关系（保留原方法，仅用于兼容旧代码）
     */
    public boolean add(String studentId1, String studentId2) {
        return sendFriendRequest(studentId1, studentId2);
    }
    
    /**
     * 删除好友关系
     */
    public boolean delete(String studentId1, String studentId2) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        boolean success = false;
        
        try {
            conn = DBConnection.getConnection();
            String sql = "DELETE FROM Friendship WHERE (student_id = ? AND friend_id = ?) OR (student_id = ? AND friend_id = ?)";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, studentId1);
            pstmt.setString(2, studentId2);
            pstmt.setString(3, studentId2);
            pstmt.setString(4, studentId1);
            
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
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        boolean isFriend = false;
        
        try {
            conn = DBConnection.getConnection();
            String sql = "SELECT 1 FROM Friendship " +
                    "WHERE student_id = ? AND friend_id = ? AND status = ? " +
                    "AND EXISTS (SELECT 1 FROM Friendship WHERE student_id = ? AND friend_id = ? AND status = ?)";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, studentId1);
            pstmt.setString(2, studentId2);
            pstmt.setString(3, FriendshipStatus.ACCEPTED.name());
            pstmt.setString(4, studentId2);
            pstmt.setString(5, studentId1);
            pstmt.setString(6, FriendshipStatus.ACCEPTED.name());
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
     * 搜索学生（通过学号）
     */
    public List<Student> searchStudents(String keyword) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        List<Student> students = new ArrayList<>();
        
        try {
            conn = DBConnection.getConnection();
            String sql = "SELECT * FROM Student WHERE student_id LIKE ? OR name LIKE ? LIMIT 20";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, "%" + keyword + "%");
            pstmt.setString(2, "%" + keyword + "%");
            rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Student student = new Student();
                student.setStudentId(rs.getString("student_id"));
                student.setName(rs.getString("name"));
                student.setCreatedAt(rs.getTimestamp("created_at"));
                // 不返回敏感信息如密码
                students.add(student);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBConnection.close(conn, pstmt, rs);
        }
        
        return students;
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
                    "JOIN Student s ON f.friend_id = s.student_id " +
                    "LEFT JOIN Course c ON c.course_id IN (SELECT course_id FROM Enrollment WHERE student_id = s.student_id) " +
                    "LEFT JOIN Department d ON c.dept_id = d.dept_id " +
                    "WHERE f.student_id = ? AND f.status = ? " +
                    "GROUP BY f.student_id, f.friend_id, f.created_at, s.name, d.dept_name";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, studentId);
            pstmt.setString(2, FriendshipStatus.ACCEPTED.name());
            rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Friendship friendship = new Friendship();
                friendship.setStudentId1(rs.getString("student_id"));
                friendship.setStudentId2(rs.getString("friend_id"));
                friendship.setFriendshipDate(rs.getTimestamp("created_at"));
                friendship.setStatus(FriendshipStatus.valueOf(rs.getString("status")));
                friendship.setFriendName(rs.getString("friend_name"));
                friendship.setFriendDepartment(rs.getString("dept_name"));
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
        String sql = "SELECT s.* FROM Student s JOIN Friendship f " +
                     "ON s.student_id = f.friend_id " +
                     "WHERE f.student_id = ? AND f.status = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, studentId);
            pstmt.setString(2, FriendshipStatus.ACCEPTED.name());

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Student friend = new Student();
                    friend.setStudentId(rs.getString("student_id"));
                    friend.setName(rs.getString("name"));
                    friend.setBirthDate(rs.getDate("birth_date"));
                    friend.setIdCard(rs.getString("id_card"));
                    friend.setAddress(rs.getString("address"));
                    friend.setCreatedAt(rs.getTimestamp("created_at"));
                    friend.setBalance(rs.getDouble("balance"));
                    friends.add(friend);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return friends;
    }

    /**
     * 获取好友数量
     */
    public int getFriendCount(String studentId) {
        String sql = "SELECT COUNT(*) FROM Friendship WHERE student_id = ? AND status = ?";
        int count = 0;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, studentId);
            pstmt.setString(2, FriendshipStatus.ACCEPTED.name());
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
                    "WHERE s.student_id != ? AND NOT EXISTS " +
                    "(SELECT 1 FROM Friendship WHERE student_id = ? AND friend_id = s.student_id AND status = ?) " +
                    "GROUP BY s.student_id " +
                    "ORDER BY common_courses DESC " +
                    "LIMIT ?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, studentId);
            pstmt.setString(2, studentId);
            pstmt.setString(3, studentId);
            pstmt.setString(4, FriendshipStatus.ACCEPTED.name());
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