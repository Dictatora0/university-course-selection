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
                System.out.println("[FriendshipDAO] 发送好友请求失败：已存在好友关系或请求 " + requesterId + " -> " + targetId);
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
            
            if (success) {
                System.out.println("[FriendshipDAO] 发送好友请求成功: " + requesterId + " -> " + targetId);
            } else {
                System.out.println("[FriendshipDAO] 发送好友请求失败: " + requesterId + " -> " + targetId);
            }
        } catch (SQLException e) {
            System.out.println("[FriendshipDAO] 发送好友请求异常: " + e.getMessage());
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
        String sql = "SELECT s.*, d.dept_name, d.dept_id FROM Student s " +
                     "JOIN Friendship f ON s.student_id = f.friend_id " +
                     "LEFT JOIN Department d ON s.dept_id = d.dept_id " +
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
                    friend.setDeptId(rs.getString("dept_id"));
                    friend.setDeptName(rs.getString("dept_name"));
                    friend.setBirthDate(rs.getDate("birth_date"));
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

    /**
     * 获取用户发送但未被接受的好友请求
     * @param studentId 学生ID
     * @return 未接受的好友请求列表
     */
    public List<Student> findPendingRequestsByStudentId(String studentId) {
        List<Student> pendingRequests = new ArrayList<>();
        String sql = "SELECT s.*, d.dept_name FROM Student s " +
                     "JOIN Friendship f ON s.student_id = f.friend_id " +
                     "LEFT JOIN Department d ON s.dept_id = d.dept_id " +
                     "WHERE f.student_id = ? AND f.status = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, studentId);
            pstmt.setString(2, FriendshipStatus.PENDING.name());

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Student friend = new Student();
                    friend.setStudentId(rs.getString("student_id"));
                    friend.setName(rs.getString("name"));
                    friend.setDeptId(rs.getString("dept_id"));
                    friend.setDeptName(rs.getString("dept_name"));
                    // 设置一个标志，表示这是一个等待中的好友请求
                    friend.setStatus("PENDING_REQUEST");
                    pendingRequests.add(friend);
                }
            }
            
            System.out.println("[FriendshipDAO] 获取待处理好友请求成功，数量: " + pendingRequests.size() + " for student: " + studentId);
        } catch (SQLException e) {
            System.out.println("[FriendshipDAO] 获取待处理好友请求异常: " + e.getMessage());
            e.printStackTrace();
        }
        return pendingRequests;
    }

    /**
     * 获取收到的好友请求（由其他用户发送给当前用户的请求）
     * @param studentId 当前用户ID
     * @return 收到的好友请求列表
     */
    public List<Student> findReceivedRequestsByStudentId(String studentId) {
        List<Student> receivedRequests = new ArrayList<>();
        String sql = "SELECT s.*, d.dept_name FROM Student s " +
                     "JOIN Friendship f ON s.student_id = f.student_id " +
                     "LEFT JOIN Department d ON s.dept_id = d.dept_id " +
                     "WHERE f.friend_id = ? AND f.status = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, studentId);
            pstmt.setString(2, FriendshipStatus.PENDING.name());

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Student friend = new Student();
                    friend.setStudentId(rs.getString("student_id"));
                    friend.setName(rs.getString("name"));
                    friend.setDeptId(rs.getString("dept_id"));
                    friend.setDeptName(rs.getString("dept_name"));
                    // 设置一个标志，表示这是一个收到的好友请求
                    friend.setStatus("PENDING_RECEIVED");
                    receivedRequests.add(friend);
                }
            }
            
            System.out.println("[FriendshipDAO] 获取收到的好友请求成功，数量: " + receivedRequests.size() + " for student: " + studentId);
        } catch (SQLException e) {
            System.out.println("[FriendshipDAO] 获取收到的好友请求异常: " + e.getMessage());
            e.printStackTrace();
        }
        return receivedRequests;
    }
    
    /**
     * 获取被拒绝的好友请求
     * @param studentId 学生ID
     * @return 被拒绝的好友请求列表
     */
    public List<Student> findRejectedRequestsByStudentId(String studentId) {
        List<Student> rejectedRequests = new ArrayList<>();
        String sql = "SELECT s.*, d.dept_name, f.confirm_time as reject_time FROM Student s " +
                     "JOIN Friendship f ON s.student_id = f.friend_id " +
                     "LEFT JOIN Department d ON s.dept_id = d.dept_id " +
                     "WHERE f.student_id = ? AND f.status = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, studentId);
            pstmt.setString(2, FriendshipStatus.REJECTED.name());

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Student friend = new Student();
                    friend.setStudentId(rs.getString("student_id"));
                    friend.setName(rs.getString("name"));
                    friend.setDeptId(rs.getString("dept_id"));
                    friend.setDeptName(rs.getString("dept_name"));
                    // 设置被拒绝时间
                    friend.setRejectTime(rs.getTimestamp("reject_time"));
                    // 设置一个标志，表示这是一个被拒绝的好友请求
                    friend.setStatus("REJECTED");
                    rejectedRequests.add(friend);
                }
            }
            
            System.out.println("[FriendshipDAO] 获取被拒绝的好友请求成功，数量: " + rejectedRequests.size() + " for student: " + studentId);
        } catch (SQLException e) {
            System.out.println("[FriendshipDAO] 获取被拒绝的好友请求异常: " + e.getMessage());
            e.printStackTrace();
        }
        return rejectedRequests;
    }
    
    /**
     * 好友推荐 - 基于共同好友
     * @param studentId 学生ID
     * @param limit 推荐数量限制
     * @return 推荐好友列表
     */
    public List<Student> recommendByMutualFriends(String studentId, int limit) {
        List<Student> recommendations = new ArrayList<>();
        String sql = "SELECT s.*, d.dept_name, COUNT(f2.friend_id) as mutual_count FROM Student s " +
                     "JOIN Friendship f1 ON s.student_id = f1.friend_id " +
                     "JOIN Friendship f2 ON f1.student_id = f2.student_id " +
                     "LEFT JOIN Department d ON s.dept_id = d.dept_id " +
                     "WHERE f2.friend_id != s.student_id " +
                     "AND f2.friend_id = ? " +
                     "AND f1.status = 'ACCEPTED' AND f2.status = 'ACCEPTED' " +
                     "AND s.student_id NOT IN ( " +
                     "  SELECT friend_id FROM Friendship WHERE student_id = ? AND status IN ('ACCEPTED', 'PENDING') " +
                     "  UNION " +
                     "  SELECT student_id FROM Friendship WHERE friend_id = ? AND status IN ('ACCEPTED', 'PENDING') " +
                     ") " +
                     "AND s.student_id != ? " +
                     "GROUP BY s.student_id " +
                     "ORDER BY mutual_count DESC " +
                     "LIMIT ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, studentId);
            pstmt.setString(2, studentId);
            pstmt.setString(3, studentId);
            pstmt.setString(4, studentId);
            pstmt.setInt(5, limit);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Student friend = new Student();
                    friend.setStudentId(rs.getString("student_id"));
                    friend.setName(rs.getString("name"));
                    friend.setDeptId(rs.getString("dept_id"));
                    friend.setDeptName(rs.getString("dept_name"));
                    int mutualCount = rs.getInt("mutual_count");
                    // 设置推荐理由
                    friend.setRecommendReason("你们有" + mutualCount + "个共同好友");
                    friend.setStatus("RECOMMENDED_MUTUAL");
                    recommendations.add(friend);
                }
            }
            
            System.out.println("[FriendshipDAO] 基于共同好友推荐成功，数量: " + recommendations.size() + " for student: " + studentId);
        } catch (SQLException e) {
            System.out.println("[FriendshipDAO] 基于共同好友推荐异常: " + e.getMessage());
            e.printStackTrace();
        }
        return recommendations;
    }
    
    /**
     * 好友推荐 - 基于相同课程
     * @param studentId 学生ID
     * @param limit 推荐数量限制
     * @return 推荐好友列表
     */
    public List<Student> recommendBySameCourses(String studentId, int limit) {
        List<Student> recommendations = new ArrayList<>();
        String sql = "SELECT s.*, d.dept_name, COUNT(e1.course_id) as course_count, " +
                     "GROUP_CONCAT(c.course_name SEPARATOR ', ') as common_courses FROM Student s " +
                     "JOIN Enrollment e1 ON s.student_id = e1.student_id " +
                     "JOIN Enrollment e2 ON e1.course_id = e2.course_id " +
                     "JOIN Course c ON e1.course_id = c.course_id " +
                     "LEFT JOIN Department d ON s.dept_id = d.dept_id " +
                     "WHERE e2.student_id = ? " +
                     "AND s.student_id NOT IN ( " +
                     "  SELECT friend_id FROM Friendship WHERE student_id = ? AND status IN ('ACCEPTED', 'PENDING') " +
                     "  UNION " +
                     "  SELECT student_id FROM Friendship WHERE friend_id = ? AND status IN ('ACCEPTED', 'PENDING') " +
                     ") " +
                     "AND s.student_id != ? " +
                     "GROUP BY s.student_id " +
                     "ORDER BY course_count DESC " +
                     "LIMIT ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, studentId);
            pstmt.setString(2, studentId);
            pstmt.setString(3, studentId);
            pstmt.setString(4, studentId);
            pstmt.setInt(5, limit);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Student friend = new Student();
                    friend.setStudentId(rs.getString("student_id"));
                    friend.setName(rs.getString("name"));
                    friend.setDeptId(rs.getString("dept_id"));
                    friend.setDeptName(rs.getString("dept_name"));
                    int courseCount = rs.getInt("course_count");
                    String commonCourses = rs.getString("common_courses");
                    // 设置推荐理由
                    friend.setRecommendReason("你们选了" + courseCount + "门相同的课程: " + 
                                             (commonCourses.length() > 50 ? commonCourses.substring(0, 47) + "..." : commonCourses));
                    friend.setStatus("RECOMMENDED_COURSES");
                    recommendations.add(friend);
                }
            }
            
            System.out.println("[FriendshipDAO] 基于相同课程推荐成功，数量: " + recommendations.size() + " for student: " + studentId);
        } catch (SQLException e) {
            System.out.println("[FriendshipDAO] 基于相同课程推荐异常: " + e.getMessage());
            e.printStackTrace();
        }
        return recommendations;
    }
    
    /**
     * 好友推荐 - 基于最近活跃
     * @param studentId 学生ID
     * @param limit 推荐数量限制
     * @return 推荐好友列表
     */
    public List<Student> recommendByRecentActivity(String studentId, int limit) {
        List<Student> recommendations = new ArrayList<>();
        String sql = "SELECT s.*, d.dept_name, MAX(l.login_time) as last_login FROM Student s " +
                     "JOIN LoginLog l ON s.student_id = l.student_id " +
                     "LEFT JOIN Department d ON s.dept_id = d.dept_id " +
                     "WHERE s.student_id NOT IN ( " +
                     "  SELECT friend_id FROM Friendship WHERE student_id = ? AND status IN ('ACCEPTED', 'PENDING') " +
                     "  UNION " +
                     "  SELECT student_id FROM Friendship WHERE friend_id = ? AND status IN ('ACCEPTED', 'PENDING') " +
                     ") " +
                     "AND s.student_id != ? " +
                     "GROUP BY s.student_id " +
                     "ORDER BY last_login DESC " +
                     "LIMIT ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, studentId);
            pstmt.setString(2, studentId);
            pstmt.setString(3, studentId);
            pstmt.setInt(4, limit);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Student friend = new Student();
                    friend.setStudentId(rs.getString("student_id"));
                    friend.setName(rs.getString("name"));
                    friend.setDeptId(rs.getString("dept_id"));
                    friend.setDeptName(rs.getString("dept_name"));
                    Timestamp lastLogin = rs.getTimestamp("last_login");
                    // 设置推荐理由
                    friend.setRecommendReason("该用户最近活跃于 " + 
                                             (lastLogin != null ? new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(lastLogin) : "未知时间"));
                    friend.setStatus("RECOMMENDED_ACTIVE");
                    recommendations.add(friend);
                }
            }
            
            System.out.println("[FriendshipDAO] 基于最近活跃推荐成功，数量: " + recommendations.size() + " for student: " + studentId);
        } catch (SQLException e) {
            System.out.println("[FriendshipDAO] 基于最近活跃推荐异常: " + e.getMessage());
            e.printStackTrace();
        }
        return recommendations;
    }
    
    /**
     * 获取综合好友推荐
     * @param studentId 学生ID
     * @return 推荐好友列表
     */
    public List<Student> getAllRecommendations(String studentId) {
        List<Student> recommendations = new ArrayList<>();
        
        // 最多推荐5个共同好友
        List<Student> mutualFriends = recommendByMutualFriends(studentId, 5);
        recommendations.addAll(mutualFriends);
        
        // 最多推荐5个相同课程的同学
        List<Student> sameCourseStudents = recommendBySameCourses(studentId, 5);
        recommendations.addAll(sameCourseStudents);
        
        // 最多推荐5个最近活跃的学生
        List<Student> recentActiveStudents = recommendByRecentActivity(studentId, 5);
        recommendations.addAll(recentActiveStudents);
        
        System.out.println("[FriendshipDAO] 综合推荐成功，总数量: " + recommendations.size() + " for student: " + studentId);
        return recommendations;
    }

    /**
     * 判断两个学生之间是否存在待处理的好友请求
     * @param senderId 发送请求的学生ID
     * @param receiverId 接收请求的学生ID
     * @return 如果存在待处理请求则返回true，否则返回false
     */
    public boolean isPendingRequest(String senderId, String receiverId) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        boolean isPending = false;
        
        try {
            conn = DBConnection.getConnection();
            String sql = "SELECT * FROM Friendship WHERE student_id = ? AND friend_id = ? AND status = ?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, senderId);
            pstmt.setString(2, receiverId);
            pstmt.setString(3, FriendshipStatus.PENDING.name());
            rs = pstmt.executeQuery();
            
            isPending = rs.next(); // 如果有记录则表示存在待处理请求
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBConnection.close(conn, pstmt, rs);
        }
        
        return isPending;
    }
} 