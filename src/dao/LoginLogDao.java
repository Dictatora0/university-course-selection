package dao;

import model.LoginLog;
import util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 登录日志数据访问对象
 */
public class LoginLogDao {
    private static final Logger LOGGER = Logger.getLogger(LoginLogDao.class.getName());
    
    /**
     * 记录登录日志
     * @param loginLog 登录日志对象
     * @return 是否记录成功
     */
    public boolean recordLogin(LoginLog loginLog) {
        String sql = "INSERT INTO LoginLog (student_id, login_time, ip_address, device_info) " +
                     "VALUES (?, NOW(), ?, ?)";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, loginLog.getStudentId());
            stmt.setString(2, loginLog.getIpAddress());
            stmt.setString(3, loginLog.getDeviceInfo());
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "记录登录日志失败", e);
            return false;
        }
    }
    
    /**
     * 获取学生的登录日志
     * @param studentId 学生ID
     * @return 登录日志列表
     */
    public List<LoginLog> getLoginLogs(String studentId) {
        List<LoginLog> logs = new ArrayList<>();
        
        String sql = "SELECT l.*, s.name as student_name " +
                     "FROM LoginLog l " +
                     "JOIN Student s ON l.student_id = s.student_id " +
                     "WHERE l.student_id = ? " +
                     "ORDER BY l.login_time DESC";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, studentId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    logs.add(extractLoginLogFromResultSet(rs));
                }
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "获取学生登录日志失败", e);
        }
        
        return logs;
    }
    
    /**
     * 获取所有登录日志
     * @param limit 限制返回条数
     * @param offset 偏移量
     * @return 登录日志列表
     */
    public List<LoginLog> getAllLoginLogs(int limit, int offset) {
        List<LoginLog> logs = new ArrayList<>();
        
        String sql = "SELECT l.*, s.name as student_name " +
                     "FROM LoginLog l " +
                     "JOIN Student s ON l.student_id = s.student_id " +
                     "ORDER BY l.login_time DESC " +
                     "LIMIT ? OFFSET ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, limit);
            stmt.setInt(2, offset);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    logs.add(extractLoginLogFromResultSet(rs));
                }
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "获取所有登录日志失败", e);
        }
        
        return logs;
    }
    
    /**
     * 获取指定时间段内的登录日志
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 登录日志列表
     */
    public List<LoginLog> getLoginLogsByDateRange(Date startDate, Date endDate) {
        List<LoginLog> logs = new ArrayList<>();
        
        String sql = "SELECT l.*, s.name as student_name " +
                     "FROM LoginLog l " +
                     "JOIN Student s ON l.student_id = s.student_id " +
                     "WHERE l.login_time BETWEEN ? AND ? " +
                     "ORDER BY l.login_time DESC";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setTimestamp(1, new Timestamp(startDate.getTime()));
            stmt.setTimestamp(2, new Timestamp(endDate.getTime()));
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    logs.add(extractLoginLogFromResultSet(rs));
                }
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "获取指定时间段内的登录日志失败", e);
        }
        
        return logs;
    }
    
    /**
     * 获取登录日志总数
     * @return 日志总数
     */
    public int getTotalLoginLogsCount() {
        String sql = "SELECT COUNT(*) FROM LoginLog";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                return rs.getInt(1);
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "获取登录日志总数失败", e);
        }
        
        return 0;
    }
    
    /**
     * 获取可疑登录记录（同一学生短时间内多IP登录）
     * @param timeWindowMinutes 时间窗口（分钟）
     * @return 可疑登录日志列表
     */
    public List<LoginLog> getSuspiciousLogins(int timeWindowMinutes) {
        List<LoginLog> suspiciousLogs = new ArrayList<>();
        
        String sql = "SELECT l.*, s.name as student_name FROM LoginLog l " +
                     "JOIN Student s ON l.student_id = s.student_id " +
                     "WHERE l.student_id IN (" +
                     "  SELECT DISTINCT student_id FROM LoginLog " +
                     "  WHERE login_time > DATE_SUB(NOW(), INTERVAL ? MINUTE) " +
                     "  GROUP BY student_id, ip_address " +
                     "  HAVING COUNT(DISTINCT ip_address) > 1" +
                     ") AND l.login_time > DATE_SUB(NOW(), INTERVAL ? MINUTE) " +
                     "ORDER BY l.student_id, l.login_time DESC";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, timeWindowMinutes);
            stmt.setInt(2, timeWindowMinutes);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    suspiciousLogs.add(extractLoginLogFromResultSet(rs));
                }
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "获取可疑登录记录失败", e);
        }
        
        return suspiciousLogs;
    }
    
    /**
     * 从ResultSet中提取登录日志对象的辅助方法
     * @param rs 结果集
     * @return 登录日志对象
     * @throws SQLException 如果数据库访问出错
     */
    private LoginLog extractLoginLogFromResultSet(ResultSet rs) throws SQLException {
        LoginLog log = new LoginLog();
        log.setLogId(rs.getLong("log_id"));
        log.setStudentId(rs.getString("student_id"));
        log.setLoginTime(rs.getTimestamp("login_time"));
        log.setIpAddress(rs.getString("ip_address"));
        log.setDeviceInfo(rs.getString("device_info"));
        log.setStudentName(rs.getString("student_name"));
        return log;
    }
    
    /**
     * 获取指定天数内活跃的学生数量
     * @param days 天数
     * @return 活跃学生数量
     */
    public int getActiveStudentsCount(int days) {
        String sql = "SELECT COUNT(DISTINCT student_id) FROM LoginLog " +
                    "WHERE login_time >= DATE_SUB(CURRENT_DATE(), INTERVAL ? DAY)";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, days);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "获取活跃学生数量失败", e);
        }
        
        return 0;
    }
    
    /**
     * 获取今日活跃的学生数量
     * @return 今日活跃学生数量
     */
    public int getTodayActiveStudentsCount() {
        return getActiveStudentsCount(1);
    }
} 