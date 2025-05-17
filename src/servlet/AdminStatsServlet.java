package servlet;

import com.google.gson.Gson;
import dao.CourseDAO;
import dao.StudentDAO;
import dao.TransactionDAO;
import dao.LoginLogDao;
import model.Administrator;
import model.Transaction;
import util.ResponseUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 管理员统计信息Servlet
 */
@WebServlet("/api/admin/stats/*")
public class AdminStatsServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(AdminStatsServlet.class.getName());
    private static final Gson GSON = new Gson();
    
    private final StudentDAO studentDAO = new StudentDAO();
    private final CourseDAO courseDAO = new CourseDAO();
    private final TransactionDAO transactionDAO = new TransactionDAO();
    private final LoginLogDao loginLogDao = new LoginLogDao();

    /**
     * 处理GET请求
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // 验证管理员登录状态
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("admin") == null) {
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_UNAUTHORIZED, "未登录或会话已过期");
            return;
        }
        
        Administrator admin = (Administrator) session.getAttribute("admin");
        String pathInfo = req.getPathInfo();
        
        try {
            if (pathInfo == null || pathInfo.equals("/")) {
                // 获取全部统计信息
                handleGetAllStats(req, resp, admin);
            } else if (pathInfo.equals("/students")) {
                // 获取学生总数
                handleGetStudentCount(req, resp, admin);
            } else if (pathInfo.equals("/courses")) {
                // 获取课程总数
                handleGetCourseCount(req, resp, admin);
            } else if (pathInfo.equals("/active-users")) {
                // 获取今日活跃用户数
                handleGetActiveUsers(req, resp, admin);
            } else if (pathInfo.equals("/transactions/recent")) {
                // 获取最近交易记录
                handleGetRecentTransactions(req, resp, admin);
            } else {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "获取统计信息失败", e);
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "获取统计信息失败: " + e.getMessage());
        }
    }

    /**
     * 获取全部统计信息
     */
    private void handleGetAllStats(HttpServletRequest req, HttpServletResponse resp, Administrator admin) throws IOException {
        try {
            // 构建统计信息对象
            Map<String, Object> stats = new HashMap<>();
            
            // 获取学生总数
            stats.put("studentCount", studentDAO.getTotalStudentCount());
            
            // 获取课程总数
            stats.put("courseCount", courseDAO.getTotalCourseCount());
            
            // 获取今日活跃用户数
            stats.put("activeUsers", loginLogDao.getTodayActiveStudentsCount());
            
            // 获取最近交易记录
            stats.put("recentTransactions", transactionDAO.getRecentTransactions(10));
            
            ResponseUtil.sendSuccessResponse(resp, "成功获取统计信息", stats);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "获取全部统计信息失败", e);
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "获取统计信息失败: " + e.getMessage());
        }
    }

    /**
     * 获取学生总数
     */
    private void handleGetStudentCount(HttpServletRequest req, HttpServletResponse resp, Administrator admin) throws IOException {
        try {
            int count = studentDAO.getTotalStudentCount();
            ResponseUtil.sendSuccessResponse(resp, "成功获取学生总数", count);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "获取学生总数失败", e);
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "获取学生总数失败: " + e.getMessage());
        }
    }

    /**
     * 获取课程总数
     */
    private void handleGetCourseCount(HttpServletRequest req, HttpServletResponse resp, Administrator admin) throws IOException {
        try {
            int count = courseDAO.getTotalCourseCount();
            ResponseUtil.sendSuccessResponse(resp, "成功获取课程总数", count);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "获取课程总数失败", e);
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "获取课程总数失败: " + e.getMessage());
        }
    }

    /**
     * 获取今日活跃用户数
     */
    private void handleGetActiveUsers(HttpServletRequest req, HttpServletResponse resp, Administrator admin) throws IOException {
        try {
            int count = loginLogDao.getTodayActiveStudentsCount();
            ResponseUtil.sendSuccessResponse(resp, "成功获取今日活跃用户数", count);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "获取今日活跃用户数失败", e);
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "获取今日活跃用户数失败: " + e.getMessage());
        }
    }

    /**
     * 获取最近交易记录
     */
    private void handleGetRecentTransactions(HttpServletRequest req, HttpServletResponse resp, Administrator admin) throws IOException {
        try {
            // 默认获取最近10条交易记录
            int limit = 10;
            String limitParam = req.getParameter("limit");
            if (limitParam != null && !limitParam.isEmpty()) {
                try {
                    limit = Integer.parseInt(limitParam);
                    // 限制最大查询数量
                    if (limit > 100) limit = 100;
                } catch (NumberFormatException e) {
                    // 忽略解析错误，使用默认值
                }
            }
            
            List<Transaction> transactions = transactionDAO.getRecentTransactions(limit);
            
            ResponseUtil.sendSuccessResponse(resp, "成功获取最近交易记录", transactions);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "获取最近交易记录失败", e);
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "获取最近交易记录失败: " + e.getMessage());
        }
    }
} 