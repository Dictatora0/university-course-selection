package dao;

import model.Department;
import util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 院系数据访问对象，处理与Department表相关的数据库操作
 */
public class DepartmentDAO {
    
    /**
     * 查询所有院系
     */
    public List<Department> findAll() {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        List<Department> departments = new ArrayList<>();
        
        try {
            conn = DBConnection.getConnection();
            String sql = "SELECT * FROM Department";
            pstmt = conn.prepareStatement(sql);
            rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Department department = new Department();
                department.setDeptId(rs.getString("dept_id"));
                department.setDeptName(rs.getString("dept_name"));
                departments.add(department);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBConnection.close(conn, pstmt, rs);
        }
        
        return departments;
    }
    
    /**
     * 根据ID查询院系
     */
    public Department findById(String deptId) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        Department department = null;
        
        try {
            conn = DBConnection.getConnection();
            String sql = "SELECT * FROM Department WHERE dept_id = ?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, deptId);
            rs = pstmt.executeQuery();
            
            if (rs.next()) {
                department = new Department();
                department.setDeptId(rs.getString("dept_id"));
                department.setDeptName(rs.getString("dept_name"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBConnection.close(conn, pstmt, rs);
        }
        
        return department;
    }
    
    /**
     * 添加院系
     */
    public boolean add(Department department) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        boolean success = false;
        
        try {
            conn = DBConnection.getConnection();
            String sql = "INSERT INTO Department (dept_id, dept_name) VALUES (?, ?)";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, department.getDeptId());
            pstmt.setString(2, department.getDeptName());
            
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
     * 更新院系
     */
    public boolean update(Department department) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        boolean success = false;
        
        try {
            conn = DBConnection.getConnection();
            String sql = "UPDATE Department SET dept_name = ? WHERE dept_id = ?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, department.getDeptName());
            pstmt.setString(2, department.getDeptId());
            
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
     * 删除院系
     */
    public boolean delete(String deptId) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        boolean success = false;
        
        try {
            conn = DBConnection.getConnection();
            String sql = "DELETE FROM Department WHERE dept_id = ?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, deptId);
            
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