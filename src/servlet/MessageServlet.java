package servlet;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import dao.FriendshipDAO;
import dao.MessageDAO;
import dao.StudentDAO;
import model.Message;
import model.Student;
import model.Contact;
import util.ResponseUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 处理消息相关请求的Servlet
 */
@WebServlet("/api/message/*")
public class MessageServlet extends BaseServlet {
    private static final Logger LOGGER = Logger.getLogger(MessageServlet.class.getName());
    private final MessageDAO messageDao = new MessageDAO();
    private final StudentDAO studentDao = new StudentDAO();
    private final FriendshipDAO friendshipDao = new FriendshipDAO();
    private final Gson gson = new Gson();

    /**
     * 处理发送消息请求
     */
    private void handleSendMessage(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            // 验证用户是否已登录
            HttpSession session = req.getSession(false);
            if (session == null || session.getAttribute("student") == null) {
                LOGGER.warning("发送消息失败：用户未登录");
                ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, "未登录");
                return;
            }
            
            Student sessionStudent = (Student) session.getAttribute("student");
            String senderId = sessionStudent.getStudentId();
            LOGGER.info("尝试读取请求JSON数据");
            JsonObject requestData = ResponseUtil.readRequestJson(req);
            
            if (requestData == null) {
                LOGGER.warning("发送消息失败：请求数据为空");
                ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, "无效的请求数据");
                return;
            }
            
            // 验证接收者ID和消息内容
            String receiverId = null;
            String content = null;
            
            try {
                if (requestData.has("toId")) {
                    receiverId = requestData.get("toId").getAsString().trim();
                } else if (requestData.has("toStudentId")) {
                    // 向下兼容，保留对toStudentId的支持
                    receiverId = requestData.get("toStudentId").getAsString().trim();
                }
                if (requestData.has("content")) {
                    content = requestData.get("content").getAsString().trim();
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "解析JSON字段时出错: " + e.getMessage(), e);
            }
            
            if (receiverId == null || receiverId.isEmpty()) {
                LOGGER.warning("发送消息失败：接收者ID为空");
                ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, "接收者ID不能为空");
                return;
            }
            if (content == null || content.isEmpty()) {
                LOGGER.warning("发送消息失败：消息内容为空");
                ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, "消息内容不能为空");
                return;
            }
            
            // 记录发送的消息内容长度和内容
            LOGGER.info("准备发送消息，内容长度: " + content.length() + ", 内容前10个字符: " + 
                    (content.length() > 10 ? content.substring(0, 10) + "..." : content));
            
            // 不能给自己发消息
            if (senderId.equals(receiverId)) {
                LOGGER.warning("发送消息失败：用户尝试给自己发消息");
                ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, "不能给自己发送消息");
                return;
            }
            
            // 验证接收者是否存在
            Student receiver = studentDao.findById(receiverId);
            if (receiver == null) {
                LOGGER.warning("发送消息失败：接收者ID不存在：" + receiverId);
                ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_NOT_FOUND, "接收者不存在");
                return;
            }
            
            // 验证发送者和接收者是否是好友关系
            if (!friendshipDao.isFriend(senderId, receiverId)) {
                LOGGER.warning("发送消息失败：用户" + senderId + "与" + receiverId + "不是好友关系");
                ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_FORBIDDEN, "只能向好友发送消息");
                return;
            }
            
            // 发送消息
            Message message = new Message();
            message.setFromStudentId(senderId);
            message.setToStudentId(receiverId);
            
            // 确保消息内容不超过数据库字段长度限制（假设为4000字符）
            if (content.length() > 4000) {
                content = content.substring(0, 4000);
                LOGGER.warning("消息内容过长，已截断至4000字符");
            }
            
            message.setContent(content);
            message.setRead(false);
            
            LOGGER.info("开始添加消息到数据库");
            boolean success = messageDao.add(message);
            
            if (success) {
                LOGGER.log(Level.INFO, "消息发送成功: {0} -> {1}", new Object[]{senderId, receiverId});
                ResponseUtil.sendSuccessResponse(resp, "消息发送成功");
            } else {
                LOGGER.log(Level.WARNING, "消息发送失败: {0} -> {1}", new Object[]{senderId, receiverId});
                ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "消息发送失败，请稍后再试");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "处理发送消息请求时发生错误: " + e.getMessage(), e);
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "服务器内部错误: " + e.getMessage());
        }
    }
    
    /**
     * 处理获取与指定好友的对话请求
     */
    private void handleGetConversation(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // 验证用户是否已登录
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("student") == null) {
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, "未登录");
            return;
        }
        
        Student sessionStudent = (Student) session.getAttribute("student");
        String currentUserId = sessionStudent.getStudentId();
        String pathInfo = req.getPathInfo();
        
        // 路径格式应该是 /conversation/{friendId}
        String[] pathParts = pathInfo.split("/");
        if (pathParts.length < 3 || pathParts[2].trim().isEmpty()) {
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, "好友ID未提供或无效的请求路径");
            return;
        }
        
        String friendId = pathParts[2];
        
        // 验证好友存在
        Student friend = studentDao.findById(friendId);
        if (friend == null) {
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_NOT_FOUND, "好友不存在");
            return;
        }
        
        // 验证是否是好友关系
        if (!friendshipDao.isFriend(currentUserId, friendId)) {
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_FORBIDDEN, "只能查看与好友的对话");
            return;
        }
        
        // 获取对话消息
        List<Message> messages = messageDao.findConversation(currentUserId, friendId);
        
        // 为消息添加发送者和接收者姓名
        Student currentUser = studentDao.findById(currentUserId);
        enrichMessages(messages, currentUser, friend);
        
        // 将与该好友的所有消息标记为已读
        messageDao.markAllAsRead(friendId, currentUserId);
        
        ResponseUtil.sendSuccessResponse(resp, "获取对话成功", messages);
    }
    
    /**
     * 处理获取未读消息请求
     */
    private void handleGetUnreadMessages(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // 验证用户是否已登录
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("student") == null) {
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, "未登录");
            return;
        }
        
        Student sessionStudent = (Student) session.getAttribute("student");
        String studentId = sessionStudent.getStudentId();
        
        // 获取未读消息
        List<Message> messages = messageDao.findUnreadMessages(studentId);
        
        // 为消息添加发送者姓名
        for (Message message : messages) {
            if (message.getFromStudentName() == null && message.getFromStudentId() != null) {
                Student sender = studentDao.findById(message.getFromStudentId());
                if (sender != null) {
                    message.setFromStudentName(sender.getName());
                }
            }
        }
        
        ResponseUtil.sendSuccessResponse(resp, "获取未读消息成功", messages);
    }
    
    /**
     * 处理获取未读消息数量请求
     */
    private void handleGetUnreadMessageCount(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // 验证用户是否已登录
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("student") == null) {
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, "未登录");
            return;
        }
        
        Student sessionStudent = (Student) session.getAttribute("student");
        String studentId = sessionStudent.getStudentId();
        int count = messageDao.getUnreadMessageCount(studentId);
        
        JsonObject result = new JsonObject();
        result.addProperty("count", count);
        
        ResponseUtil.sendSuccessResponse(resp, "获取未读消息数量成功", result);
    }
    
    /**
     * 处理获取最近联系人请求
     */
    private void handleGetRecentContacts(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // 验证用户是否已登录
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("student") == null) {
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, "未登录");
            return;
        }
        
        Student sessionStudent = (Student) session.getAttribute("student");
        String studentId = sessionStudent.getStudentId();
        
        try {
            // 调用DAO获取最近联系人列表
            List<Contact> contacts = messageDao.getRecentContacts(studentId);
            ResponseUtil.sendSuccessResponse(resp, "获取最近联系人成功", contacts);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "获取最近联系人失败: " + e.getMessage(), e);
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "获取最近联系人失败: " + e.getMessage());
        }
    }
    
    /**
     * 处理标记消息为已读请求
     */
    private void handleMarkMessageAsRead(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // 验证用户是否已登录
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("student") == null) {
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, "未登录");
            return;
        }
        
        Student sessionStudent = (Student) session.getAttribute("student");
        String studentId = sessionStudent.getStudentId();
        String pathInfo = req.getPathInfo();
        
        // 路径格式应该是 /read/{messageId}
        String[] pathParts = pathInfo.split("/");
        if (pathParts.length < 3 || pathParts[2].trim().isEmpty()) {
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, "消息ID未提供或无效的请求路径");
            return;
        }
        
        try {
            Long messageId = Long.parseLong(pathParts[2]);
            boolean success = messageDao.markAsRead(messageId, studentId);
            
            if (success) {
                ResponseUtil.sendSuccessResponse(resp, "标记消息为已读成功");
            } else {
                ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_NOT_FOUND, "标记消息为已读失败，消息不存在或无权限");
            }
        } catch (NumberFormatException e) {
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, "无效的消息ID格式");
        }
    }
    
    /**
     * 处理删除消息请求
     */
    private void handleDeleteMessage(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // 验证用户是否已登录
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("student") == null) {
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, "未登录");
            return;
        }
        
        Student sessionStudent = (Student) session.getAttribute("student");
        String studentId = sessionStudent.getStudentId();
        String pathInfo = req.getPathInfo();
        
        // 路径格式应该是 /delete/{messageId}
        String[] pathParts = pathInfo.split("/");
        if (pathParts.length < 3 || pathParts[2].trim().isEmpty()) {
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, "消息ID未提供或无效的请求路径");
            return;
        }
        
        try {
            Long messageId = Long.parseLong(pathParts[2]);
            // 需要确保 MessageDAO 的 delete 方法会检查消息是否属于该用户，或者在这里检查
            Message message = messageDao.findById(messageId);
            if (message == null || (!message.getFromStudentId().equals(studentId) && !message.getToStudentId().equals(studentId))) {
                ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_FORBIDDEN, "无权删除此消息");
                return;
            }

            boolean success = messageDao.delete(messageId);
            
            if (success) {
                ResponseUtil.sendSuccessResponse(resp, "删除消息成功");
            } else {
                ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "删除消息失败");
            }
        } catch (NumberFormatException e) {
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, "无效的消息ID格式");
        }
    }
    
    /**
     * 为消息列表添加发送者和接收者的名字
     * @param messages 消息列表
     * @param student1 学生1
     * @param student2 学生2
     */
    private void enrichMessages(List<Message> messages, Student student1, Student student2) {
        for (Message message : messages) {
            if (message.getFromStudentId().equals(student1.getStudentId())) {
                message.setFromStudentName(student1.getName());
                message.setToStudentName(student2.getName());
            } else {
                message.setFromStudentName(student2.getName());
                message.setToStudentName(student1.getName());
            }
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        
        if (pathInfo == null || pathInfo.equals("/")) {
            handleGetUnreadMessages(req, resp);
            return;
        }
        
        if (pathInfo.startsWith("/conversation/")) {
            handleGetConversation(req, resp);
        } else if (pathInfo.equals("/unread")) {
            handleGetUnreadMessages(req, resp);
        } else if (pathInfo.equals("/unread_count")) {
            handleGetUnreadMessageCount(req, resp);
        } else if (pathInfo.equals("/recent_contacts")) {
            handleGetRecentContacts(req, resp);
        } else if (pathInfo.equals("/list")) {
            handleGetRecentContacts(req, resp);
        } else {
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_NOT_FOUND, "未找到请求的资源: GET " + pathInfo);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        
        if (pathInfo == null || pathInfo.trim().isEmpty()) {
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, "无效的请求路径");
            return;
        }
        
        if (pathInfo.equals("/send")) {
            handleSendMessage(req, resp);
        } else {
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_NOT_FOUND, "未找到请求的资源: POST " + pathInfo);
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        if (pathInfo == null || pathInfo.equals("/")) {
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, "无效的请求路径");
            return;
        }
        
        if (pathInfo.startsWith("/read/")) { // e.g. /read/{messageId}
            handleMarkMessageAsRead(req, resp);
        } else {
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_NOT_FOUND, "未找到请求的资源: PUT " + pathInfo);
        }
    }
    
    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        
        if (pathInfo == null || pathInfo.equals("/")) {
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, "无效的请求路径");
            return;
        }
        
        if (pathInfo.startsWith("/delete/")) { // e.g. /delete/{messageId}
            handleDeleteMessage(req, resp);
        } else {
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_NOT_FOUND, "未找到请求的资源: DELETE " + pathInfo);
        }
    }
    
    /**
     * 发送消息（供BaseServlet反射调用）
     */
    public void send(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        handleSendMessage(req, resp);
    }
    
    /**
     * 获取会话消息（供BaseServlet反射调用）
     */
    public void conversation(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        handleGetConversation(req, resp);
    }
    
    /**
     * 获取未读消息（供BaseServlet反射调用）
     */
    public void unread(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        handleGetUnreadMessages(req, resp);
    }
    
    /**
     * 获取未读消息数量（供BaseServlet反射调用）
     */
    public void unread_count(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        handleGetUnreadMessageCount(req, resp);
    }
    
    /**
     * 标记消息为已读（供BaseServlet反射调用）
     */
    public void read(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        handleMarkMessageAsRead(req, resp);
    }
    
    /**
     * 删除消息（供BaseServlet反射调用）
     */
    public void delete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        handleDeleteMessage(req, resp);
    }
} 