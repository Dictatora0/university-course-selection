package model;

import java.util.Date;

/**
 * 管理员模型类
 */
public class Administrator {
    private String adminId;         // 管理员ID
    private String name;            // 管理员姓名
    private String password;        // 密码
    private String role;            // 角色：SUPER_ADMIN(超级管理员), COURSE_ADMIN(课程管理员), STUDENT_ADMIN(学生管理员)
    private Date createdAt;         // 创建时间
    private Date lastLogin;         // 最后登录时间
    
    public Administrator() {
    }
    
    public Administrator(String adminId, String name, String password, String role) {
        this.adminId = adminId;
        this.name = name;
        this.password = password;
        this.role = role;
    }
    
    // Getters and Setters
    public String getAdminId() {
        return adminId;
    }
    
    public void setAdminId(String adminId) {
        this.adminId = adminId;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public String getRole() {
        return role;
    }
    
    public void setRole(String role) {
        this.role = role;
    }
    
    public Date getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
    
    public Date getLastLogin() {
        return lastLogin;
    }
    
    public void setLastLogin(Date lastLogin) {
        this.lastLogin = lastLogin;
    }
    
    /**
     * 检查是否为超级管理员
     */
    public boolean isSuperAdmin() {
        return "SUPER_ADMIN".equals(this.role);
    }
    
    /**
     * 检查是否为课程管理员
     */
    public boolean isCourseAdmin() {
        return "COURSE_ADMIN".equals(this.role) || isSuperAdmin();
    }
    
    /**
     * 检查是否为学生管理员
     */
    public boolean isStudentAdmin() {
        return "STUDENT_ADMIN".equals(this.role) || isSuperAdmin();
    }
} 