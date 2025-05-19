package dao;

import model.TransactionControl;
import util.DBConnection;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 交易控制数据访问对象
 */
public class TransactionControlDao {
    private static final Logger LOGGER = Logger.getLogger(TransactionControlDao.class.getName());
    
    /**
     * 获取当前有效的交易控制设置
     * @return 交易控制对象，不存在则返回null
     */
    public TransactionControl getCurrentControl() {
        String sql = "SELECT tc.*, a.name as updater_name " +
                     "FROM TransactionControl tc " +
                     "LEFT JOIN Administrator a ON tc.updated_by = a.admin_id " +
                     "WHERE tc.enabled = 1 " +
                     "ORDER BY tc.last_updated DESC LIMIT 1";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                return extractControlFromResultSet(rs);
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "获取当前有效的交易控制设置失败", e);
        }
        
        return null;
    }
    
    /**
     * 添加新的交易控制设置
     * @param control 交易控制对象
     * @param adminId 管理员ID
     * @return 新控制ID，失败返回-1
     */
    public int addControl(TransactionControl control, String adminId) {
        // 首先禁用所有现有的控制设置
        disableAllControls();
        
        String sql = "INSERT INTO TransactionControl " +
                     "(max_single_amount, daily_limit, max_daily_transactions, " +
                     "frequent_transfer_time_window, frequent_transfer_threshold, " +
                     "enabled, last_updated, updated_by) " +
                     "VALUES (?, ?, ?, ?, ?, ?, NOW(), ?)";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setBigDecimal(1, control.getMaxSingleAmount());
            stmt.setBigDecimal(2, control.getDailyLimit());
            stmt.setInt(3, control.getMaxDailyTransactions());
            stmt.setInt(4, control.getFrequentTransferTimeWindow());
            stmt.setInt(5, control.getFrequentTransferThreshold());
            stmt.setBoolean(6, control.isEnabled());
            stmt.setString(7, adminId);
            
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "添加新的交易控制设置失败", e);
        }
        
        return -1;
    }
    
    /**
     * 更新交易控制设置
     * @param control 交易控制对象
     * @param adminId 管理员ID
     * @return 是否更新成功
     */
    public boolean updateControl(TransactionControl control, String adminId) {
        String sql = "UPDATE TransactionControl SET " +
                     "max_single_amount = ?, daily_limit = ?, max_daily_transactions = ?, " +
                     "frequent_transfer_time_window = ?, frequent_transfer_threshold = ?, " +
                     "enabled = ?, last_updated = NOW(), updated_by = ? " +
                     "WHERE control_id = ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setBigDecimal(1, control.getMaxSingleAmount());
            stmt.setBigDecimal(2, control.getDailyLimit());
            stmt.setInt(3, control.getMaxDailyTransactions());
            stmt.setInt(4, control.getFrequentTransferTimeWindow());
            stmt.setInt(5, control.getFrequentTransferThreshold());
            stmt.setBoolean(6, control.isEnabled());
            stmt.setString(7, adminId);
            stmt.setInt(8, control.getControlId());
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "更新交易控制设置失败", e);
            return false;
        }
    }
    
    /**
     * 启用或禁用交易控制
     * @param controlId 控制ID
     * @param enabled 是否启用
     * @param adminId 管理员ID
     * @return 是否操作成功
     */
    public boolean setControlEnabled(int controlId, boolean enabled, String adminId) {
        // 如果要启用，先禁用所有其他控制设置
        if (enabled) {
            disableAllControls();
        }
        
        String sql = "UPDATE TransactionControl SET enabled = ?, last_updated = NOW(), updated_by = ? " +
                     "WHERE control_id = ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setBoolean(1, enabled);
            stmt.setString(2, adminId);
            stmt.setInt(3, controlId);
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "设置交易控制状态失败", e);
            return false;
        }
    }
    
    /**
     * 获取所有交易控制设置历史
     * @return 交易控制列表
     */
    public List<TransactionControl> getAllControls() {
        List<TransactionControl> controls = new ArrayList<>();
        
        String sql = "SELECT tc.*, a.name as updater_name " +
                     "FROM TransactionControl tc " +
                     "LEFT JOIN Administrator a ON tc.updated_by = a.admin_id " +
                     "ORDER BY tc.last_updated DESC";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                controls.add(extractControlFromResultSet(rs));
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "获取所有交易控制设置历史失败", e);
        }
        
        return controls;
    }
    
    /**
     * 根据ID获取交易控制设置
     * @param controlId 控制ID
     * @return 交易控制对象，不存在则返回null
     */
    public TransactionControl getControlById(int controlId) {
        String sql = "SELECT tc.*, a.name as updater_name " +
                     "FROM TransactionControl tc " +
                     "LEFT JOIN Administrator a ON tc.updated_by = a.admin_id " +
                     "WHERE tc.control_id = ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, controlId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return extractControlFromResultSet(rs);
                }
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "根据ID获取交易控制设置失败", e);
        }
        
        return null;
    }
    
    /**
     * 禁用所有交易控制设置
     */
    private void disableAllControls() {
        String sql = "UPDATE TransactionControl SET enabled = 0";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "禁用所有交易控制设置失败", e);
        }
    }
    
    /**
     * 从ResultSet中提取交易控制对象的辅助方法
     * @param rs 结果集
     * @return 交易控制对象
     * @throws SQLException 如果数据库访问出错
     */
    private TransactionControl extractControlFromResultSet(ResultSet rs) throws SQLException {
        TransactionControl control = new TransactionControl();
        control.setControlId(rs.getInt("control_id"));
        control.setMaxSingleAmount(rs.getBigDecimal("max_single_amount"));
        control.setDailyLimit(rs.getBigDecimal("daily_limit"));
        control.setMaxDailyTransactions(rs.getInt("max_daily_transactions"));
        control.setFrequentTransferTimeWindow(rs.getInt("frequent_transfer_time_window"));
        control.setFrequentTransferThreshold(rs.getInt("frequent_transfer_threshold"));
        control.setEnabled(rs.getBoolean("enabled"));
        control.setLastUpdated(rs.getTimestamp("last_updated"));
        control.setUpdatedBy(rs.getString("updated_by"));
        control.setUpdaterName(rs.getString("updater_name"));
        return control;
    }
    
    /**
     * 初始化默认交易控制设置（如果不存在）
     * @param adminId 管理员ID
     * @return 是否初始化成功
     */
    public boolean initializeDefaultControlIfNeeded(String adminId) {
        // 检查是否存在任何控制设置
        if (getCurrentControl() != null) {
            return true; // 已存在，不需要初始化
        }
        
        // 创建默认设置
        TransactionControl defaultControl = new TransactionControl(
                new BigDecimal("1000.00"),  // 单笔最大金额1000元
                new BigDecimal("5000.00"),  // 日累计限额5000元
                20                          // 每日最大交易次数20次
        );
        
        // 设置频繁转账预警参数
        defaultControl.setFrequentTransferTimeWindow(30);  // 30分钟内
        defaultControl.setFrequentTransferThreshold(3);    // 3次及以上转账视为频繁
        
        // 添加到数据库
        int controlId = addControl(defaultControl, adminId);
        return controlId > 0;
    }
} 