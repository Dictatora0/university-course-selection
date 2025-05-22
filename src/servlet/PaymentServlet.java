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
import java.sql.Connection;
import java.sql.PreparedStatement;
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

        // 计算新余额 - 使用BigDecimal进行计算避免精度问题
        BigDecimal currentBalance = new BigDecimal(student.getBalance());
        BigDecimal newBalance = currentBalance.add(amount);
        
        // 使用事务管理更新余额和添加交易记录
        Connection conn = null;
        boolean success = false;
        
        try {
            conn = util.DBConnection.getConnection();
            conn.setAutoCommit(false);
            
            logTransactionDetails("充值操作", 
                "学生: " + student.getName() + "(" + studentId + ")", 
                "原余额: " + student.getBalance(),
                "充值金额: " + amount,
                "新余额: " + newBalance);
            
            // 使用StudentDAO更新余额
            if (!studentDAO.updateBalance(studentId, newBalance)) {
                throw new SQLException("更新余额失败");
            }
            
            logTransactionDetails("余额更新", "充值余额更新成功", "新余额: " + newBalance);
            
            // 记录充值交易
            Transaction transaction = new Transaction();
            transaction.setStudentId(studentId);
            transaction.setAmount(amount);  // 充值金额为正数
            transaction.setType(Transaction.TransactionType.DEPOSIT);
            transaction.setDescription("账户充值");
            transaction.setStatus(true); // 设置交易状态为成功
            
            if (!transactionDAO.add(transaction)) {
                throw new SQLException("创建充值交易记录失败");
            }
            
            logTransactionDetails("交易记录", "充值交易记录创建成功", "ID: " + transaction.getTransactionId());
            
            // 提交事务
            conn.commit();
            success = true;
            
            // 更新session中的学生对象
            student.setBalance(newBalance.doubleValue());
            request.getSession().setAttribute("student", student);
            
            // 返回成功信息
            ResponseUtil.sendSuccessResponse(response, "充值成功，已向账户充值 " + amount + " 元");
            
        } catch (SQLException e) {
            // 发生异常时回滚事务
            if (conn != null) {
                try {
                    conn.rollback();
                    logTransactionDetails("充值错误", "发生异常，事务已回滚", "错误: " + e.getMessage());
                } catch (SQLException ex) {
                    logger.log(Level.SEVERE, "回滚事务失败", ex);
                }
            }
            
            // 记录错误并向客户端返回错误信息
            logger.log(Level.SEVERE, "充值过程中发生错误", e);
            ResponseUtil.sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                    "充值失败: " + e.getMessage());
        } finally {
            // 无论成功或失败，最后都要关闭连接并恢复自动提交
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ex) {
                    logger.log(Level.SEVERE, "关闭数据库连接失败", ex);
                }
            }
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

        // 检查余额是否足够 - 使用BigDecimal进行比较
        BigDecimal currentBalance = new BigDecimal(student.getBalance());
        if (currentBalance.compareTo(amount) < 0) {
            ResponseUtil.sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "余额不足，当前余额: " + student.getBalance());
            return;
        }

        // 计算新余额 - 使用BigDecimal进行计算
        BigDecimal newBalance = currentBalance.subtract(amount);
        
        // 使用事务管理更新余额和添加交易记录
        Connection conn = null;
        boolean success = false;
        
        try {
            conn = util.DBConnection.getConnection();
            conn.setAutoCommit(false);
            
            logTransactionDetails("提现操作", 
                "学生: " + student.getName() + "(" + studentId + ")", 
                "原余额: " + student.getBalance(),
                "提现金额: " + amount,
                "新余额: " + newBalance);
            
            // 使用StudentDAO更新余额
            if (!studentDAO.updateBalance(studentId, newBalance)) {
                throw new SQLException("更新余额失败");
            }
            
            logTransactionDetails("余额更新", "提现余额更新成功", "新余额: " + newBalance);
            
            // 记录提现交易（金额为负数表示支出）
            Transaction transaction = new Transaction();
            transaction.setStudentId(studentId);
            transaction.setAmount(amount.negate()); // 使用负数表示支出
            transaction.setType(Transaction.TransactionType.WITHDRAW);
            transaction.setDescription("账户提现");
            transaction.setStatus(true); // 设置交易状态为成功
            
            if (!transactionDAO.add(transaction)) {
                throw new SQLException("创建提现交易记录失败");
            }
            
            logTransactionDetails("交易记录", "提现交易记录创建成功", "ID: " + transaction.getTransactionId());
            
            // 提交事务
            conn.commit();
            success = true;
            
            // 更新session中的学生对象
            student.setBalance(newBalance.doubleValue());
            request.getSession().setAttribute("student", student);
            
            // 返回成功信息
            ResponseUtil.sendSuccessResponse(response, "提现成功，已从账户提取 " + amount + " 元");
            
        } catch (SQLException e) {
            // 发生异常时回滚事务
            if (conn != null) {
                try {
                    conn.rollback();
                    logTransactionDetails("提现错误", "发生异常，事务已回滚", "错误: " + e.getMessage());
                } catch (SQLException ex) {
                    logger.log(Level.SEVERE, "回滚事务失败", ex);
                }
            }
            
            // 记录错误并向客户端返回错误信息
            logger.log(Level.SEVERE, "提现过程中发生错误", e);
            ResponseUtil.sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                    "提现失败: " + e.getMessage());
        } finally {
            // 无论成功或失败，最后都要关闭连接并恢复自动提交
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ex) {
                    logger.log(Level.SEVERE, "关闭数据库连接失败", ex);
                }
            }
        }
    }

    /**
     * 处理转账操作
     */
    private void handleTransfer(String fromStudentId, String toStudentId, BigDecimal amount, String description, 
                               HttpServletResponse response, HttpServletRequest request) throws IOException, SQLException {
        // 添加详细日志
        logger.log(Level.INFO, "开始处理转账请求: 从 " + fromStudentId + " 到 " + toStudentId + 
                  ", 金额: " + amount + ", 描述: " + description);
        
        // 检查发起方和接收方ID是否相同
        if (fromStudentId.equals(toStudentId)) {
            ResponseUtil.sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "不能向自己转账");
            logger.log(Level.WARNING, "转账失败: 不能向自己转账");
            return;
        }
        
        // 检查转账金额是否合理
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            ResponseUtil.sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "转账金额必须大于0");
            logger.log(Level.WARNING, "转账失败: 金额必须大于0, 当前金额: " + amount);
            return;
        }
        
        // 获取转出方和接收方学生信息
        Student fromStudent = studentDAO.findById(fromStudentId);
        Student toStudent = studentDAO.findById(toStudentId);
        
        logger.log(Level.INFO, "转账用户信息: 转出方: " + 
                  (fromStudent != null ? fromStudent.getName() + ", 余额: " + fromStudent.getBalance() : "未找到") + 
                  ", 接收方: " + (toStudent != null ? toStudent.getName() : "未找到"));
        
        if (fromStudent == null) {
            ResponseUtil.sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, "转出方学生不存在");
            logger.log(Level.WARNING, "转账失败: 转出方学生不存在, ID: " + fromStudentId);
            return;
        }
        
        if (toStudent == null) {
            ResponseUtil.sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, "接收方学生不存在");
            logger.log(Level.WARNING, "转账失败: 接收方学生不存在, ID: " + toStudentId);
            return;
        }
        
        // 检查余额是否足够
        if (fromStudent.getBalance() < amount.doubleValue()) {
            ResponseUtil.sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "余额不足，当前余额: " + fromStudent.getBalance());
            logger.log(Level.WARNING, "转账失败: 余额不足, 当前余额: " + fromStudent.getBalance() + ", 转账金额: " + amount);
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
        double toNewBalance = toStudent.getBalance() + amount.doubleValue();
        
        // 获取数据库连接并开始事务
        Connection conn = null;
        boolean success = false;
        
        try {
            // 获取连接并设置事务隔离级别
            conn = util.DBConnection.getConnection();
            conn.setAutoCommit(false);
            conn.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            
            logTransactionDetails("转账事务", 
                "从" + fromStudent.getName() + "(" + fromStudentId + ")", 
                "到" + toStudent.getName() + "(" + toStudentId + ")", 
                "金额: " + amount);
            
            // 使用预编译语句更新转出方余额
            String updateFromSql = "UPDATE Student SET balance = ? WHERE student_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(updateFromSql)) {
                pstmt.setDouble(1, fromNewBalance);
                pstmt.setString(2, fromStudentId);
                int updatedRows = pstmt.executeUpdate();
                
                if (updatedRows != 1) {
                    throw new SQLException("更新转出方余额失败，影响行数: " + updatedRows);
                }
                
                logTransactionDetails("余额更新", "转出方余额更新成功", "新余额: " + fromNewBalance);
            }
            
            // 使用预编译语句更新转入方余额
            String updateToSql = "UPDATE Student SET balance = ? WHERE student_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(updateToSql)) {
                pstmt.setDouble(1, toNewBalance);
                pstmt.setString(2, toStudentId);
                int updatedRows = pstmt.executeUpdate();
                
                if (updatedRows != 1) {
                    throw new SQLException("更新转入方余额失败，影响行数: " + updatedRows);
                }
                
                logTransactionDetails("余额更新", "转入方余额更新成功", "新余额: " + toNewBalance);
            }
            
            // 创建转出交易记录
            Transaction outTransaction = new Transaction();
            outTransaction.setStudentId(fromStudentId);
            outTransaction.setRelatedStudentId(toStudentId);
            outTransaction.setAmount(amount.negate()); // 负数表示转出
            outTransaction.setType(Transaction.TransactionType.TRANSFER);
            outTransaction.setDescription("转账给 " + toStudent.getName() + "(" + toStudentId + ")" + (description != null ? ": " + description : ""));
            
            if (!transactionDAO.add(outTransaction)) {
                throw new SQLException("创建转出交易记录失败");
            }
            
            logTransactionDetails("交易记录", "转出交易记录创建成功", "ID: " + outTransaction.getTransactionId());
            
            // 创建转入交易记录
            Transaction inTransaction = new Transaction();
            inTransaction.setStudentId(toStudentId);
            inTransaction.setRelatedStudentId(fromStudentId);
            inTransaction.setAmount(amount); // 正数表示转入
            inTransaction.setType(Transaction.TransactionType.TRANSFER);
            inTransaction.setDescription("收到来自 " + fromStudent.getName() + "(" + fromStudentId + ") 的转账" + (description != null ? ": " + description : ""));
            
            if (!transactionDAO.add(inTransaction)) {
                throw new SQLException("创建转入交易记录失败");
            }
            
            logTransactionDetails("交易记录", "转入交易记录创建成功", "ID: " + inTransaction.getTransactionId());
            
            // 提交事务
            conn.commit();
            success = true;
            
            logTransactionDetails("转账结果", "转账事务提交成功");
            
            // 更新session中的学生对象
            fromStudent.setBalance(fromNewBalance);
            request.getSession().setAttribute("student", fromStudent);
        } catch (SQLException e) {
            // 发生异常时回滚事务
            if (conn != null) {
                try {
                    conn.rollback();
                    logTransactionDetails("转账错误", "发生异常，事务已回滚", "错误: " + e.getMessage());
                } catch (SQLException ex) {
                    logger.log(Level.SEVERE, "回滚事务失败", ex);
                }
            }
            
            // 记录错误并向客户端返回错误信息
            logger.log(Level.SEVERE, "转账过程中发生错误", e);
            ResponseUtil.sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                    "转账失败: " + e.getMessage());
            return;
        } finally {
            // 无论成功或失败，最后都要关闭连接并恢复自动提交
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ex) {
                    logger.log(Level.SEVERE, "关闭数据库连接失败", ex);
                }
            }
        }
        
        // 如果成功执行到这里，说明转账成功
        if (success) {
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
    
    /**
     * 记录详细的交易日志
     * @param operation 操作类型
     * @param details 详细信息
     */
    private void logTransactionDetails(String operation, String... details) {
        StringBuilder logMessage = new StringBuilder();
        logMessage.append("[").append(operation).append("] ");
        
        if (details != null && details.length > 0) {
            for (String detail : details) {
                logMessage.append(detail).append("; ");
            }
        }
        
        logger.log(Level.INFO, logMessage.toString());
    }
} 