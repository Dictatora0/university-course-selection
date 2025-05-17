package servlet;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import dao.FriendshipDAO;
import dao.StudentDAO;
import dao.TransferDAO;
import model.Student;
import util.ResponseUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 处理好友转账的Servlet
 */
@WebServlet("/api/transfer/*")
public class TransferServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(TransferServlet.class.getName());
    private final TransferDAO transferDAO = new TransferDAO();
    private final StudentDAO studentDAO = new StudentDAO();
    private final FriendshipDAO friendshipDAO = new FriendshipDAO();
    private final Gson gson = new Gson();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        if (pathInfo == null) {
            pathInfo = "/";
        }
        
        if (pathInfo.equals("/friend")) {
            handleFriendTransfer(req, resp);
        } else {
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_NOT_FOUND, "未找到请求的资源: " + pathInfo);
        }
    }
    
    /**
     * 处理好友转账请求
     */
    private void handleFriendTransfer(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("student") == null) {
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_UNAUTHORIZED, "未登录");
            return;
        }
        
        Student sessionStudent = (Student) session.getAttribute("student");
        String senderId = sessionStudent.getStudentId();
        
        try {
            // 解析请求体
            JsonObject requestData = ResponseUtil.readRequestJson(req);
            if (requestData == null) {
                ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, "无效的请求数据");
                return;
            }
            
            // 验证参数
            if (!requestData.has("receiverId") || requestData.get("receiverId").isJsonNull()) {
                ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, "接收者ID不能为空");
                return;
            }
            
            if (!requestData.has("amount") || requestData.get("amount").isJsonNull()) {
                ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, "转账金额不能为空");
                return;
            }
            
            String receiverId = requestData.get("receiverId").getAsString().trim();
            BigDecimal amount;
            
            try {
                amount = requestData.get("amount").getAsBigDecimal();
                if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                    ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, "转账金额必须大于0");
                    return;
                }
            } catch (NumberFormatException e) {
                ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, "转账金额格式不正确");
                return;
            }
            
            String description = requestData.has("description") ? requestData.get("description").getAsString().trim() : "好友转账";
            
            // 不能转账给自己
            if (senderId.equals(receiverId)) {
                ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, "不能转账给自己");
                return;
            }
            
            // 检查接收者是否存在
            Student receiver = studentDAO.findById(receiverId);
            if (receiver == null) {
                ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_NOT_FOUND, "接收者不存在");
                return;
            }
            
            // 检查是否是好友关系
            if (!friendshipDAO.isFriend(senderId, receiverId)) {
                ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_FORBIDDEN, "只能向好友转账");
                return;
            }
            
            // 检查余额是否足够
            if (sessionStudent.getBalance() < amount.doubleValue()) {
                ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_FORBIDDEN, "余额不足");
                return;
            }
            
            // 执行转账
            boolean success = transferDAO.transfer(senderId, receiverId, amount, description);
            
            if (success) {
                // 更新会话中的余额
                Student updatedStudent = studentDAO.findById(senderId);
                session.setAttribute("student", updatedStudent);
                
                LOGGER.log(Level.INFO, "转账成功: {0} -> {1}, 金额: {2}", new Object[]{senderId, receiverId, amount});
                
                // 构建响应
                JsonObject result = new JsonObject();
                result.addProperty("newBalance", updatedStudent.getBalance());
                
                ResponseUtil.sendSuccessResponse(resp, "转账成功", result);
            } else {
                LOGGER.log(Level.WARNING, "转账失败: {0} -> {1}, 金额: {2}", new Object[]{senderId, receiverId, amount});
                ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "转账失败，请稍后再试");
            }
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "处理转账请求时发生错误", e);
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "服务器内部错误");
        }
    }
} 