package dao;

import model.Message;
import model.Student;
import model.Contact;
import util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 消息数据访问对象，处理与Message表相关的数据库操作
 */
public class MessageDAO {
    
    /**
     * 添加消息 (原 sendMessage)
     */
    public boolean add(Message message) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        boolean success = false;
        
        try {
            conn = DBConnection.getConnection();
            String sql = "INSERT INTO Message (from_student_id, to_student_id, content, send_time, is_read) " +
                    "VALUES (?, ?, ?, ?, ?)";
            pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
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
            if (rowsAffected > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        message.setMessageId(generatedKeys.getLong(1));
                    }
                }
                success = true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBConnection.close(conn, pstmt, null);
        }
        return success;
    }

    /**
     * 通过ID查找消息
     */
    public Message findById(Long messageId) {
        String sql = "SELECT m.*, s1.name as from_name, s2.name as to_name FROM Message m " +
                     "LEFT JOIN Student s1 ON m.from_student_id = s1.student_id " +
                     "LEFT JOIN Student s2 ON m.to_student_id = s2.student_id " +
                     "WHERE m.message_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, messageId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToMessage(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    /**
     * 查询两个学生之间的消息记录 (原 findMessagesBetweenStudents)
     */
    public List<Message> findConversation(String studentId1, String studentId2) {
        List<Message> messages = new ArrayList<>();
        String sql = "SELECT m.*, s1.name as from_name, s2.name as to_name FROM Message m " +
                    "JOIN Student s1 ON m.from_student_id = s1.student_id " +
                    "JOIN Student s2 ON m.to_student_id = s2.student_id " +
                    "WHERE (m.from_student_id = ? AND m.to_student_id = ?) " +
                    "OR (m.from_student_id = ? AND m.to_student_id = ?) " +
                    "ORDER BY m.send_time ASC"; // ASC for chronological order
        try (Connection conn = DBConnection.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, studentId1);
            pstmt.setString(2, studentId2);
            pstmt.setString(3, studentId2);
            pstmt.setString(4, studentId1);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    messages.add(mapRowToMessage(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return messages;
    }

    /**
     * 将指定发送者给指定接收者的所有未读消息标记为已读 (原 markMessagesAsRead)
     * @param fromStudentId 发送者ID
     * @param toStudentId   接收者ID (即当前用户)
     */
    public boolean markAllAsRead(String fromStudentId, String toStudentId) {
        String sql = "UPDATE Message SET is_read = TRUE " +
                     "WHERE from_student_id = ? AND to_student_id = ? AND is_read = FALSE";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, fromStudentId);
            pstmt.setString(2, toStudentId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 将单条消息标记为已读，并校验接收者
     * @param messageId 消息ID
     * @param receiverId 接收者ID (当前用户)
     */
    public boolean markAsRead(Long messageId, String receiverId) {
        String sql = "UPDATE Message SET is_read = TRUE WHERE message_id = ? AND to_student_id = ? AND is_read = FALSE";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, messageId);
            pstmt.setString(2, receiverId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 获取某学生的所有未读消息
     */
    public List<Message> findUnreadMessages(String studentId) {
        List<Message> messages = new ArrayList<>();
        // 获取发送给studentId的未读消息，并带上发送者姓名
        String sql = "SELECT m.*, s.name as from_name FROM Message m " +
                     "JOIN Student s ON m.from_student_id = s.student_id " +
                     "WHERE m.to_student_id = ? AND m.is_read = FALSE " +
                     "ORDER BY m.send_time DESC"; // 通常最新的未读消息在前面
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, studentId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Message message = mapRowToMessage(rs); // mapRowToMessage will handle from_name
                    // to_name is not directly available in this query context for the receiver, 
                    // but the servlet already knows the current user (receiver).
                    messages.add(message);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return messages;
    }

    /**
     * 获取某学生的总未读消息数量
     */
    public int getUnreadMessageCount(String studentId) {
        String sql = "SELECT COUNT(*) FROM Message WHERE to_student_id = ? AND is_read = FALSE";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, studentId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
    
    /**
     * 获取与某学生有过消息往来的最近联系人列表
     * 包括最后一条消息内容和未读消息数
     */
    public List<Contact> getRecentContacts(String studentId) {
        List<Contact> contacts = new ArrayList<>();
        
        // 这个SQL查询较复杂，主要目标是：
        // 1. 找出当前用户最近有消息往来的联系人
        // 2. 获取每个联系人的最后一条消息
        // 3. 获取来自每个联系人的未读消息数量
        String sql = 
            "WITH recent_contacts AS (" +
            "  SELECT " +
            "    CASE " +
            "      WHEN m.from_student_id = ? THEN m.to_student_id " +
            "      ELSE m.from_student_id " +
            "    END AS contact_id, " +
            "    MAX(m.send_time) AS last_message_time " +
            "  FROM Message m " +
            "  WHERE m.from_student_id = ? OR m.to_student_id = ? " +
            "  GROUP BY contact_id " +
            "  ORDER BY last_message_time DESC" +
            ") " +
            "SELECT " +
            "  rc.contact_id, " +
            "  s.name AS contact_name, " +
            "  s.dept_name, " +
            "  lm.content AS last_message, " +
            "  lm.send_time AS last_message_time, " +
            "  (SELECT COUNT(*) FROM Message " +
            "   WHERE from_student_id = rc.contact_id AND to_student_id = ? AND is_read = FALSE) AS unread_count " +
            "FROM recent_contacts rc " +
            "JOIN Student s ON rc.contact_id = s.student_id " +
            "JOIN Message lm ON ((lm.from_student_id = rc.contact_id AND lm.to_student_id = ?) " +
            "                 OR (lm.from_student_id = ? AND lm.to_student_id = rc.contact_id)) " +
            "                AND lm.send_time = rc.last_message_time";
            
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, studentId);
            pstmt.setString(2, studentId);
            pstmt.setString(3, studentId);
            pstmt.setString(4, studentId);
            pstmt.setString(5, studentId);
            pstmt.setString(6, studentId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Contact contact = new Contact();
                    contact.setStudentId(rs.getString("contact_id"));
                    contact.setName(rs.getString("contact_name"));
                    contact.setDeptName(rs.getString("dept_name"));
                    contact.setLastMessage(rs.getString("last_message"));
                    contact.setLastMessageTime(rs.getTimestamp("last_message_time"));
                    contact.setUnreadCount(rs.getInt("unread_count"));
                    contacts.add(contact);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return contacts;
    }
    
    /**
     * 删除消息 (原 deleteMessage)
     * 注意：此方法不检查用户权限，权限检查应在Servlet层完成
     */
    public boolean delete(Long messageId) {
        String sql = "DELETE FROM Message WHERE message_id = ?";
        try (Connection conn = DBConnection.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, messageId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // Helper method to map ResultSet row to Message object
    private Message mapRowToMessage(ResultSet rs) throws SQLException {
        Message message = new Message();
        message.setMessageId(rs.getLong("message_id"));
        message.setFromStudentId(rs.getString("from_student_id"));
        message.setToStudentId(rs.getString("to_student_id"));
        message.setContent(rs.getString("content"));
        message.setSendTime(rs.getTimestamp("send_time"));
        message.setRead(rs.getBoolean("is_read"));

        // Attempt to get from_name and to_name if columns exist in ResultSet
        if (hasColumn(rs, "from_name")) {
            message.setFromStudentName(rs.getString("from_name"));
        }
        if (hasColumn(rs, "to_name")) {
            message.setToStudentName(rs.getString("to_name"));
        }
        return message;
    }

    // Helper to check if a column exists in ResultSet
    private boolean hasColumn(ResultSet rs, String columnName) throws SQLException {
        ResultSetMetaData rsmd = rs.getMetaData();
        int columns = rsmd.getColumnCount();
        for (int x = 1; x <= columns; x++) {
            if (columnName.equals(rsmd.getColumnName(x))) {
                return true;
            }
        }
        return false;
    }
} 