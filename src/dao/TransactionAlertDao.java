package dao;

import model.TransactionAlert;
import util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 交易风控警报数据访问对象
 */
public class TransactionAlertDao {
    private static final Logger LOGGER = Logger.getLogger(TransactionAlertDao.class.getName());
    
    /**
     * 创建新警报
     * @param alert 警报对象
     * @return 新警报ID，失败返回-1
     */
    public int createAlert(TransactionAlert alert) {
        String sql = "INSERT INTO TransactionAlert (student_id, alert_time, alert_type, description, resolved) " +
                     "VALUES (?, NOW(), ?, ?, 0)";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, alert.getStudentId());
            stmt.setString(2, alert.getAlertType());
            stmt.setString(3, alert.getDescription());
            
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "创建风控警报失败", e);
        }
        
        return -1;
    }
    
    /**
     * 解决警报
     * @param alertId 警报ID
     * @param resolvedBy 处理人ID
     * @return 是否成功
     */
    public boolean resolveAlert(int alertId, String resolvedBy) {
        String sql = "UPDATE TransactionAlert SET resolved = 1, resolved_by = ?, resolved_at = NOW() " +
                     "WHERE alert_id = ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, resolvedBy);
            stmt.setInt(2, alertId);
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "解决风控警报失败", e);
            return false;
        }
    }
    
    /**
     * 获取未解决的警报
     * @return 未解决警报列表
     */
    public List<TransactionAlert> getUnresolvedAlerts() {
        return getAlertsByStatus(false);
    }
    
    /**
     * 获取已解决的警报
     * @return 已解决警报列表
     */
    public List<TransactionAlert> getResolvedAlerts() {
        return getAlertsByStatus(true);
    }
    
    /**
     * 获取学生的所有警报
     * @param studentId 学生ID
     * @return 警报列表
     */
    public List<TransactionAlert> getAlertsByStudentId(String studentId) {
        List<TransactionAlert> alerts = new ArrayList<>();
        
        String sql = "SELECT a.*, s.name as student_name, " +
                     "CASE WHEN a.resolved_by IS NOT NULL THEN adm.name ELSE NULL END as resolver_name " +
                     "FROM TransactionAlert a " +
                     "JOIN Student s ON a.student_id = s.student_id " +
                     "LEFT JOIN Administrator adm ON a.resolved_by = adm.admin_id " +
                     "WHERE a.student_id = ? " +
                     "ORDER BY a.alert_time DESC";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, studentId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    alerts.add(extractAlertFromResultSet(rs));
                }
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "获取学生风控警报失败", e);
        }
        
        return alerts;
    }
    
    /**
     * 获取指定时间段内的警报
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 警报列表
     */
    public List<TransactionAlert> getAlertsByDateRange(Date startDate, Date endDate) {
        List<TransactionAlert> alerts = new ArrayList<>();
        
        String sql = "SELECT a.*, s.name as student_name, " +
                     "CASE WHEN a.resolved_by IS NOT NULL THEN adm.name ELSE NULL END as resolver_name " +
                     "FROM TransactionAlert a " +
                     "JOIN Student s ON a.student_id = s.student_id " +
                     "LEFT JOIN Administrator adm ON a.resolved_by = adm.admin_id " +
                     "WHERE a.alert_time BETWEEN ? AND ? " +
                     "ORDER BY a.alert_time DESC";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setTimestamp(1, new Timestamp(startDate.getTime()));
            stmt.setTimestamp(2, new Timestamp(endDate.getTime()));
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    alerts.add(extractAlertFromResultSet(rs));
                }
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "获取指定时间段内的风控警报失败", e);
        }
        
        return alerts;
    }
    
    /**
     * 获取特定类型的警报
     * @param alertType 警报类型
     * @return 警报列表
     */
    public List<TransactionAlert> getAlertsByType(String alertType) {
        List<TransactionAlert> alerts = new ArrayList<>();
        
        String sql = "SELECT a.*, s.name as student_name, " +
                     "CASE WHEN a.resolved_by IS NOT NULL THEN adm.name ELSE NULL END as resolver_name " +
                     "FROM TransactionAlert a " +
                     "JOIN Student s ON a.student_id = s.student_id " +
                     "LEFT JOIN Administrator adm ON a.resolved_by = adm.admin_id " +
                     "WHERE a.alert_type = ? " +
                     "ORDER BY a.alert_time DESC";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, alertType);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    alerts.add(extractAlertFromResultSet(rs));
                }
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "获取特定类型的风控警报失败", e);
        }
        
        return alerts;
    }
    
    /**
     * 根据ID获取警报
     * @param alertId 警报ID
     * @return 警报对象，不存在则返回null
     */
    public TransactionAlert getAlertById(int alertId) {
        String sql = "SELECT a.*, s.name as student_name, " +
                     "CASE WHEN a.resolved_by IS NOT NULL THEN adm.name ELSE NULL END as resolver_name " +
                     "FROM TransactionAlert a " +
                     "JOIN Student s ON a.student_id = s.student_id " +
                     "LEFT JOIN Administrator adm ON a.resolved_by = adm.admin_id " +
                     "WHERE a.alert_id = ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, alertId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return extractAlertFromResultSet(rs);
                }
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "根据ID获取风控警报失败", e);
        }
        
        return null;
    }
    
    /**
     * 根据解决状态获取警报
     * @param resolved 是否已解决
     * @return 警报列表
     */
    private List<TransactionAlert> getAlertsByStatus(boolean resolved) {
        List<TransactionAlert> alerts = new ArrayList<>();
        
        String sql = "SELECT a.*, s.name as student_name, " +
                     "CASE WHEN a.resolved_by IS NOT NULL THEN adm.name ELSE NULL END as resolver_name " +
                     "FROM TransactionAlert a " +
                     "JOIN Student s ON a.student_id = s.student_id " +
                     "LEFT JOIN Administrator adm ON a.resolved_by = adm.admin_id " +
                     "WHERE a.resolved = ? " +
                     "ORDER BY a.alert_time DESC";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setBoolean(1, resolved);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    alerts.add(extractAlertFromResultSet(rs));
                }
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "获取" + (resolved ? "已解决" : "未解决") + "的风控警报失败", e);
        }
        
        return alerts;
    }
    
    /**
     * 从ResultSet中提取警报对象的辅助方法
     * @param rs 结果集
     * @return 警报对象
     * @throws SQLException 如果数据库访问出错
     */
    private TransactionAlert extractAlertFromResultSet(ResultSet rs) throws SQLException {
        TransactionAlert alert = new TransactionAlert();
        alert.setAlertId(rs.getInt("alert_id"));
        alert.setStudentId(rs.getString("student_id"));
        alert.setAlertTime(rs.getTimestamp("alert_time"));
        alert.setAlertType(rs.getString("alert_type"));
        alert.setDescription(rs.getString("description"));
        alert.setResolved(rs.getBoolean("resolved"));
        alert.setResolvedBy(rs.getString("resolved_by"));
        alert.setResolvedAt(rs.getTimestamp("resolved_at"));
        alert.setStudentName(rs.getString("student_name"));
        alert.setResolverName(rs.getString("resolver_name"));
        return alert;
    }
} 