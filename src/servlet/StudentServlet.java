package servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.google.gson.Gson;
import dao.CourseDAO;
import dao.DepartmentDAO;
import dao.StudentDAO;
import model.Course;
import model.Department;
import model.Student;
import util.PasswordUtil;
import util.ResponseUtil;

@WebServlet("/api/student/*")
public class StudentServlet extends BaseServlet {
    
    private StudentDAO studentDAO = new StudentDAO();
    private DepartmentDAO departmentDAO = new DepartmentDAO();
    private CourseDAO courseDAO = new CourseDAO();
    private Gson gson = new Gson();
    
    public void login(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        BufferedReader reader = req.getReader();
        StringBuilder builder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            builder.append(line);
        }
        
        Student loginInfo = gson.fromJson(builder.toString(), Student.class);
        
        Student student = studentDAO.validateLogin(loginInfo.getStudentId(), loginInfo.getPassword());
        
        if (student != null) {
            // 创建会话
            HttpSession session = req.getSession();
            session.setAttribute("student", student);
            
            // 返回用户信息（不包含密码）
            student.setPassword(null);
            ResponseUtil.sendSuccess(resp, student);
        } else {
            ResponseUtil.sendError(resp, "学号或密码错误");
        }
    }
    
    public void register(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        System.out.println("[StudentServlet.register] 开始处理注册请求");
        BufferedReader reader = req.getReader();
        StringBuilder builder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            builder.append(line);
        }
        
        String jsonData = builder.toString();
        System.out.println("[StudentServlet.register] 收到的JSON数据: " + jsonData);
        
        Student registerInfo = gson.fromJson(jsonData, Student.class);
        System.out.println("[StudentServlet.register] 解析后的学生信息: " + registerInfo);
        
        // 检查学号是否已存在
        Student existingStudent = studentDAO.findById(registerInfo.getStudentId());
        if (existingStudent != null) {
            System.out.println("[StudentServlet.register] 学号已存在: " + registerInfo.getStudentId());
            ResponseUtil.sendError(resp, "该学号已被注册");
            return;
        }
        
        // 检查必要字段是否为空
        if (registerInfo.getStudentId() == null || registerInfo.getStudentId().trim().isEmpty() ||
            registerInfo.getName() == null || registerInfo.getName().trim().isEmpty() ||
            registerInfo.getPassword() == null || registerInfo.getPassword().trim().isEmpty()) {
            System.out.println("[StudentServlet.register] 必要字段为空");
            ResponseUtil.sendError(resp, "学号、姓名和密码不能为空");
            return;
        }
        
        // 确保非必要字段为null，避免唯一键冲突
        registerInfo.setBirthDate(null);
        registerInfo.setIdCard(null);
        registerInfo.setAddress(null);
        
        System.out.println("[StudentServlet.register] 准备添加学生: " + registerInfo);
        
        // 添加新学生
        boolean success = studentDAO.add(registerInfo);
        
        if (success) {
            System.out.println("[StudentServlet.register] 注册成功");
            ResponseUtil.sendSuccess(resp, null);
        } else {
            System.out.println("[StudentServlet.register] 注册失败");
            ResponseUtil.sendError(resp, "注册失败，请稍后再试");
        }
    }
    
    public void getInfo(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        if (session != null && session.getAttribute("student") != null) {
            Student student = (Student) session.getAttribute("student");
            student = studentDAO.findById(student.getStudentId());
            student.setPassword(null); // 不返回密码
            ResponseUtil.sendSuccess(resp, student);
        } else {
            ResponseUtil.sendError(resp, "未登录");
        }
    }
    
    public void logout(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        ResponseUtil.sendSuccess(resp, null);
    }
    
    public void importCourses(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("student") == null) {
            ResponseUtil.sendError(resp, "未登录");
            return;
        }
        
        // 读取CSV文件
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(
                        getClass().getClassLoader().getResourceAsStream("course.csv"), 
                        StandardCharsets.UTF_8))) {
            
            // 跳过标题行
            br.readLine();
            
            String line;
            Map<String, String> departmentMap = new HashMap<>();
            List<Course> courses = new ArrayList<>();
            int importCount = 0;
            
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                if (values.length >= 3) {
                    String courseId = values[0].trim();
                    String courseName = values[1].trim();
                    String deptName = values[2].trim();
                    
                    // 处理院系信息
                    String deptId = departmentMap.get(deptName);
                    if (deptId == null) {
                        // 查找或创建院系
                        Department dept = new Department();
                        dept.setDeptName(deptName);
                        
                        // 生成院系ID (简单使用名称的哈希码)
                        deptId = "DEPT" + Math.abs(deptName.hashCode() % 1000);
                        dept.setDeptId(deptId);
                        
                        // 检查院系是否存在
                        Department existingDept = departmentDAO.findByName(deptName);
                        if (existingDept == null) {
                            departmentDAO.add(dept);
                        } else {
                            deptId = existingDept.getDeptId();
                        }
                        
                        departmentMap.put(deptName, deptId);
                    }
                    
                    // 创建课程
                    Course course = new Course();
                    course.setCourseId(courseId);
                    course.setCourseName(courseName);
                    course.setDeptId(deptId);
                    course.setCredit(new BigDecimal("3.0")); // 默认学分为3.0
                    
                    // 检查课程是否已存在
                    Course existingCourse = courseDAO.findById(courseId);
                    if (existingCourse == null) {
                        courseDAO.add(course);
                        importCount++;
                    }
                    
                    courses.add(course);
                }
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("importCount", importCount);
            result.put("totalCourses", courses.size());
            
            ResponseUtil.sendSuccess(resp, result);
            
        } catch (Exception e) {
            e.printStackTrace();
            ResponseUtil.sendError(resp, "导入失败: " + e.getMessage());
        }
    }
} 