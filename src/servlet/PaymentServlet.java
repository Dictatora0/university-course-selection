package servlet;

import dao.StudentDAO;
import dao.TransactionDAO;
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
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 处理支付相关操作的Servlet
 */
@WebServlet("/api/payment")
public class PaymentServlet extends HttpServlet {
    private static final Logger logger = Logger.getLogger(PaymentServlet.class.getName());
    private StudentDAO studentDAO = new StudentDAO();
    private TransactionDAO transactionDAO = new TransactionDAO();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // 获取操作类型
        String action = request.getParameter("action");
        
        if (action == null) {
            ResponseUtil.sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "未指定操作类型");
            return;
        }

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("studentId") == null) {
            ResponseUtil.sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "未登录或会话已过期");
            return;
        }

        String studentId = (String) session.getAttribute("studentId");
        
        try {
            // 获取金额并验证
            String amountStr = request.getParameter("amount");
            if (amountStr == null || amountStr.isEmpty()) {
                ResponseUtil.sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "金额不能为空");
                return;
            }
            
            BigDecimal amount;
            try {
                amount = new BigDecimal(amountStr);
                if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                    ResponseUtil.sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "金额必须大于0");
                    return;
                }
            } catch (NumberFormatException e) {
                ResponseUtil.sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "金额格式不正确");
                return;
            }

            // 根据操作类型处理
            switch (action) {
                case "deposit":
                    handleDeposit(studentId, amount, response);
                    break;
                case "withdraw":
                    handleWithdraw(studentId, amount, response);
                    break;
                default:
                    ResponseUtil.sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "不支持的操作类型: " + action);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "处理支付操作时发生错误", e);
            ResponseUtil.sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "服务器内部错误");
        }
    }

    /**
     * 处理充值操作
     */
    private void handleDeposit(String studentId, BigDecimal amount, HttpServletResponse response) throws IOException, SQLException {
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
            
            // 返回成功信息
            ResponseUtil.sendSuccessResponse(response, "充值成功，已添加 " + amount + " 元到账户");
        } else {
            ResponseUtil.sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "充值失败");
        }
    }

    /**
     * 处理提现操作
     */
    private void handleWithdraw(String studentId, BigDecimal amount, HttpServletResponse response) throws IOException, SQLException {
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
            
            // 返回成功信息
            ResponseUtil.sendSuccessResponse(response, "提现成功，已从账户扣除 " + amount + " 元");
        } else {
            ResponseUtil.sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "提现失败");
        }
    }
} 