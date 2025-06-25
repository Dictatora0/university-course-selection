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
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 处理好友关系的Servlet
 */
@WebServlet("/api/friendship/*")
public class FriendshipServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(FriendshipServlet.class.getName());
    private final FriendshipDAO friendshipDao = new FriendshipDAO();
    private final StudentDAO studentDao = new StudentDAO();
    private final Gson gson = new Gson();

    /**
     * 设置CORS头信息
     */
    private void setCorsHeaders(HttpServletRequest req, HttpServletResponse resp) {
        String origin = req.getHeader("Origin");
        if (origin != null) {
            resp.setHeader("Access-Control-Allow-Origin", origin);
        } else {
            resp.setHeader("Access-Control-Allow-Origin", "*");
        }
        resp.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type, Accept, X-Requested-With");
        resp.setHeader("Access-Control-Allow-Credentials", "true");
        resp.setHeader("Access-Control-Max-Age", "3600");
    }
    
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // 设置CORS头部
        setCorsHeaders(req, resp);
        
        // OPTIONS请求直接返回
        if (req.getMethod().equals("OPTIONS")) {
            resp.setStatus(HttpServletResponse.SC_OK);
            return;
        }
        
        // 调用父类的service方法处理请求
        super.service(req, resp);
    }

    /**
     * 处理获取好友列表请求
     */
    private void handleGetFriends(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        
        if (session == null || session.getAttribute("student") == null) {
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, "未登录");
            System.out.println("[FriendshipServlet] 获取好友列表 - 用户未登录");
            return;
        }
        
        Student sessionStudent = (Student) session.getAttribute("student");
        String studentId = sessionStudent.getStudentId();
        System.out.println("[FriendshipServlet] 获取好友列表 - 学生ID: " + studentId);
        
        try {
            // 获取已接受的好友
            List<Student> acceptedFriends = friendshipDao.getFriendsWithDetails(studentId);
            System.out.println("[FriendshipServlet] 获取好友列表 - 已接受的好友数量: " + acceptedFriends.size());
            
            // 对已接受的好友标记状态
            for (Student friend : acceptedFriends) {
                friend.setStatus("ACCEPTED");
            }
            
            // 获取已发送但未被接受的好友请求
            List<Student> pendingRequests = friendshipDao.findPendingRequestsByStudentId(studentId);
            System.out.println("[FriendshipServlet] 获取好友列表 - 未被接受的请求数量: " + pendingRequests.size());
            
            // 获取收到的好友请求
            List<Student> receivedRequests = friendshipDao.findReceivedRequestsByStudentId(studentId);
            System.out.println("[FriendshipServlet] 获取好友列表 - 收到的请求数量: " + receivedRequests.size());
            
            // 获取被拒绝的请求
            List<Student> rejectedRequests = friendshipDao.findRejectedRequestsByStudentId(studentId);
            System.out.println("[FriendshipServlet] 获取好友列表 - 被拒绝的请求数量: " + rejectedRequests.size());
            
            // 合并列表
            List<Student> allFriends = new ArrayList<>(acceptedFriends);
            allFriends.addAll(pendingRequests);
            allFriends.addAll(receivedRequests);
            allFriends.addAll(rejectedRequests);
            
            System.out.println("[FriendshipServlet] 获取好友列表 - 总好友数量(所有状态): " + allFriends.size());
            
            if (allFriends.size() > 0) {
                System.out.println("[FriendshipServlet] 第一个好友/请求信息: " + 
                                "ID=" + allFriends.get(0).getStudentId() + 
                                ", 姓名=" + allFriends.get(0).getName() + 
                                ", 院系ID=" + allFriends.get(0).getDeptId() + 
                                ", 院系名称=" + allFriends.get(0).getDeptName() +
                                ", 状态=" + allFriends.get(0).getStatus());
            }
            
            ResponseUtil.sendSuccessResponse(resp, "获取好友和请求列表成功", allFriends);
        } catch (Exception e) {
            System.out.println("[FriendshipServlet] 获取好友列表异常: " + e.getMessage());
            e.printStackTrace();
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "获取好友列表失败: " + e.getMessage());
        }
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
        String friendId = null;
        
        // 获取好友ID，可能是通过路径/delete/{friendId}或直接/{friendId}提供
        String pathInfo = req.getPathInfo();
        if (pathInfo != null) {
            if (pathInfo.startsWith("/delete/")) {
                friendId = pathInfo.substring("/delete/".length());
            } else if (pathInfo.startsWith("/")) {
                friendId = pathInfo.substring(1); // 去掉开头的 /
            }
        }
        
        // 如果通过req.getAttribute设置了friendId，优先使用它
        if (req.getAttribute("friendId") != null) {
            friendId = (String) req.getAttribute("friendId");
        }
        
        if (friendId == null || friendId.trim().isEmpty()) {
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, "好友ID未提供或无效");
            return;
        }
        
        // 检查是否为好友关系或待处理的请求
        boolean isFriend = friendshipDao.isFriend(studentId, friendId);
        boolean isPendingRequest = friendshipDao.isPendingRequest(studentId, friendId);
        boolean isReceivedRequest = friendshipDao.isPendingRequest(friendId, studentId);
        
        if (!isFriend && !isPendingRequest && !isReceivedRequest) {
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_NOT_FOUND, "好友关系或请求不存在");
            return;
        }
        
        boolean success = friendshipDao.delete(studentId, friendId);
        
        if (success) {
            if (isFriend) {
            LOGGER.log(Level.INFO, "删除好友成功: {0} -> {1}", new Object[]{studentId, friendId});
            ResponseUtil.sendSuccessResponse(resp, "删除好友成功");
            } else if (isPendingRequest) {
                LOGGER.log(Level.INFO, "取消好友请求成功: {0} -> {1}", new Object[]{studentId, friendId});
                ResponseUtil.sendSuccessResponse(resp, "取消好友请求成功");
            } else {
                LOGGER.log(Level.INFO, "拒绝好友请求成功: {0} <- {1}", new Object[]{studentId, friendId});
                ResponseUtil.sendSuccessResponse(resp, "拒绝好友请求成功");
            }
        } else {
            LOGGER.log(Level.WARNING, "删除好友/取消请求失败: {0} -> {1}", new Object[]{studentId, friendId});
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "操作失败，请稍后再试");
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
        
        if (pathInfo == null || "/".equals(pathInfo)) {
            handleGetFriends(req, resp);
        } else if (pathInfo.startsWith("/requests")) {
            handleGetFriendRequests(req, resp);
        } else if (pathInfo.startsWith("/check/")) {
            handleCheckFriendship(req, resp);
        } else if (pathInfo.startsWith("/count")) {
            handleGetFriendCount(req, resp);
        } else if (pathInfo.startsWith("/search")) {
            handleSearchStudents(req, resp);
        } else if (pathInfo.startsWith("/received")) {
            handleGetReceivedRequests(req, resp);
        } else if (pathInfo.equals("/sent")) {
            handleGetSentRequests(req, resp);
        } else if (pathInfo.startsWith("/recommendations")) {
            handleGetRecommendations(req, resp);
        } else if (pathInfo.equals("/list")) {
            handleGetFriends(req, resp);
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "未找到对应的API路径");
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
        } else if (pathInfo.equals("/sendRequest")) {
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
        
        try {
            // 直接处理 /friendId 路径格式，不再要求 /delete/ 前缀
        if (pathInfo.startsWith("/delete/")) {
            handleDeleteFriend(req, resp);
            } else if (pathInfo.startsWith("/")) { // 处理/friendId格式
                // 从路径中提取好友ID
                String friendId = pathInfo.substring(1);
                req.setAttribute("friendId", friendId);
            handleDeleteFriend(req, resp);
        } else {
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_NOT_FOUND, "未找到请求的资源: DELETE " + pathInfo);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "处理DELETE请求时发生异常: " + pathInfo, e);
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "服务器内部错误: " + e.getMessage());
        }
    }
    
    /**
     * 处理获取收到的好友请求
     */
    private void handleGetReceivedRequests(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        
        if (session == null || session.getAttribute("student") == null) {
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, "未登录");
            return;
        }
        
        Student sessionStudent = (Student) session.getAttribute("student");
        String studentId = sessionStudent.getStudentId();
        
        try {
            List<Student> receivedRequests = friendshipDao.findReceivedRequestsByStudentId(studentId);
            ResponseUtil.sendSuccessResponse(resp, "获取收到的好友请求成功", receivedRequests);
        } catch (Exception e) {
            System.out.println("[FriendshipServlet] 获取收到的好友请求异常: " + e.getMessage());
            e.printStackTrace();
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "获取收到的好友请求失败: " + e.getMessage());
        }
    }

    /**
     * 处理获取已发送的好友请求
     */
    private void handleGetSentRequests(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        
        if (session == null || session.getAttribute("student") == null) {
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, "未登录");
            return;
        }
        
        Student sessionStudent = (Student) session.getAttribute("student");
        String studentId = sessionStudent.getStudentId();
        
        try {
            List<Student> sentRequests = friendshipDao.findPendingRequestsByStudentId(studentId);
            ResponseUtil.sendSuccessResponse(resp, "获取发送的好友请求成功", sentRequests);
        } catch (Exception e) {
            System.out.println("[FriendshipServlet] 获取发送的好友请求异常: " + e.getMessage());
            e.printStackTrace();
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "获取发送的好友请求失败: " + e.getMessage());
        }
    }

    /**
     * 处理获取好友推荐
     */
    private void handleGetRecommendations(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        
        if (session == null || session.getAttribute("student") == null) {
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, "未登录");
            return;
        }
        
        Student sessionStudent = (Student) session.getAttribute("student");
        String studentId = sessionStudent.getStudentId();
        
        String type = req.getParameter("type");
        try {
            List<Student> recommendations;
            if (type == null || "all".equals(type)) {
                recommendations = friendshipDao.getAllRecommendations(studentId);
            } else if ("mutual".equals(type)) {
                int limit = req.getParameter("limit") != null ? Integer.parseInt(req.getParameter("limit")) : 10;
                recommendations = friendshipDao.recommendByMutualFriends(studentId, limit);
            } else if ("courses".equals(type)) {
                int limit = req.getParameter("limit") != null ? Integer.parseInt(req.getParameter("limit")) : 10;
                recommendations = friendshipDao.recommendBySameCourses(studentId, limit);
            } else if ("active".equals(type)) {
                int limit = req.getParameter("limit") != null ? Integer.parseInt(req.getParameter("limit")) : 10;
                recommendations = friendshipDao.recommendByRecentActivity(studentId, limit);
            } else {
                ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, "无效的推荐类型");
                return;
            }
            
            ResponseUtil.sendSuccessResponse(resp, "获取好友推荐成功", recommendations);
        } catch (Exception e) {
            System.out.println("[FriendshipServlet] 获取好友推荐异常: " + e.getMessage());
            e.printStackTrace();
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "获取好友推荐失败: " + e.getMessage());
        }
    }
} 