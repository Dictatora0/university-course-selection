package servlet;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import dao.AdministratorDao;
import dao.StudentDAO;
import dao.CourseDAO;
import dao.EnrollmentDAO;
import model.Administrator;
import util.ResponseUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 管理员相关请求处理Servlet
 */
@WebServlet("/api/admin/*")
public class AdminServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(AdminServlet.class.getName());
    private static final Gson GSON = new Gson();
    private final AdministratorDao administratorDao = new AdministratorDao();

    /**
     * 处理GET请求
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        
        if (pathInfo == null || pathInfo.equals("/")) {
            // 获取当前登录的管理员信息
            handleGetCurrentAdmin(req, resp);
        } else if (pathInfo.equals("/logout")) {
            // 管理员退出登录
            handleLogout(req, resp);
        } else if (pathInfo.equals("/all")) {
            // 获取所有管理员列表（仅超级管理员可访问）
            handleGetAllAdmins(req, resp);
        } else if (pathInfo.equals("/stats/overview")) {
            // 获取系统概览统计数据
            handleGetSystemOverview(req, resp);
        } else if (pathInfo.equals("/stats/students")) {
            // 获取学生统计数据
            handleGetStudentStats(req, resp);
        } else if (pathInfo.equals("/stats/courses")) {
            // 获取课程统计数据
            handleGetCourseStats(req, resp);
        } else if (pathInfo.equals("/stats/enrollments")) {
            // 获取选课统计数据
            handleGetEnrollmentStats(req, resp);
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    /**
     * 处理POST请求
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        
        if (pathInfo == null || pathInfo.equals("/")) {
            // 创建新管理员（仅超级管理员可操作）
            handleCreateAdmin(req, resp);
        } else if (pathInfo.equals("/login")) {
            // 管理员登录
            handleLogin(req, resp);
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    /**
     * 处理PUT请求
     */
    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        
        if (pathInfo != null && pathInfo.startsWith("/")) {
            String adminId = pathInfo.substring(1);
            // 更新管理员信息（仅超级管理员可操作）
            handleUpdateAdmin(req, resp, adminId);
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    /**
     * 处理DELETE请求
     */
    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        
        if (pathInfo != null && pathInfo.startsWith("/")) {
            String adminId = pathInfo.substring(1);
            // 删除管理员（仅超级管理员可操作）
            handleDeleteAdmin(req, resp, adminId);
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    /**
     * 处理管理员登录
     */
    private void handleLogin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            JsonObject requestBody = GSON.fromJson(req.getReader(), JsonObject.class);
            String adminId = requestBody.get("adminId").getAsString();
            String password = requestBody.get("password").getAsString();

            Administrator admin = administratorDao.login(adminId, password);

            if (admin != null) {
                // 登录成功，将管理员信息存入Session
                HttpSession session = req.getSession();
                session.setAttribute("admin", admin);
                session.setAttribute("userType", "admin");

                // 移除密码字段，避免返回给前端
                admin.setPassword(null);

                ResponseUtil.sendSuccessResponse(resp, "登录成功", admin);
            } else {
                ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_UNAUTHORIZED, "管理员ID或密码错误");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "管理员登录失败", e);
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "登录失败：" + e.getMessage());
        }
    }

    /**
     * 处理获取当前登录的管理员信息
     */
    private void handleGetCurrentAdmin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        
        if (session != null && session.getAttribute("admin") != null) {
            Administrator admin = (Administrator) session.getAttribute("admin");
            
            // 复制一份，移除密码等敏感信息
            Administrator safeAdmin = new Administrator();
            safeAdmin.setAdminId(admin.getAdminId());
            safeAdmin.setName(admin.getName());
            safeAdmin.setRole(admin.getRole());
            safeAdmin.setCreatedAt(admin.getCreatedAt());
            safeAdmin.setLastLogin(admin.getLastLogin());
            
            ResponseUtil.sendSuccessResponse(resp, "成功获取管理员信息", safeAdmin);
        } else {
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_UNAUTHORIZED, "未登录或会话已过期");
        }
    }

    /**
     * 处理管理员退出登录
     */
    private void handleLogout(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        
        if (session != null) {
            session.invalidate();
        }
        
        ResponseUtil.sendSuccessResponse(resp, "退出登录成功", null);
    }

    /**
     * 处理获取所有管理员列表（仅超级管理员可访问）
     */
    private void handleGetAllAdmins(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // 检查是否是超级管理员
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("admin") == null) {
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_UNAUTHORIZED, "未登录或会话已过期");
            return;
        }
        
        Administrator currentAdmin = (Administrator) session.getAttribute("admin");
        if (!currentAdmin.isSuperAdmin()) {
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_FORBIDDEN, "权限不足，仅超级管理员可执行此操作");
            return;
        }
        
        List<Administrator> admins = administratorDao.getAllAdministrators();
        
        // 移除所有管理员的密码字段
        for (Administrator admin : admins) {
            admin.setPassword(null);
        }
        
        ResponseUtil.sendSuccessResponse(resp, "成功获取所有管理员列表", admins);
    }

    /**
     * 处理创建新管理员（仅超级管理员可操作）
     */
    private void handleCreateAdmin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // 检查是否是超级管理员
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("admin") == null) {
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_UNAUTHORIZED, "未登录或会话已过期");
            return;
        }
        
        Administrator currentAdmin = (Administrator) session.getAttribute("admin");
        if (!currentAdmin.isSuperAdmin()) {
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_FORBIDDEN, "权限不足，仅超级管理员可执行此操作");
            return;
        }
        
        try {
            Administrator newAdmin = GSON.fromJson(req.getReader(), Administrator.class);
            
            // 简单验证
            if (newAdmin.getAdminId() == null || newAdmin.getAdminId().isEmpty() ||
                newAdmin.getName() == null || newAdmin.getName().isEmpty() ||
                newAdmin.getPassword() == null || newAdmin.getPassword().isEmpty() ||
                newAdmin.getRole() == null || newAdmin.getRole().isEmpty()) {
                ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, "管理员信息不完整，ID、姓名、密码和角色不能为空");
                return;
            }
            
            // 检查是否存在同ID的管理员
            if (administratorDao.getAdministratorById(newAdmin.getAdminId()) != null) {
                ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_CONFLICT, "管理员ID已存在");
                return;
            }
            
            boolean success = administratorDao.addAdministrator(newAdmin);
            
            if (success) {
                // 移除密码字段
                newAdmin.setPassword(null);
                ResponseUtil.sendSuccessResponse(resp, "管理员创建成功", newAdmin);
            } else {
                ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "管理员创建失败");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "创建管理员失败", e);
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "创建管理员失败：" + e.getMessage());
        }
    }

    /**
     * 处理更新管理员信息（仅超级管理员可操作）
     */
    private void handleUpdateAdmin(HttpServletRequest req, HttpServletResponse resp, String adminId) throws IOException {
        // 检查是否是超级管理员
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("admin") == null) {
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_UNAUTHORIZED, "未登录或会话已过期");
            return;
        }
        
        Administrator currentAdmin = (Administrator) session.getAttribute("admin");
        if (!currentAdmin.isSuperAdmin() && !currentAdmin.getAdminId().equals(adminId)) {
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_FORBIDDEN, "权限不足，仅超级管理员或本人可执行此操作");
            return;
        }
        
        try {
            JsonObject requestBody = GSON.fromJson(req.getReader(), JsonObject.class);
            
            // 获取现有管理员信息
            Administrator admin = administratorDao.getAdministratorById(adminId);
            if (admin == null) {
                ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_NOT_FOUND, "管理员不存在");
                return;
            }
            
            // 更新基本信息
            if (requestBody.has("name")) {
                admin.setName(requestBody.get("name").getAsString());
            }
            
            // 仅超级管理员可更新角色
            if (requestBody.has("role") && currentAdmin.isSuperAdmin()) {
                admin.setRole(requestBody.get("role").getAsString());
            }
            
            // 处理密码更新
            if (requestBody.has("password")) {
                String newPassword = requestBody.get("password").getAsString();
                boolean passwordSuccess = administratorDao.updatePassword(adminId, newPassword);
                
                if (!passwordSuccess) {
                    ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "更新密码失败");
                    return;
                }
            }
            
            boolean success = administratorDao.updateAdministrator(admin);
            
            if (success) {
                // 移除密码字段
                admin.setPassword(null);
                ResponseUtil.sendSuccessResponse(resp, "管理员信息更新成功", admin);
                
                // 如果更新的是当前登录的管理员，同步更新Session
                if (adminId.equals(currentAdmin.getAdminId())) {
                    session.setAttribute("admin", admin);
                }
            } else {
                ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "管理员信息更新失败");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "更新管理员信息失败", e);
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "更新管理员信息失败：" + e.getMessage());
        }
    }

    /**
     * 处理删除管理员（仅超级管理员可操作）
     */
    private void handleDeleteAdmin(HttpServletRequest req, HttpServletResponse resp, String adminId) throws IOException {
        // 检查是否是超级管理员
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("admin") == null) {
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_UNAUTHORIZED, "未登录或会话已过期");
            return;
        }
        
        Administrator currentAdmin = (Administrator) session.getAttribute("admin");
        if (!currentAdmin.isSuperAdmin()) {
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_FORBIDDEN, "权限不足，仅超级管理员可执行此操作");
            return;
        }
        
        // 防止删除自己
        if (adminId.equals(currentAdmin.getAdminId())) {
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, "不能删除当前登录的管理员账号");
            return;
        }
        
        // 检查是否存在此管理员
        Administrator admin = administratorDao.getAdministratorById(adminId);
        if (admin == null) {
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_NOT_FOUND, "管理员不存在");
            return;
        }
        
        boolean success = administratorDao.deleteAdministrator(adminId);
        
        if (success) {
            ResponseUtil.sendSuccessResponse(resp, "管理员删除成功", null);
        } else {
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "管理员删除失败");
        }
    }

    /**
     * 处理获取系统概览统计数据
     */
    private void handleGetSystemOverview(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // 检查管理员登录状态
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("admin") == null) {
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_UNAUTHORIZED, "未登录或会话已过期");
            return;
        }
        
        try {
            // 初始化各DAO
            StudentDAO studentDAO = new StudentDAO();
            CourseDAO courseDAO = new CourseDAO();
            EnrollmentDAO enrollmentDAO = new EnrollmentDAO();
            
            // 获取各项统计数据
            int totalStudents = studentDAO.getTotalStudentCount();
            int totalCourses = courseDAO.getTotalCourseCount();
            int totalEnrollments = enrollmentDAO.getTotalEnrollmentCount();
            int recentStudents = studentDAO.getRecentRegisteredStudentCount(7); // 最近7天注册的学生
            
            // 构建返回数据
            Map<String, Object> overviewData = new HashMap<>();
            overviewData.put("totalStudents", totalStudents);
            overviewData.put("totalCourses", totalCourses);
            overviewData.put("totalEnrollments", totalEnrollments);
            overviewData.put("recentStudents", recentStudents);
            
            ResponseUtil.sendSuccessResponse(resp, "获取系统概览统计数据成功", overviewData);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "获取系统概览统计数据失败", e);
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                    "获取系统概览统计数据失败：" + e.getMessage());
        }
    }
    
    /**
     * 处理获取学生统计数据
     */
    private void handleGetStudentStats(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // 检查管理员登录状态
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("admin") == null) {
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_UNAUTHORIZED, "未登录或会话已过期");
            return;
        }
        
        try {
            StudentDAO studentDAO = new StudentDAO();
            
            // 获取学生相关统计数据
            int totalStudents = studentDAO.getTotalStudentCount();
            int recentStudents = studentDAO.getRecentRegisteredStudentCount(30); // 最近30天
            List<Object[]> genderDistribution = studentDAO.getGenderDistribution();
            
            // 构建返回数据
            Map<String, Object> studentStats = new HashMap<>();
            studentStats.put("totalStudents", totalStudents);
            studentStats.put("recentStudents", recentStudents);
            studentStats.put("genderDistribution", genderDistribution);
            
            ResponseUtil.sendSuccessResponse(resp, "获取学生统计数据成功", studentStats);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "获取学生统计数据失败", e);
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                    "获取学生统计数据失败：" + e.getMessage());
        }
    }
    
    /**
     * 处理获取课程统计数据
     */
    private void handleGetCourseStats(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // 检查管理员登录状态
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("admin") == null) {
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_UNAUTHORIZED, "未登录或会话已过期");
            return;
        }
        
        try {
            CourseDAO courseDAO = new CourseDAO();
            
            // 获取课程相关统计数据
            int totalCourses = courseDAO.getTotalCourseCount();
            List<Object[]> coursesByDepartment = courseDAO.getCoursesCountByDepartment();
            java.math.BigDecimal avgCredit = courseDAO.getAverageCourseCredit();
            List<Object[]> popularCourses = courseDAO.getMostPopularCourses(10); // 前10门热门课程
            
            // 构建返回数据
            Map<String, Object> courseStats = new HashMap<>();
            courseStats.put("totalCourses", totalCourses);
            courseStats.put("coursesByDepartment", coursesByDepartment);
            courseStats.put("averageCourseCredit", avgCredit);
            courseStats.put("popularCourses", popularCourses);
            
            ResponseUtil.sendSuccessResponse(resp, "获取课程统计数据成功", courseStats);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "获取课程统计数据失败", e);
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                    "获取课程统计数据失败：" + e.getMessage());
        }
    }
    
    /**
     * 处理获取选课统计数据
     */
    private void handleGetEnrollmentStats(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // 检查管理员登录状态
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("admin") == null) {
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_UNAUTHORIZED, "未登录或会话已过期");
            return;
        }
        
        try {
            EnrollmentDAO enrollmentDAO = new EnrollmentDAO();
            
            // 获取选课相关统计数据
            int totalEnrollments = enrollmentDAO.getTotalEnrollmentCount();
            List<Object[]> dailyStats = enrollmentDAO.getDailyEnrollmentStats(30); // 最近30天
            List<Object[]> averageGrades = enrollmentDAO.getAverageGradesByCourse();
            List<Object[]> gradeDistribution = enrollmentDAO.getGradeDistribution();
            
            // 构建返回数据
            Map<String, Object> enrollmentStats = new HashMap<>();
            enrollmentStats.put("totalEnrollments", totalEnrollments);
            enrollmentStats.put("dailyStatistics", dailyStats);
            enrollmentStats.put("averageGradesByCourse", averageGrades);
            enrollmentStats.put("gradeDistribution", gradeDistribution);
            
            ResponseUtil.sendSuccessResponse(resp, "获取选课统计数据成功", enrollmentStats);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "获取选课统计数据失败", e);
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                    "获取选课统计数据失败：" + e.getMessage());
        }
    }
} 