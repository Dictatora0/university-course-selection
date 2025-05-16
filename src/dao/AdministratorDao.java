package dao;

import model.Administrator;
import util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 管理员数据访问对象
 */
public class AdministratorDao {
    private static final Logger LOGGER = Logger.getLogger(AdministratorDao.class.getName());
    
    /**
     * 管理员登录验证
     * @param adminId 管理员ID
     * @param password 密码
     * @return 管理员对象，验证失败返回null
     */
    public Administrator login(String adminId, String password) {
        String sql = "SELECT * FROM Administrator WHERE admin_id = ? AND password = ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, adminId);
            stmt.setString(2, password);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Administrator admin = extractAdminFromResultSet(rs);
                    
                    // 更新最后登录时间
                    updateLastLogin(adminId);
                    
                    return admin;
                }
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "管理员登录验证失败", e);
        }
        
        return null;
    }
    
    /**
     * 添加新管理员
     * @param admin 管理员对象
     * @return 是否添加成功
     */
    public boolean addAdministrator(Administrator admin) {
        String sql = "INSERT INTO Administrator (admin_id, name, password, role, created_at) " +
                     "VALUES (?, ?, ?, ?, NOW())";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, admin.getAdminId());
            stmt.setString(2, admin.getName());
            stmt.setString(3, admin.getPassword());
            stmt.setString(4, admin.getRole());
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "添加管理员失败", e);
            return false;
        }
    }
    
    /**
     * 更新管理员信息
     * @param admin 管理员对象
     * @return 是否更新成功
     */
    public boolean updateAdministrator(Administrator admin) {
        String sql = "UPDATE Administrator SET name = ?, role = ? WHERE admin_id = ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, admin.getName());
            stmt.setString(2, admin.getRole());
            stmt.setString(3, admin.getAdminId());
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "更新管理员信息失败", e);
            return false;
        }
    }
    
    /**
     * 更新管理员密码
     * @param adminId 管理员ID
     * @param newPassword 新密码
     * @return 是否更新成功
     */
    public boolean updatePassword(String adminId, String newPassword) {
        String sql = "UPDATE Administrator SET password = ? WHERE admin_id = ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, newPassword);
            stmt.setString(2, adminId);
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "更新管理员密码失败", e);
            return false;
        }
    }
    
    /**
     * 删除管理员
     * @param adminId 管理员ID
     * @return 是否删除成功
     */
    public boolean deleteAdministrator(String adminId) {
        String sql = "DELETE FROM Administrator WHERE admin_id = ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, adminId);
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "删除管理员失败", e);
            return false;
        }
    }
    
    /**
     * 获取所有管理员
     * @return 管理员列表
     */
    public List<Administrator> getAllAdministrators() {
        List<Administrator> admins = new ArrayList<>();
        
        String sql = "SELECT * FROM Administrator ORDER BY admin_id";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                admins.add(extractAdminFromResultSet(rs));
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "获取所有管理员失败", e);
        }
        
        return admins;
    }
    
    /**
     * 根据ID获取管理员
     * @param adminId 管理员ID
     * @return 管理员对象，不存在则返回null
     */
    public Administrator getAdministratorById(String adminId) {
        String sql = "SELECT * FROM Administrator WHERE admin_id = ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, adminId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return extractAdminFromResultSet(rs);
                }
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "根据ID获取管理员失败", e);
        }
        
        return null;
    }
    
    /**
     * 获取特定角色的管理员
     * @param role 角色
     * @return 管理员列表
     */
    public List<Administrator> getAdministratorsByRole(String role) {
        List<Administrator> admins = new ArrayList<>();
        
        String sql = "SELECT * FROM Administrator WHERE role = ? ORDER BY admin_id";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, role);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    admins.add(extractAdminFromResultSet(rs));
                }
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "获取特定角色的管理员失败", e);
        }
        
        return admins;
    }
    
    /**
     * 更新最后登录时间
     * @param adminId 管理员ID
     */
    private void updateLastLogin(String adminId) {
        String sql = "UPDATE Administrator SET last_login = NOW() WHERE admin_id = ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, adminId);
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "更新管理员最后登录时间失败", e);
        }
    }
    
    /**
     * 从ResultSet中提取管理员对象的辅助方法
     * @param rs 结果集
     * @return 管理员对象
     * @throws SQLException 如果数据库访问出错
     */
    private Administrator extractAdminFromResultSet(ResultSet rs) throws SQLException {
        Administrator admin = new Administrator();
        admin.setAdminId(rs.getString("admin_id"));
        admin.setName(rs.getString("name"));
        admin.setPassword(rs.getString("password"));
        admin.setRole(rs.getString("role"));
        admin.setCreatedAt(rs.getTimestamp("created_at"));
        admin.setLastLogin(rs.getTimestamp("last_login"));
        return admin;
    }
} 