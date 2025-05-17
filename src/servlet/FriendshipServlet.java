package servlet;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import dao.FriendshipDAO;
import dao.StudentDAO;
import model.Friendship;
import model.Student;
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
 * 处理好友关系的Servlet
 */
@WebServlet("/api/friendship/*")
public class FriendshipServlet extends BaseServlet {
    private static final Logger LOGGER = Logger.getLogger(FriendshipServlet.class.getName());
    private final FriendshipDAO friendshipDao = new FriendshipDAO();
    private final StudentDAO studentDao = new StudentDAO();
    private final Gson gson = new Gson();

    /**
     * 处理获取好友列表请求
     */
    private void handleGetFriends(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        
        if (session == null || session.getAttribute("student") == null) {
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, "未登录");
            return;
        }
        
        Student sessionStudent = (Student) session.getAttribute("student");
        String studentId = sessionStudent.getStudentId();
        List<Student> friends = friendshipDao.getFriendsWithDetails(studentId);
        
        ResponseUtil.sendSuccessResponse(resp, "获取好友列表成功", friends);
    }
    
    /**
     * 处理添加好友请求
     */
    private void handleAddFriend(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        
        if (session == null || session.getAttribute("student") == null) {
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, "未登录");
            return;
        }
        
        Student sessionStudent = (Student) session.getAttribute("student");
        String studentId = sessionStudent.getStudentId();
        JsonObject requestData = ResponseUtil.readRequestJson(req);
        
        if (requestData == null || !requestData.has("friendId") || requestData.get("friendId").isJsonNull() || requestData.get("friendId").getAsString().trim().isEmpty()) {
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, "好友ID不能为空");
            return;
        }
        
        String friendId = requestData.get("friendId").getAsString().trim();
        
        if (studentId.equals(friendId)) {
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, "不能添加自己为好友");
            return;
        }
        
        if (studentDao.findById(friendId) == null) {
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_NOT_FOUND, "要添加的好友不存在");
            return;
        }
        
        if (friendshipDao.isFriend(studentId, friendId)) {
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_CONFLICT, "已经是好友关系");
            return;
        }
        
        boolean success = friendshipDao.sendFriendRequest(studentId, friendId);
        
        if (success) {
            LOGGER.log(Level.INFO, "发送好友请求成功: {0} -> {1}", new Object[]{studentId, friendId});
            ResponseUtil.sendSuccessResponse(resp, "好友请求已发送，等待对方确认");
        } else {
            LOGGER.log(Level.WARNING, "发送好友请求失败: {0} -> {1}", new Object[]{studentId, friendId});
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "发送好友请求失败，请稍后再试");
        }
    }
    
    /**
     * 处理搜索学生请求
     */
    private void handleSearchStudents(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        
        if (session == null || session.getAttribute("student") == null) {
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, "未登录");
            return;
        }
        
        String keyword = req.getParameter("keyword");
        if (keyword == null || keyword.trim().isEmpty()) {
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, "搜索关键词不能为空");
            return;
        }
        
        List<Student> students = friendshipDao.searchStudents(keyword.trim());
        ResponseUtil.sendSuccessResponse(resp, "搜索学生成功", students);
    }
    
    /**
     * 处理获取好友请求列表
     */
    private void handleGetFriendRequests(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        
        if (session == null || session.getAttribute("student") == null) {
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, "未登录");
            return;
        }
        
        Student sessionStudent = (Student) session.getAttribute("student");
        String studentId = sessionStudent.getStudentId();
        
        List<Friendship> requests = friendshipDao.getFriendRequests(studentId);
        ResponseUtil.sendSuccessResponse(resp, "获取好友请求列表成功", requests);
    }
    
    /**
     * 处理接受好友请求
     */
    private void handleAcceptFriendRequest(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        
        if (session == null || session.getAttribute("student") == null) {
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, "未登录");
            return;
        }
        
        Student sessionStudent = (Student) session.getAttribute("student");
        String studentId = sessionStudent.getStudentId();
        
        String pathInfo = req.getPathInfo();
        if (pathInfo == null || pathInfo.trim().isEmpty()) {
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, "无效的请求路径");
            return;
        }
        
        String[] pathParts = pathInfo.split("/");
        if (pathParts.length < 3 || pathParts[2].trim().isEmpty()) { // parts[0] is empty, parts[1] is "accept"
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, "请求者ID未提供或无效的请求路径");
            return;
        }
        
        String requesterId = pathParts[2];
        
        boolean success = friendshipDao.acceptFriendRequest(studentId, requesterId);
        
        if (success) {
            LOGGER.log(Level.INFO, "接受好友请求成功: {0} <- {1}", new Object[]{studentId, requesterId});
            ResponseUtil.sendSuccessResponse(resp, "已接受好友请求");
        } else {
            LOGGER.log(Level.WARNING, "接受好友请求失败: {0} <- {1}", new Object[]{studentId, requesterId});
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "接受好友请求失败，请稍后再试");
        }
    }
    
    /**
     * 处理拒绝好友请求
     */
    private void handleRejectFriendRequest(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        
        if (session == null || session.getAttribute("student") == null) {
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, "未登录");
            return;
        }
        
        Student sessionStudent = (Student) session.getAttribute("student");
        String studentId = sessionStudent.getStudentId();
        
        String pathInfo = req.getPathInfo();
        if (pathInfo == null || pathInfo.trim().isEmpty()) {
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, "无效的请求路径");
            return;
        }
        
        String[] pathParts = pathInfo.split("/");
        if (pathParts.length < 3 || pathParts[2].trim().isEmpty()) { // parts[0] is empty, parts[1] is "reject"
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, "请求者ID未提供或无效的请求路径");
            return;
        }
        
        String requesterId = pathParts[2];
        
        boolean success = friendshipDao.rejectFriendRequest(studentId, requesterId);
        
        if (success) {
            LOGGER.log(Level.INFO, "拒绝好友请求成功: {0} <- {1}", new Object[]{studentId, requesterId});
            ResponseUtil.sendSuccessResponse(resp, "已拒绝好友请求");
        } else {
            LOGGER.log(Level.WARNING, "拒绝好友请求失败: {0} <- {1}", new Object[]{studentId, requesterId});
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "拒绝好友请求失败，请稍后再试");
        }
    }
    
    /**
     * 处理删除好友请求
     */
    private void handleDeleteFriend(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        
        if (session == null || session.getAttribute("student") == null) {
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, "未登录");
            return;
        }
        
        Student sessionStudent = (Student) session.getAttribute("student");
        String studentId = sessionStudent.getStudentId();
        String pathInfo = req.getPathInfo();
        
        if (pathInfo == null || pathInfo.trim().isEmpty()) {
             ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, "无效的请求路径");
             return;
        }
        
        String[] pathParts = pathInfo.split("/");
        if (pathParts.length < 3 || pathParts[2].trim().isEmpty()) { // parts[0] is empty, parts[1] is "delete"
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, "好友ID未提供或无效的请求路径");
            return;
        }
        
        String friendId = pathParts[2];
        
        if (!friendshipDao.isFriend(studentId, friendId)) {
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_NOT_FOUND, "好友关系不存在");
            return;
        }
        
        boolean success = friendshipDao.delete(studentId, friendId);
        
        if (success) {
            LOGGER.log(Level.INFO, "删除好友成功: {0} -> {1}", new Object[]{studentId, friendId});
            ResponseUtil.sendSuccessResponse(resp, "删除好友成功");
        } else {
            LOGGER.log(Level.WARNING, "删除好友失败: {0} -> {1}", new Object[]{studentId, friendId});
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "删除好友失败，请稍后再试");
        }
    }
    
    /**
     * 处理检查好友关系请求
     */
    private void handleCheckFriendship(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        
        if (session == null || session.getAttribute("student") == null) {
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, "未登录");
            return;
        }
        
        Student sessionStudent = (Student) session.getAttribute("student");
        String studentId = sessionStudent.getStudentId();
        String pathInfo = req.getPathInfo();

        if (pathInfo == null || pathInfo.trim().isEmpty()) {
             ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, "无效的请求路径");
             return;
        }
        
        String[] pathParts = pathInfo.split("/");
        if (pathParts.length < 3 || pathParts[2].trim().isEmpty()) { // parts[0] is empty, parts[1] is "check"
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, "好友ID未提供或无效的请求路径");
            return;
        }
        
        String friendId = pathParts[2];
        
        boolean isFriend = friendshipDao.isFriend(studentId, friendId);
        
        JsonObject result = new JsonObject();
        result.addProperty("isFriend", isFriend);
        
        ResponseUtil.sendSuccessResponse(resp, "检查好友关系成功", result);
    }
    
    /**
     * 处理获取好友数量请求
     */
    private void handleGetFriendCount(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        
        if (session == null || session.getAttribute("student") == null) {
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, "未登录");
            return;
        }
        
        Student sessionStudent = (Student) session.getAttribute("student");
        String studentId = sessionStudent.getStudentId();
        int count = friendshipDao.getFriendCount(studentId);
        
        JsonObject result = new JsonObject();
        result.addProperty("count", count);
        
        ResponseUtil.sendSuccessResponse(resp, "获取好友数量成功", result);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        
        if (pathInfo == null || pathInfo.equals("/")) {
            handleGetFriends(req, resp);
            return;
        }
        
        if (pathInfo.equals("/list")) {
            handleGetFriends(req, resp);
        } else if (pathInfo.startsWith("/check/")) {
            handleCheckFriendship(req, resp);
        } else if (pathInfo.equals("/count")) {
            handleGetFriendCount(req, resp);
        } else if (pathInfo.equals("/requests")) {
            handleGetFriendRequests(req, resp);
        } else if (pathInfo.startsWith("/search")) {
            handleSearchStudents(req, resp);
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
        
        if (pathInfo.equals("/add")) {
            handleAddFriend(req, resp);
        } else if (pathInfo.startsWith("/accept/")) {
            handleAcceptFriendRequest(req, resp);
        } else if (pathInfo.startsWith("/reject/")) {
            handleRejectFriendRequest(req, resp);
        } else {
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_NOT_FOUND, "未找到请求的资源: POST " + pathInfo);
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        
        if (pathInfo == null || pathInfo.equals("/")) {
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, "无效的请求路径");
            return;
        }
        
        if (pathInfo.startsWith("/delete/")) {
            handleDeleteFriend(req, resp);
        } else {
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_NOT_FOUND, "未找到请求的资源: DELETE " + pathInfo);
        }
    }
    
    /**
     * 获取好友列表（供BaseServlet反射调用）
     */
    public void list(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        handleGetFriends(req, resp);
    }
    
    /**
     * 添加好友（供BaseServlet反射调用）
     */
    public void add(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        handleAddFriend(req, resp);
    }
    
    /**
     * 删除好友（供BaseServlet反射调用）
     */
    public void delete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        handleDeleteFriend(req, resp);
    }
    
    /**
     * 检查好友关系（供BaseServlet反射调用）
     */
    public void check(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        handleCheckFriendship(req, resp);
    }
    
    /**
     * 获取好友数量（供BaseServlet反射调用）
     */
    public void count(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        handleGetFriendCount(req, resp);
    }
    
    /**
     * 获取好友请求（供BaseServlet反射调用）
     */
    public void requests(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        handleGetFriendRequests(req, resp);
    }
    
    /**
     * 搜索学生（供BaseServlet反射调用）
     */
    public void search(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        handleSearchStudents(req, resp);
    }
    
    /**
     * 接受好友请求（供BaseServlet反射调用）
     */
    public void accept(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        handleAcceptFriendRequest(req, resp);
    }
    
    /**
     * 拒绝好友请求（供BaseServlet反射调用）
     */
    public void reject(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        handleRejectFriendRequest(req, resp);
    }
} 