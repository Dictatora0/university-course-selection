package servlet;

import java.io.IOException;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import dao.CourseDAO;
import model.Course;
import model.Student;
import model.Administrator;
import util.ResponseUtil;

@WebServlet("/api/course/*")
public class CourseServlet extends BaseServlet {
    
    private CourseDAO courseDAO = new CourseDAO();
    private static final Gson GSON = new Gson();
    
    /**
     * 处理GET请求
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        System.out.println("[CourseServlet] doGet pathInfo: " + pathInfo);
        
        // 处理搜索请求
        if (pathInfo != null && pathInfo.equals("/search")) {
            search(req, resp);
            return;
        }
        
        // 对于其他GET请求，交给父类处理
        super.doGet(req, resp);
    }
    
    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        
        HttpSession session = req.getSession(false);
        // 验证管理员权限
        if (session == null || session.getAttribute("admin") == null) {
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_UNAUTHORIZED, "未登录或没有删除课程的权限");
            return;
        }
        
        // 获取管理员信息
        Administrator admin = (Administrator) session.getAttribute("admin");
        // 只有超级管理员和课程管理员可以删除课程
        if (!"SUPER_ADMIN".equals(admin.getRole()) && !"COURSE_ADMIN".equals(admin.getRole())) {
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_FORBIDDEN, "您没有删除课程的权限");
            return;
        }
        
        if (pathInfo != null && pathInfo.length() > 1) {
            // 提取课程ID
            String courseId = pathInfo.substring(1);
            
            try {
                // 检查课程是否存在
                Course course = courseDAO.findById(courseId);
                if (course == null) {
                    ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_NOT_FOUND, "课程不存在: " + courseId);
                    return;
                }
                
                // 判断课程是否有学生选课，如果有则不允许删除
                if (course.getEnrollmentCount() > 0) {
                    ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_CONFLICT, 
                            "该课程已有学生选课，无法删除。请先取消所有选课记录。");
                    return;
                }
                
                // 删除课程
                boolean success = courseDAO.delete(courseId);
                if (success) {
                    ResponseUtil.sendSuccessResponse(resp, "课程删除成功");
                } else {
                    ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "课程删除失败");
                }
            } catch (Exception e) {
                ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                        "删除课程时发生错误: " + e.getMessage());
            }
        } else {
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, "无效的课程ID");
        }
    }
    
    public void list(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        if (session != null && (session.getAttribute("student") != null || session.getAttribute("admin") != null)) {
            List<Course> courses = courseDAO.findAllWithDeptName();
            ResponseUtil.sendSuccess(resp, courses);
        } else {
            ResponseUtil.sendError(resp, "未登录");
        }
    }
    
    public void getByDept(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        if (session != null && session.getAttribute("student") != null) {
            String deptId = req.getParameter("deptId");
            if (deptId != null && !deptId.isEmpty()) {
                List<Course> courses = courseDAO.findByDeptId(deptId);
                ResponseUtil.sendSuccess(resp, courses);
            } else {
                ResponseUtil.sendError(resp, "院系ID不能为空");
            }
        } else {
            ResponseUtil.sendError(resp, "未登录");
        }
    }
    
    /**
     * 添加课程（仅管理员）
     */
    public void add(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        // 验证管理员权限
        if (session == null || session.getAttribute("admin") == null) {
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_UNAUTHORIZED, "未登录或没有添加课程的权限");
            return;
        }
        
        // 获取管理员信息
        Administrator admin = (Administrator) session.getAttribute("admin");
        // 只有超级管理员和课程管理员可以添加课程
        if (!"SUPER_ADMIN".equals(admin.getRole()) && !"COURSE_ADMIN".equals(admin.getRole())) {
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_FORBIDDEN, "您没有添加课程的权限");
            return;
        }
        
        try {
            // 解析请求体中的JSON数据
            String requestBody = req.getReader().lines().reduce("", (accumulator, actual) -> accumulator + actual);
            Course newCourse = GSON.fromJson(requestBody, Course.class);
            
            // 验证必填字段
            if (newCourse.getCourseId() == null || newCourse.getCourseId().trim().isEmpty() ||
                newCourse.getCourseName() == null || newCourse.getCourseName().trim().isEmpty() ||
                newCourse.getDeptId() == null || newCourse.getDeptId().trim().isEmpty()) {
                ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, "课程ID、课程名称和院系ID不能为空");
                return;
            }
            
            // 验证学分和容量
            if (newCourse.getCredit() != null && newCourse.getCredit().compareTo(java.math.BigDecimal.ZERO) <= 0) {
                ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, "学分必须大于0");
                return;
            }
            
            if (newCourse.getCapacity() <= 0) {
                ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, "容量必须大于0");
                return;
            }
            
            // 检查课程ID是否已存在
            Course existingCourse = courseDAO.findById(newCourse.getCourseId());
            if (existingCourse != null) {
                ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_CONFLICT, "课程ID已存在");
                return;
            }
            
            // 添加课程
            boolean success = courseDAO.add(newCourse);
            if (success) {
                ResponseUtil.sendSuccessResponse(resp, "课程添加成功", newCourse);
            } else {
                ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "课程添加失败");
            }
        } catch (JsonSyntaxException e) {
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, "请求数据格式不正确: " + e.getMessage());
        } catch (Exception e) {
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "服务器内部错误: " + e.getMessage());
        }
    }
    
    /**
     * 搜索课程
     */
    public void search(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        System.out.println("[CourseServlet] 进入搜索方法");
        HttpSession session = req.getSession(false);
        if (session == null || (session.getAttribute("student") == null && session.getAttribute("admin") == null)) {
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_UNAUTHORIZED, "未登录或会话已过期");
            return;
        }
        
        // 获取搜索关键词
        String keyword = req.getParameter("keyword");
        System.out.println("[CourseServlet] 搜索关键词: " + keyword);
        if (keyword == null || keyword.trim().isEmpty()) {
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, "搜索关键词不能为空");
            return;
        }
        
        try {
            // 调用DAO层执行搜索
            List<Course> courses = courseDAO.searchCourses(keyword);
            System.out.println("[CourseServlet] 搜索结果数量: " + courses.size());
            ResponseUtil.sendSuccessResponse(resp, "搜索课程成功", courses);
        } catch (Exception e) {
            System.err.println("[CourseServlet] 搜索课程失败: " + e.getMessage());
            e.printStackTrace();
            ResponseUtil.sendErrorResponse(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                    "搜索课程失败: " + e.getMessage());
        }
    }
} 