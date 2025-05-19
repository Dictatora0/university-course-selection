package servlet;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import dao.AdministratorDao;
import dao.StudentDAO;
import dao.CourseDAO;
import dao.EnrollmentDAO;
import dao.TransactionDAO;
import dao.LoginLogDao;
import model.Administrator;
import model.Student;
import model.Transaction;
import util.ResponseUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.ArrayList;
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
    private final StudentDAO studentDAO = new StudentDAO();
    private final CourseDAO courseDAO = new CourseDAO();
    private final EnrollmentDAO enrollmentDAO = new EnrollmentDAO();
    private final TransactionDAO transactionDAO = new TransactionDAO();
    private final LoginLogDao loginLogDao = new LoginLogDao();

    /**
     * 处理GET请求
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        
        HttpSession session = req.getSession(false);
        if (!isUserAdmin(session) && !isLoginPath(pathInfo)) {
             ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_UNAUTHORIZED, "未登录或会话已过期");
             return;
        }
        Administrator currentAdmin = isAdminSessionValid(session) ? (Administrator) session.getAttribute("admin") : null;
        
        if (pathInfo == null || pathInfo.equals("/")) {
            handleGetCurrentAdmin(req, resp);
        } else if (pathInfo.equals("/logout")) {
            handleLogout(req, resp);
        } else if (pathInfo.equals("/all")) {
            handleGetAllAdmins(req, resp);
        } else if (pathInfo.equals("/students")) {
            handleGetAllStudents(req, resp);
        } else if (pathInfo.startsWith("/stats")) {
            handleStatsRequests(req, resp, pathInfo, currentAdmin);
        } else if (pathInfo.startsWith("/transactions")) {
            handleTransactionRequests(req, resp, pathInfo, currentAdmin);
        } else if (pathInfo.equals("/students/search")) {
            handleSearchStudents(req, resp);
        } else if (pathInfo.startsWith("/students/")) {
            // 处理获取单个学生详情的请求
            String studentId = pathInfo.substring("/students/".length());
            // 移除可能的路径参数，例如 /students/S001/status
            int slashIndex = studentId.indexOf('/');
            if (slashIndex != -1) {
                studentId = studentId.substring(0, slashIndex);
            }
            handleGetStudentById(req, resp, studentId);
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private boolean isUserAdmin(HttpSession session) {
        return session != null && session.getAttribute("admin") != null && "admin".equals(session.getAttribute("userType"));
    }
    
    private boolean isAdminSessionValid(HttpSession session) {
        return session != null && session.getAttribute("admin") != null;
    }

    private boolean isLoginPath(String pathInfo) {
        return pathInfo != null && pathInfo.equals("/login");
    }

    private void handleStatsRequests(HttpServletRequest req, HttpServletResponse resp, String pathInfo, Administrator admin) throws IOException {
        if (admin == null) {
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_UNAUTHORIZED, "统计功能需要管理员登录");
            return;
        }
        
        if (pathInfo.equals("/stats/all")) {
            handleGetAllStatsCombined(req, resp, admin);
        } else if (pathInfo.equals("/stats/students")) {
            handleGetStudentStats(req, resp);
        } else if (pathInfo.equals("/stats/courses")) {
            handleGetCourseStats(req, resp);
        } else if (pathInfo.equals("/stats/enrollments")) {
            handleGetEnrollmentStats(req, resp);
        } else if (pathInfo.equals("/stats/activeUsers")) {
            handleGetActiveUsersCombined(req, resp, admin);
        } else if (pathInfo.equals("/stats/recentTransactions")) {
            handleGetRecentTransactionsCombined(req, resp, admin);
        } else if (pathInfo.equals("/transactions/recent")) {
            handleGetRecentTransactionsForAdmin(req, resp);
        } else {
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_NOT_FOUND, "未知的统计路径");
        }
    }

    /**
     * 处理交易记录相关请求
     */
    private void handleTransactionRequests(HttpServletRequest req, HttpServletResponse resp, String pathInfo, Administrator admin) throws IOException {
        if (admin == null) {
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_UNAUTHORIZED, "交易记录功能需要管理员登录");
            return;
        }
        
        // 只有超级管理员和学生管理员可以访问交易记录
        if (!admin.getRole().equals("SUPER_ADMIN") && !admin.getRole().equals("STUDENT_ADMIN")) {
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_FORBIDDEN, "您没有权限访问交易记录");
            return;
        }
        
        if (pathInfo.equals("/transactions")) {
            // 获取交易记录列表，支持筛选
            String typeFilter = req.getParameter("type");
            String startDate = req.getParameter("startDate");
            String endDate = req.getParameter("endDate");
            String studentId = req.getParameter("studentId");
            
            try {
                List<Transaction> transactions = new ArrayList<>();
                
                // 根据筛选条件获取交易记录
                if (studentId != null && !studentId.isEmpty()) {
                    // 按学生ID搜索
                    transactions = transactionDAO.findByStudentId(studentId);
                } else {
                    // 按其他条件筛选
                    // 这里需要在TransactionDAO中添加相应的方法
                    transactions = transactionDAO.getRecentTransactions(100); // 临时方案，返回最近100条
                }
                
                ResponseUtil.sendSuccessResponse(resp, "成功获取交易记录", transactions);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "获取交易记录失败", e);
                ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "获取交易记录失败: " + e.getMessage());
            }
        } else if (pathInfo.equals("/transactions/recent")) {
            handleGetRecentTransactionsForAdmin(req, resp);
        } else {
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_NOT_FOUND, "未知的交易记录路径");
        }
    }

    /**
     * 处理获取系统概览统计数据
     */
    private void handleGetSystemOverview(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            int totalStudents = studentDAO.getTotalStudentCount();
            int totalCourses = courseDAO.getTotalCourseCount();
            int totalEnrollments = enrollmentDAO.getTotalEnrollmentCount();
            int recentStudents = studentDAO.getRecentRegisteredStudentCount(7);
            
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
        try {
            int totalStudents = studentDAO.getTotalStudentCount();
            ResponseUtil.sendSuccessResponse(resp, "获取学生统计数据成功", totalStudents);
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
        try {
            int totalCourses = courseDAO.getTotalCourseCount();
            ResponseUtil.sendSuccessResponse(resp, "获取课程统计数据成功", totalCourses);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "获取课程统计数据失败", e);
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                    "获取课程统计数据失败：" + e.getMessage());
        }
    }
    
    /**
     * 处理获取选课统计数据 (Retained for /stats/enrollments)
     */
    private void handleGetEnrollmentStats(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            int totalEnrollments = enrollmentDAO.getTotalEnrollmentCount();
            List<Object[]> dailyStats = enrollmentDAO.getDailyEnrollmentStats(30); // 最近30天
            List<Object[]> averageGrades = enrollmentDAO.getAverageGradesByCourse();
            List<Object[]> gradeDistribution = enrollmentDAO.getGradeDistribution();
            
            Map<String, Object> enrollmentStats = new HashMap<>();
            enrollmentStats.put("totalEnrollments", totalEnrollments);
            enrollmentStats.put("dailyEnrollmentStatisticsLast30Days", dailyStats); 
            enrollmentStats.put("averageGradesByCourse", averageGrades);
            enrollmentStats.put("overallGradeDistribution", gradeDistribution);
            
            ResponseUtil.sendSuccessResponse(resp, "获取选课统计数据成功", enrollmentStats);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "获取选课统计数据失败", e);
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                    "获取选课统计数据失败：" + e.getMessage());
        }
    }

    /**
     * 获取全部统计信息
     */
    private void handleGetAllStatsCombined(HttpServletRequest req, HttpServletResponse resp, Administrator admin) throws IOException {
        try {
            Map<String, Object> stats = new HashMap<>();
            stats.put("studentCount", studentDAO.getTotalStudentCount());
            stats.put("courseCount", courseDAO.getTotalCourseCount());
            stats.put("activeUsersToday", loginLogDao.getTodayActiveStudentsCount());
            stats.put("recentTransactions", transactionDAO.getRecentTransactions(10));
            
            ResponseUtil.sendSuccessResponse(resp, "成功获取所有组合统计信息", stats);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "获取所有组合统计信息失败", e);
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "获取所有组合统计信息失败: " + e.getMessage());
        }
    }

    /**
     * 获取今日活跃用户数
     */
    private void handleGetActiveUsersCombined(HttpServletRequest req, HttpServletResponse resp, Administrator admin) throws IOException {
        try {
            int count = loginLogDao.getTodayActiveStudentsCount();
            ResponseUtil.sendSuccessResponse(resp, "成功获取今日活跃用户数", count);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "获取今日活跃用户数失败", e);
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "获取今日活跃用户数失败: " + e.getMessage());
        }
    }

    /**
     * 获取最近交易记录（管理员专用API）
     */
    private void handleGetRecentTransactionsForAdmin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            List<Transaction> transactions = transactionDAO.getRecentTransactions(20);
            ResponseUtil.sendSuccessResponse(resp, "成功获取最近交易记录", transactions);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "获取最近交易记录失败", e);
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "获取最近交易记录失败");
        }
    }

    /**
     * 获取最近交易记录（合并数据）
     */
    private void handleGetRecentTransactionsCombined(HttpServletRequest req, HttpServletResponse resp, Administrator admin) throws IOException {
        try {
            int limit = 10;
            String limitParam = req.getParameter("limit");
            if (limitParam != null && !limitParam.isEmpty()) {
                try {
                    limit = Integer.parseInt(limitParam);
                    if (limit > 100) limit = 100;
                    if (limit <= 0) limit = 10;
                } catch (NumberFormatException e) {
                    // Ignore, use default
                }
            }
            List<Transaction> transactions = transactionDAO.getRecentTransactions(limit);
            ResponseUtil.sendSuccessResponse(resp, "成功获取最近交易记录", transactions);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "获取最近交易记录失败", e);
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "获取最近交易记录失败: " + e.getMessage());
        }
    }

    /**
     * 处理POST请求
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        
        HttpSession session = req.getSession(false);
        if (pathInfo != null && pathInfo.equals("/login")) {
            handleLogin(req, resp);
        } else {
            if (!isUserAdmin(session)) {
                 ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_UNAUTHORIZED, "未登录或会话已过期");
                 return;
            }
            Administrator currentAdmin = (Administrator) session.getAttribute("admin");

            if (pathInfo == null || pathInfo.equals("/")) {
                handleCreateAdmin(req, resp);
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        }
    }

    /**
     * 处理PUT请求
     */
    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        
        HttpSession session = req.getSession(false);
        if (!isUserAdmin(session)) {
             ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_UNAUTHORIZED, "未登录或会话已过期");
             return;
        }

        if (pathInfo != null && pathInfo.matches("^/[^/]+/?$")) {
            String adminId = pathInfo.substring(1).replaceAll("/$", "");
            handleUpdateAdmin(req, resp, adminId);
        } else if (pathInfo != null && pathInfo.matches("^/students/[^/]+/status$")) {
            // 提取学生ID
            String studentId = pathInfo.replaceAll("^/students/([^/]+)/status$", "$1");
            handleUpdateStudentStatus(req, resp, studentId);
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "无效的更新路径: " + pathInfo);
        }
    }

    /**
     * 处理DELETE请求
     */
    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        
        HttpSession session = req.getSession(false);
        if (!isUserAdmin(session)) {
             ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_UNAUTHORIZED, "未登录或会话已过期");
             return;
        }

        if (pathInfo != null && pathInfo.matches("^/[^/]+/?$")) {
            String adminId = pathInfo.substring(1).replaceAll("/$", "");
            handleDeleteAdmin(req, resp, adminId);
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "无效的删除路径: " + pathInfo);
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
                HttpSession session = req.getSession();
                session.setAttribute("admin", admin);
                session.setAttribute("userType", "admin");
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
            Administrator admin = (Administrator) session.getAttribute("admin");
            Administrator safeAdmin = new Administrator();
            safeAdmin.setAdminId(admin.getAdminId());
            safeAdmin.setName(admin.getName());
            safeAdmin.setRole(admin.getRole());
            safeAdmin.setCreatedAt(admin.getCreatedAt());
            safeAdmin.setLastLogin(admin.getLastLogin());
            ResponseUtil.sendSuccessResponse(resp, "成功获取管理员信息", safeAdmin);
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
        HttpSession session = req.getSession(false);
        Administrator currentAdmin = (Administrator) session.getAttribute("admin");

        if (!currentAdmin.isSuperAdmin()) {
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_FORBIDDEN, "权限不足，仅超级管理员可执行此操作");
            return;
        }
        
        List<Administrator> admins = administratorDao.getAllAdministrators();
        for (Administrator adm : admins) {
            adm.setPassword(null);
        }
        ResponseUtil.sendSuccessResponse(resp, "成功获取所有管理员列表", admins);
    }

    /**
     * 处理创建新管理员（仅超级管理员可操作）
     */
    private void handleCreateAdmin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        Administrator currentAdmin = (Administrator) session.getAttribute("admin");

        if (!currentAdmin.isSuperAdmin()) {
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_FORBIDDEN, "权限不足，仅超级管理员可执行此操作");
            return;
        }
        
        try {
            Administrator newAdmin = GSON.fromJson(req.getReader(), Administrator.class);
            if (newAdmin.getAdminId() == null || newAdmin.getAdminId().isEmpty() ||
                newAdmin.getName() == null || newAdmin.getName().isEmpty() ||
                newAdmin.getPassword() == null || newAdmin.getPassword().isEmpty() ||
                newAdmin.getRole() == null || newAdmin.getRole().isEmpty()) {
                ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, "管理员信息不完整，ID、姓名、密码和角色不能为空");
                return;
            }
            if (administratorDao.getAdministratorById(newAdmin.getAdminId()) != null) {
                ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_CONFLICT, "管理员ID已存在");
                return;
            }
            
            boolean success = administratorDao.addAdministrator(newAdmin);
            
            if (success) {
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
     * 处理更新管理员信息（仅超级管理员或本人可操作）
     */
    private void handleUpdateAdmin(HttpServletRequest req, HttpServletResponse resp, String adminIdToUpdate) throws IOException {
        HttpSession session = req.getSession(false);
        Administrator currentAdmin = (Administrator) session.getAttribute("admin");

        if (!currentAdmin.isSuperAdmin() && !currentAdmin.getAdminId().equals(adminIdToUpdate)) {
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_FORBIDDEN, "权限不足，仅超级管理员或本人可执行此操作");
            return;
        }
        
        try {
            JsonObject requestBody = GSON.fromJson(req.getReader(), JsonObject.class);
            Administrator adminToUpdate = administratorDao.getAdministratorById(adminIdToUpdate);
            
            if (adminToUpdate == null) {
                ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_NOT_FOUND, "管理员不存在");
                return;
            }
            
            boolean changed = false;
            if (requestBody.has("name")) {
                adminToUpdate.setName(requestBody.get("name").getAsString());
                changed = true;
            }
            
            if (requestBody.has("role") && currentAdmin.isSuperAdmin()) {
                adminToUpdate.setRole(requestBody.get("role").getAsString());
                changed = true;
            }
            
            boolean passwordUpdateSuccess = true;
            if (requestBody.has("password")) {
                String newPassword = requestBody.get("password").getAsString();
                passwordUpdateSuccess = administratorDao.updatePassword(adminIdToUpdate, newPassword);
                 if (!passwordUpdateSuccess) {
                    ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "更新密码失败");
                    return;
                }
            }
            
            boolean infoUpdateSuccess = true;
            if(changed){
                 infoUpdateSuccess = administratorDao.updateAdministrator(adminToUpdate);
            }

            if (infoUpdateSuccess && passwordUpdateSuccess) {
                adminToUpdate.setPassword(null);
                ResponseUtil.sendSuccessResponse(resp, "管理员信息更新成功", adminToUpdate);
                if (adminIdToUpdate.equals(currentAdmin.getAdminId())) {
                    session.setAttribute("admin", administratorDao.getAdministratorById(adminIdToUpdate));
                }
            } else if (!infoUpdateSuccess){
                 ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "管理员基本信息更新失败");
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "更新管理员信息失败", e);
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "更新管理员信息失败：" + e.getMessage());
        }
    }

    /**
     * 处理删除管理员（仅超级管理员可操作）
     */
    private void handleDeleteAdmin(HttpServletRequest req, HttpServletResponse resp, String adminIdToDelete) throws IOException {
        HttpSession session = req.getSession(false);
        Administrator currentAdmin = (Administrator) session.getAttribute("admin");

        if (!currentAdmin.isSuperAdmin()) {
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_FORBIDDEN, "权限不足，仅超级管理员可执行此操作");
            return;
        }
        if (adminIdToDelete.equals(currentAdmin.getAdminId())) {
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, "不能删除当前登录的管理员账号");
            return;
        }
        
        Administrator adminToDelete = administratorDao.getAdministratorById(adminIdToDelete);
        if (adminToDelete == null) {
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_NOT_FOUND, "管理员不存在");
            return;
        }
        
        boolean success = administratorDao.deleteAdministrator(adminIdToDelete);
        if (success) {
            ResponseUtil.sendSuccessResponse(resp, "管理员删除成功", null);
        } else {
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "管理员删除失败");
        }
    }

    /**
     * 处理获取所有学生列表的请求
     * @param req HTTP请求
     * @param resp HTTP响应
     * @throws IOException IO异常
     */
    private void handleGetAllStudents(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            List<Student> students = studentDAO.findAll();
            
            // 过滤敏感信息，不返回密码
            for (Student student : students) {
                student.setPassword(null);
            }
            
            ResponseUtil.sendSuccessResponse(resp, "成功获取所有学生列表", students);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "获取所有学生列表失败", e);
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                    "获取所有学生列表失败: " + e.getMessage());
        }
    }
    
    /**
     * 处理更新学生状态的请求
     * @param req HTTP请求
     * @param resp HTTP响应
     * @param studentId 学生ID
     * @throws IOException IO异常
     */
    private void handleUpdateStudentStatus(HttpServletRequest req, HttpServletResponse resp, String studentId) throws IOException {
        try {
            // 解析请求体中的JSON数据
            JsonObject requestBody = GSON.fromJson(req.getReader(), JsonObject.class);
            boolean status = requestBody.get("status").getAsBoolean();
            
            // 先检查学生是否存在
            Student student = studentDAO.findById(studentId);
            if (student == null) {
                ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_NOT_FOUND, "学生不存在: " + studentId);
                return;
            }
            
            // 更新学生状态
            boolean updateSuccess = studentDAO.updateStatus(studentId, status);
            
            if (updateSuccess) {
                // 更新成功，返回成功响应
                Student updatedStudent = studentDAO.findById(studentId);
                // 确保返回的学生对象使用正确的状态值
                updatedStudent.setAccountStatus(status);
                updatedStudent.setPassword(null); // 不返回密码信息
                ResponseUtil.sendSuccessResponse(resp, "学生状态更新成功", updatedStudent);
            } else {
                // 更新失败，返回错误响应
                ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "学生状态更新失败");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "更新学生状态失败", e);
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                    "更新学生状态失败: " + e.getMessage());
        }
    }

    /**
     * 处理获取单个学生详情的请求
     */
    private void handleGetStudentById(HttpServletRequest req, HttpServletResponse resp, String studentId) throws IOException {
        try {
            if (studentId == null || studentId.trim().isEmpty()) {
                ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, "学生ID不能为空");
                return;
            }
            
            // 查询学生信息
            Student student = studentDAO.findById(studentId);
            if (student == null) {
                ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_NOT_FOUND, "未找到学生: " + studentId);
                return;
            }
            
            // 出于安全考虑，不返回密码
            student.setPassword(null);
            
            // 返回学生详情
            ResponseUtil.sendSuccessResponse(resp, "获取学生详情成功", student);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "获取学生详情失败", e);
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                    "获取学生详情失败: " + e.getMessage());
        }
    }

    /**
     * 处理学生搜索请求
     */
    private void handleSearchStudents(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        System.out.println("[AdminServlet] 进入handleSearchStudents方法");
        try {
            // 获取搜索关键词
            String keyword = req.getParameter("keyword");
            System.out.println("[AdminServlet] 搜索关键词: " + keyword);
            
            if (keyword == null || keyword.trim().isEmpty()) {
                ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, "搜索关键词不能为空");
                return;
            }
            
            // 调用DAO层执行搜索
            List<Student> students = studentDAO.searchStudents(keyword);
            System.out.println("[AdminServlet] 搜索结果数量: " + students.size());
            
            // 出于安全考虑，不返回密码
            for (Student student : students) {
                student.setPassword(null);
            }
            
            ResponseUtil.sendSuccessResponse(resp, "搜索学生成功", students);
        } catch (Exception e) {
            System.err.println("[AdminServlet] 搜索学生失败: " + e.getMessage());
            e.printStackTrace();
            LOGGER.log(Level.SEVERE, "搜索学生失败", e);
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                    "搜索学生失败: " + e.getMessage());
        }
    }
} 