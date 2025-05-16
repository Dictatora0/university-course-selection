package dao;

import model.Message;
import util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 消息数据访问对象，处理与Message表相关的数据库操作
 */
public class MessageDAO {
    
    /**
     * 发送消息
     */
    public boolean sendMessage(Message message) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        boolean success = false;
        
        try {
            conn = DBConnection.getConnection();
            String sql = "INSERT INTO Message (from_student_id, to_student_id, content, send_time, is_read) " +
                    "VALUES (?, ?, ?, ?, ?)";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, message.getFromStudentId());
            pstmt.setString(2, message.getToStudentId());
            pstmt.setString(3, message.getContent());
            
            if (message.getSendTime() == null) {
                pstmt.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
            } else {
                pstmt.setTimestamp(4, new Timestamp(message.getSendTime().getTime()));
            }
            
            pstmt.setBoolean(5, message.isRead());
            
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
     * 查询两个学生之间的消息记录
     */
    public List<Message> findMessagesBetweenStudents(String studentId1, String studentId2) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        List<Message> messages = new ArrayList<>();
        
        try {
            conn = DBConnection.getConnection();
            String sql = "SELECT m.*, s1.name as from_name, s2.name as to_name FROM Message m " +
                    "JOIN Student s1 ON m.from_student_id = s1.student_id " +
                    "JOIN Student s2 ON m.to_student_id = s2.student_id " +
                    "WHERE (m.from_student_id = ? AND m.to_student_id = ?) " +
                    "OR (m.from_student_id = ? AND m.to_student_id = ?) " +
                    "ORDER BY m.send_time";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, studentId1);
            pstmt.setString(2, studentId2);
            pstmt.setString(3, studentId2);
            pstmt.setString(4, studentId1);
            rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Message message = new Message();
                message.setMessageId(rs.getLong("message_id"));
                message.setFromStudentId(rs.getString("from_student_id"));
                message.setToStudentId(rs.getString("to_student_id"));
                message.setContent(rs.getString("content"));
                message.setSendTime(rs.getTimestamp("send_time"));
                message.setRead(rs.getBoolean("is_read"));
                message.setFromStudentName(rs.getString("from_name"));
                message.setToStudentName(rs.getString("to_name"));
                messages.add(message);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBConnection.close(conn, pstmt, rs);
        }
        
        return messages;
    }
    
    /**
     * 将消息标记为已读
     */
    public boolean markMessagesAsRead(String fromStudentId, String toStudentId) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        boolean success = false;
        
        try {
            conn = DBConnection.getConnection();
            String sql = "UPDATE Message SET is_read = TRUE " +
                    "WHERE from_student_id = ? AND to_student_id = ? AND is_read = FALSE";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, fromStudentId);
            pstmt.setString(2, toStudentId);
            
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
     * 获取未读消息数量
     */
    public Map<String, Integer> getUnreadMessageCounts(String studentId) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        Map<String, Integer> unreadCounts = new HashMap<>();
        
        try {
            conn = DBConnection.getConnection();
            String sql = "SELECT from_student_id, COUNT(*) as unread_count FROM Message " +
                    "WHERE to_student_id = ? AND is_read = FALSE " +
                    "GROUP BY from_student_id";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, studentId);
            rs = pstmt.executeQuery();
            
            while (rs.next()) {
                String fromStudentId = rs.getString("from_student_id");
                int unreadCount = rs.getInt("unread_count");
                unreadCounts.put(fromStudentId, unreadCount);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBConnection.close(conn, pstmt, rs);
        }
        
        return unreadCounts;
    }
    
    /**
     * 删除消息
     */
    public boolean deleteMessage(Long messageId) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        boolean success = false;
        
        try {
            conn = DBConnection.getConnection();
            String sql = "DELETE FROM Message WHERE message_id = ?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setLong(1, messageId);
            
            int rowsAffected = pstmt.executeUpdate();
            success = rowsAffected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBConnection.close(conn, pstmt, null);
        }
        
        return success;
    }
} 