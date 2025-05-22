package servlet;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import dao.StudentDAO;
import dao.TransactionDAO;
import dao.TransactionControlDao;
import model.Student;
import model.Transaction;
import model.TransactionControl;
import util.ResponseUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 处理支付相关操作的Servlet
 */
@WebServlet("/api/payment/*")
public class PaymentServlet extends HttpServlet {
    private static final Logger logger = Logger.getLogger(PaymentServlet.class.getName());
    private StudentDAO studentDAO = new StudentDAO();
    private TransactionDAO transactionDAO = new TransactionDAO();
    private TransactionControlDao tcDao = new TransactionControlDao();
    private Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String pathInfo = request.getPathInfo();
        if (pathInfo == null) {
            pathInfo = "/";
        }
        
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("student") == null) {
            ResponseUtil.sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "未登录或会话已过期");
            return;
        }

        Student sessionStudent = (Student) session.getAttribute("student");
        String studentId = sessionStudent.getStudentId();
        
        if (pathInfo.equals("/transactions")) {
            handleGetTransactions(studentId, response);
        } else if (pathInfo.equals("/balance")) {
            handleGetBalance(studentId, response);
        } else {
            ResponseUtil.sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, "不支持的操作: " + pathInfo);
        }
    }

    /**
     * 处理获取交易记录请求
     */
    private void handleGetTransactions(String studentId, HttpServletResponse response) throws IOException {
        try {
            List<Transaction> transactions = transactionDAO.findByStudentId(studentId);
            ResponseUtil.sendSuccessResponse(response, "获取交易记录成功", transactions);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "获取交易记录失败", e);
            ResponseUtil.sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "获取交易记录失败: " + e.getMessage());
        }
    }

    /**
     * 处理获取余额请求
     */
    private void handleGetBalance(String studentId, HttpServletResponse response) throws IOException {
        try {
            Student student = studentDAO.findById(studentId);
            if (student == null) {
                ResponseUtil.sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, "未找到学生信息");
                return;
            }
            
            JsonObject result = new JsonObject();
            result.addProperty("balance", student.getBalance());
            
            ResponseUtil.sendSuccessResponse(response, "获取余额成功", result);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "获取余额失败", e);
            ResponseUtil.sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "获取余额失败: " + e.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String pathInfo = request.getPathInfo();
        if (pathInfo == null) {
            pathInfo = "/";
        }
        
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("student") == null) {
            ResponseUtil.sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "未登录或会话已过期");
            return;
        }

        Student sessionStudent = (Student) session.getAttribute("student");
        String studentId = sessionStudent.getStudentId();
        
        try {
            // 从请求体中读取JSON数据
            BufferedReader reader = request.getReader();
            StringBuilder requestData = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                requestData.append(line);
            }
            
            // 解析JSON数据
            JsonObject jsonData = gson.fromJson(requestData.toString(), JsonObject.class);
            
            // 获取金额并验证
            if (jsonData == null || !jsonData.has("amount")) {
                ResponseUtil.sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "金额不能为空");
                return;
            }
            
            BigDecimal amount;
            try {
                amount = jsonData.get("amount").getAsBigDecimal();
                if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                    ResponseUtil.sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "金额必须大于0");
                    return;
                }
            } catch (NumberFormatException e) {
                ResponseUtil.sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "金额格式不正确");
                return;
            }

            // 根据路径处理不同操作
            switch (pathInfo) {
                case "/deposit":
                    handleDeposit(studentId, amount, response, request);
                    break;
                case "/withdraw":
                    handleWithdraw(studentId, amount, response, request);
                    break;
                case "/transfer":
                    // 获取转账目标学生ID和备注
                    if (!jsonData.has("toStudentId") || jsonData.get("toStudentId").isJsonNull()) {
                        ResponseUtil.sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "接收方学号不能为空");
                        return;
                    }
                    String toStudentId = jsonData.get("toStudentId").getAsString();
                    String description = jsonData.has("description") ? jsonData.get("description").getAsString() : "好友转账";
                    handleTransfer(studentId, toStudentId, amount, description, response, request);
                    break;
                case "/pay":
                    // 获取课程ID和备注
                    if (!jsonData.has("courseId") || jsonData.get("courseId").isJsonNull()) {
                        ResponseUtil.sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "课程ID不能为空");
                        return;
                    }
                    String courseId = jsonData.get("courseId").getAsString();
                    String paymentDesc = jsonData.has("description") ? jsonData.get("description").getAsString() : "课程付费";
                    handlePayment(studentId, courseId, amount, paymentDesc, response, request);
                    break;
                case "/balance":
                    handleGetBalance(studentId, response);
                    break;
                default:
                    ResponseUtil.sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, "不支持的操作: " + pathInfo);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "处理支付操作时发生错误", e);
            ResponseUtil.sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "服务器内部错误: " + e.getMessage());
        }
    }

    /**
     * 处理充值操作
     */
    private void handleDeposit(String studentId, BigDecimal amount, HttpServletResponse response, HttpServletRequest request) throws IOException, SQLException {
        // 获取学生信息
        Student student = studentDAO.findById(studentId);
        if (student == null) {
            ResponseUtil.sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, "未找到学生信息");
            return;
        }

        // 更新余额
        double newBalance = student.getBalance() + amount.doubleValue();
        boolean success = studentDAO.updateBalance(studentId, newBalance);
        if (success) {
            // 记录交易
            Transaction transaction = new Transaction();
            transaction.setStudentId(studentId);
            transaction.setAmount(amount);
            transaction.setType(Transaction.TransactionType.DEPOSIT);
            transaction.setDescription("账户充值");
            
            transactionDAO.add(transaction);
            
            // 更新session中的学生对象
            student.setBalance(newBalance);
            request.getSession().setAttribute("student", student);
            
            // 返回成功信息
            ResponseUtil.sendSuccessResponse(response, "充值成功，已添加 " + amount + " 元到账户");
        } else {
            ResponseUtil.sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "充值失败");
        }
    }

    /**
     * 处理提现操作
     */
    private void handleWithdraw(String studentId, BigDecimal amount, HttpServletResponse response, HttpServletRequest request) throws IOException, SQLException {
        // 获取学生信息
        Student student = studentDAO.findById(studentId);
        if (student == null) {
            ResponseUtil.sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, "未找到学生信息");
            return;
        }

        // 检查余额是否足够
        if (student.getBalance() < amount.doubleValue()) {
            ResponseUtil.sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "余额不足，当前余额: " + student.getBalance());
            return;
        }

        // 更新余额（提现为负数，所以需要取负）
        double newBalance = student.getBalance() - amount.doubleValue();
        boolean success = studentDAO.updateBalance(studentId, newBalance);
        if (success) {
            // 记录交易
            Transaction transaction = new Transaction();
            transaction.setStudentId(studentId);
            transaction.setAmount(amount.negate());
            transaction.setType(Transaction.TransactionType.WITHDRAW);
            transaction.setDescription("账户提现");
            
            transactionDAO.add(transaction);
            
            // 更新session中的学生对象
            student.setBalance(newBalance);
            request.getSession().setAttribute("student", student);
            
            // 返回成功信息
            ResponseUtil.sendSuccessResponse(response, "提现成功，已从账户扣除 " + amount + " 元");
        } else {
            ResponseUtil.sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "提现失败");
        }
    }

    /**
     * 处理转账操作
     */
    private void handleTransfer(String fromStudentId, String toStudentId, BigDecimal amount, String description, 
                               HttpServletResponse response, HttpServletRequest request) throws IOException, SQLException {
        // 检查发起方和接收方ID是否相同
        if (fromStudentId.equals(toStudentId)) {
            ResponseUtil.sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "不能向自己转账");
            return;
        }
        
        // 检查转账金额是否合理
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            ResponseUtil.sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "转账金额必须大于0");
            return;
        }
        
        // 获取转出方和接收方学生信息
        Student fromStudent = studentDAO.findById(fromStudentId);
        Student toStudent = studentDAO.findById(toStudentId);
        
        if (fromStudent == null) {
            ResponseUtil.sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, "转出方学生不存在");
            return;
        }
        
        if (toStudent == null) {
            ResponseUtil.sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, "接收方学生不存在");
            return;
        }
        
        // 检查余额是否足够
        if (fromStudent.getBalance() < amount.doubleValue()) {
            ResponseUtil.sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "余额不足，当前余额: " + fromStudent.getBalance());
            return;
        }
        
        // 转账预警检查 - 检查单笔限额
        // 获取当前交易控制设置
        try {
            TransactionControl control = tcDao.getCurrentControl();
            
            if (control != null && control.isEnabled()) {
                // 检查单笔限额
                if (control.isExceedSingleLimit(amount)) {
                    ResponseUtil.sendErrorResponse(response, HttpServletResponse.SC_FORBIDDEN, 
                            "转账金额超过单笔限额 " + control.getMaxSingleAmount() + "元");
                    return;
                }
                
                // 检查日累计限额
                java.util.Date today = new java.util.Date();
                TransactionDAO txDao = new TransactionDAO();
                BigDecimal dailySum = txDao.getDailyTransferSum(fromStudentId, today);
                
                if (dailySum != null && control.isExceedDailyLimit(dailySum, amount)) {
                    ResponseUtil.sendErrorResponse(response, HttpServletResponse.SC_FORBIDDEN, 
                            "转账金额超过日累计限额，当前已累计: " + dailySum + "元，限额: " + control.getDailyLimit() + "元");
                    return;
                }
                
                // 检查日交易次数
                int dailyCount = txDao.getDailyTransferCount(fromStudentId, today);
                if (control.isExceedDailyCount(dailyCount)) {
                    ResponseUtil.sendErrorResponse(response, HttpServletResponse.SC_FORBIDDEN, 
                            "超过今日最大交易次数限制 " + control.getMaxDailyTransactions() + "次");
                    return;
                }
                
                // 检查频繁转账 - 在指定时间窗口内进行多次转账
                int recentTransfers = txDao.getTransferCountInTimeWindow(fromStudentId, control.getFrequentTransferTimeWindow());
                if (control.isFrequentTransfer(recentTransfers)) {
                    // 这里我们不阻止转账，而是返回一个包含警告信息的成功响应
                    JsonObject jsonResponse = new JsonObject();
                    jsonResponse.addProperty("success", true);
                    jsonResponse.addProperty("warning", true);
                    jsonResponse.addProperty("message", "检测到您在短时间内进行了多次转账，请确认这些操作是您本人进行的");
                    jsonResponse.addProperty("warningType", "FREQUENT_TRANSFER");
                    jsonResponse.addProperty("details", "在过去" + control.getFrequentTransferTimeWindow() + 
                            "分钟内已进行" + recentTransfers + "次转账操作");
                    
                    // 继续处理转账...但记录警告信息
                    logger.log(Level.WARNING, "检测到频繁转账行为: 学生ID=" + fromStudentId + 
                            ", 时间窗口=" + control.getFrequentTransferTimeWindow() + "分钟, 转账次数=" + recentTransfers);
                    
                    // 设置标志以便在转账完成后返回带有警告的响应
                    request.setAttribute("TRANSFER_WARNING_RESPONSE", jsonResponse.toString());
                }
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "转账限额检查失败", e);
            // 继续处理，不因检查失败而阻止转账
        }

        // 更新转出方余额（扣减）
        double fromNewBalance = fromStudent.getBalance() - amount.doubleValue();
        boolean fromSuccess = studentDAO.updateBalance(fromStudentId, fromNewBalance);
        
        if (!fromSuccess) {
            ResponseUtil.sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "转出方扣款失败");
            return;
        }
        
        // 更新接收方余额（增加）
        double toNewBalance = toStudent.getBalance() + amount.doubleValue();
        boolean toSuccess = studentDAO.updateBalance(toStudentId, toNewBalance);
        
        if (!toSuccess) {
            // 如果接收方加款失败，回滚转出方的扣款
            studentDAO.updateBalance(fromStudentId, fromStudent.getBalance());
            ResponseUtil.sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "接收方加款失败，已回滚转出方扣款");
            return;
        }
        
        // 记录转出交易
        Transaction outTransaction = new Transaction();
        outTransaction.setStudentId(fromStudentId);
        outTransaction.setAmount(amount.negate()); // 负数表示转出
        outTransaction.setType(Transaction.TransactionType.TRANSFER);
        outTransaction.setDescription("转账给 " + toStudent.getName() + "(" + toStudentId + ")" + (description != null ? ": " + description : ""));
        
        transactionDAO.add(outTransaction);
        
        // 记录转入交易
        Transaction inTransaction = new Transaction();
        inTransaction.setStudentId(toStudentId);
        inTransaction.setAmount(amount); // 正数表示转入
        inTransaction.setType(Transaction.TransactionType.TRANSFER);
        inTransaction.setDescription("收到来自 " + fromStudent.getName() + "(" + fromStudentId + ") 的转账" + (description != null ? ": " + description : ""));
        
        transactionDAO.add(inTransaction);
        
        // 更新session中的学生对象
        fromStudent.setBalance(fromNewBalance);
        request.getSession().setAttribute("student", fromStudent);
        
        // 检查是否有频繁转账警告需要返回
        String warningResponse = (String) request.getAttribute("TRANSFER_WARNING_RESPONSE");
        if (warningResponse != null) {
            // 返回带有警告的成功信息
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(warningResponse);
        } else {
            // 返回普通成功信息
            ResponseUtil.sendSuccessResponse(response, "转账成功，已向 " + toStudent.getName() + " 转账 " + amount + " 元");
        }
    }

    /**
     * 处理课程支付操作
     */
    private void handlePayment(String studentId, String courseId, BigDecimal amount, String description, 
                             HttpServletResponse response, HttpServletRequest request) throws IOException, SQLException {
        // 获取学生信息
        Student student = studentDAO.findById(studentId);
        if (student == null) {
            ResponseUtil.sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, "未找到学生信息");
            return;
        }

        // 检查余额是否足够
        if (student.getBalance() < amount.doubleValue()) {
            ResponseUtil.sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "余额不足，当前余额: " + student.getBalance());
            return;
        }
        
        // 更新余额
        double newBalance = student.getBalance() - amount.doubleValue();
        boolean success = studentDAO.updateBalance(studentId, newBalance);
        
        if (success) {
            // 记录交易
            Transaction transaction = new Transaction();
            transaction.setStudentId(studentId);
            transaction.setAmount(amount.negate()); // 负数表示支出
            transaction.setType(Transaction.TransactionType.EXPENSE);
            transaction.setDescription("支付课程 " + courseId + ": " + description);
            
            transactionDAO.add(transaction);
            
            // 更新session中的学生对象
            student.setBalance(newBalance);
            request.getSession().setAttribute("student", student);
            
            // 返回成功信息
            ResponseUtil.sendSuccessResponse(response, "支付成功，已支付 " + amount + " 元");
        } else {
            ResponseUtil.sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "支付失败");
        }
    }
} 