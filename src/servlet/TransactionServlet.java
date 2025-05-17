package servlet;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import dao.StudentDAO;
import dao.TransactionDAO;
import model.Course;
import model.Student;
import model.Transaction;
import util.ResponseUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 处理交易相关请求的Servlet
 */
@WebServlet("/api/transactions/*")
public class TransactionServlet extends BaseServlet {
    private static final Logger LOGGER = Logger.getLogger(TransactionServlet.class.getName());
    private final TransactionDAO transactionDAO = new TransactionDAO();
    private final StudentDAO studentDao = new StudentDAO();
    private final Gson gson = new Gson();

    /**
     * 处理创建交易请求
     */
    private void handleCreateTransaction(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("student") == null) {
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_UNAUTHORIZED, "未登录");
            return;
        }
        Student sessionStudent = (Student) session.getAttribute("student");
        String studentId = sessionStudent.getStudentId();
        JsonObject requestData = ResponseUtil.readRequestJson(req);
        
        if (requestData == null) {
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, "无效的请求数据");
            return;
        }
        
        String typeStr = requestData.has("type") ? requestData.get("type").getAsString().trim() : null;
        String amountStr = requestData.has("amount") ? requestData.get("amount").getAsString().trim() : null;
        String description = requestData.has("description") ? requestData.get("description").getAsString() : "";

        if (typeStr == null || typeStr.isEmpty()) {
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, "交易类型不能为空");
            return;
        }
        if (amountStr == null || amountStr.isEmpty()) {
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, "交易金额不能为空");
            return;
        }

        Transaction.TransactionType type;
        try {
            type = Transaction.TransactionType.valueOf(typeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, "无效的交易类型");
            return;
        }
        
        BigDecimal amount;
        try {
            amount = new BigDecimal(amountStr);
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, "交易金额必须大于0");
                return;
            }
        } catch (NumberFormatException e) {
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, "交易金额格式不正确");
            return;
        }
        
        Student student = studentDao.findById(studentId);
        if (student == null) {
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_NOT_FOUND, "学生账户不存在");
                return;
        }

        // 检查余额是否充足（如果是支出或转账）
        if (type == Transaction.TransactionType.EXPENSE || type == Transaction.TransactionType.TRANSFER) {
            if (BigDecimal.valueOf(student.getBalance()).compareTo(amount) < 0) {
                ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_FORBIDDEN, "账户余额不足");
                return;
            }
        }
        
        Transaction transaction = new Transaction();
        transaction.setStudentId(studentId);
        transaction.setType(type);
        transaction.setAmount(amount);
        transaction.setDescription(description);
        // transaction.setTransactionDate(new java.util.Date()); // DAO会处理时间

        boolean success = transactionDAO.add(transaction);
        
        if (success) {
            // 更新余额
            BigDecimal newBalance = (type == Transaction.TransactionType.DEPOSIT || type == Transaction.TransactionType.REFUND) ? 
                                     BigDecimal.valueOf(student.getBalance()).add(amount) :
                                     BigDecimal.valueOf(student.getBalance()).subtract(amount);
            studentDao.updateBalance(studentId, newBalance.doubleValue()); // 假设 StudentDAO 有 updateBalance 方法

            ResponseUtil.sendSuccessResponse(resp, "交易创建成功", transaction); // 返回创建的transaction对象
        } else {
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "交易创建失败");
        }
    }

    private void handleGetStudentTransactions(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("student") == null) {
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, "未登录");
            return;
        }
        Student sessionStudent = (Student) session.getAttribute("student");
        String studentId = sessionStudent.getStudentId();
        List<Transaction> transactions = transactionDAO.findByStudentId(studentId);
        ResponseUtil.sendSuccessResponse(resp, "获取交易记录成功", transactions);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        if (pathInfo == null || pathInfo.equals("/")) {
            // 默认列出交易记录
            handleGetStudentTransactions(req, resp);
        } else if (pathInfo.equals("/list")) {
            handleGetStudentTransactions(req, resp);
        } else {
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_NOT_FOUND, "未找到请求的资源");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        // 所有POST请求都视为创建新交易
        if (pathInfo == null || pathInfo.equals("/") || pathInfo.equals("/create")) {
            handleCreateTransaction(req, resp);
        } else {
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_NOT_FOUND, "未找到请求的资源: POST " + pathInfo);
        }
    }
    
    // BaseServlet methods
    public void create(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        handleCreateTransaction(req, resp);
    }
    
    public void student(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        handleGetStudentTransactions(req, resp);
    }
    
    // 添加处理 list 请求的方法
    public void list(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        handleGetStudentTransactions(req, resp);
    }
} 