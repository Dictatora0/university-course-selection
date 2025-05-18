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
import com.google.gson.JsonSyntaxException;
import dao.CourseDAO;
import dao.DepartmentDAO;
import dao.StudentDAO;
import model.Course;
import model.Department;
import model.Student;
import util.PasswordUtil;
import util.ResponseUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebServlet("/api/students/*")
public class StudentServlet extends BaseServlet {
    private static final Logger LOGGER = Logger.getLogger(StudentServlet.class.getName());
    private StudentDAO studentDAO = new StudentDAO();
    private DepartmentDAO departmentDAO = new DepartmentDAO();
    private CourseDAO courseDAO = new CourseDAO();
    private Gson gson = new Gson();
    
    public void login(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        System.out.println("[StudentServlet.login] 开始处理登录请求");
        BufferedReader reader = req.getReader();
        StringBuilder builder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            builder.append(line);
        }
        
        String jsonData = builder.toString();
        System.out.println("[StudentServlet.login] 收到的原始JSON数据: " + jsonData);
        
        Student loginInfo = gson.fromJson(jsonData, Student.class);
        System.out.println("[StudentServlet.login] 解析后的学号: " + loginInfo.getStudentId());
        System.out.println("[StudentServlet.login] 解析后的密码: " + loginInfo.getPassword());
        
        Student student = studentDAO.validateLogin(loginInfo.getStudentId(), loginInfo.getPassword());

        if (student != null) {
            System.out.println("[StudentServlet.login] 登录成功: " + loginInfo.getStudentId());
            HttpSession session = req.getSession(true);
            session.setAttribute("student", student);
            student.setPassword(null);
            ResponseUtil.sendSuccess(resp, student);
        } else {
            System.out.println("[StudentServlet.login] 登录失败: " + loginInfo.getStudentId());
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
        System.out.println("[StudentServlet.register] 收到的原始JSON数据: " + jsonData);
        
        Student registerInfo = gson.fromJson(jsonData, Student.class);
        System.out.println("[StudentServlet.register] GSON解析后的Student对象: " + registerInfo);
        if (registerInfo != null) {
            System.out.println("[StudentServlet.register] 解析后的学号: " + registerInfo.getStudentId());
            System.out.println("[StudentServlet.register] 解析后的姓名: " + registerInfo.getName());
            System.out.println("[StudentServlet.register] 解析后的密码 (原始): " + registerInfo.getPassword());
            System.out.println("[StudentServlet.register] 解析后的出生日期: " + registerInfo.getBirthDate());
            System.out.println("[StudentServlet.register] 解析后的身份证号: " + registerInfo.getIdCard());
            System.out.println("[StudentServlet.register] 解析后的地址: " + registerInfo.getAddress());
            System.out.println("[StudentServlet.register] 解析后的院系ID: " + registerInfo.getDeptId());
        } else {
            System.out.println("[StudentServlet.register] GSON解析JSON数据后得到null对象");
            ResponseUtil.sendError(resp, "无法解析注册信息");
            return;
        }
        
        Student existingStudent = studentDAO.findById(registerInfo.getStudentId());
        if (existingStudent != null) {
            System.out.println("[StudentServlet.register] 学号已存在: " + registerInfo.getStudentId());
            ResponseUtil.sendError(resp, "该学号已被注册");
            return;
        }
        
        if (registerInfo.getStudentId() == null || registerInfo.getStudentId().trim().isEmpty() ||
            registerInfo.getName() == null || registerInfo.getName().trim().isEmpty() ||
            registerInfo.getPassword() == null || registerInfo.getPassword().trim().isEmpty()) {
            System.out.println("[StudentServlet.register] 必要字段为空。学号: " + registerInfo.getStudentId() + ", 姓名: " + registerInfo.getName() + ", 密码: " + (registerInfo.getPassword() == null ? "null" : "***"));
            ResponseUtil.sendError(resp, "学号、姓名和密码不能为空");
            return;
        }

        System.out.println("[StudentServlet.register] 准备添加学生: " + registerInfo);
        
        boolean success = studentDAO.add(registerInfo);
        
        if (success) {
            System.out.println("[StudentServlet.register] 注册成功");
            ResponseUtil.sendSuccess(resp, null);
        } else {
            System.out.println("[StudentServlet.register] 注册失败");
            ResponseUtil.sendError(resp, "注册失败，请稍后再试");
        }
    }

    public void logout(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        ResponseUtil.sendSuccess(resp, null);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        LOGGER.info("Received GET request with pathInfo: " + pathInfo);

        if (pathInfo == null || pathInfo.equals("/")) {
            findAllStudents(req, resp);
        } else if (pathInfo.equals("/getInfo")) {
            getInfo(req, resp);
        } else {
            try {
                String studentId = pathInfo.substring(1);
                findStudentById(req, resp);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error parsing student ID from path", e);
                ResponseUtil.sendError(resp, "无效的学生ID格式");
            }
        }
    }

    public void findAllStudents(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        List<Student> students = studentDAO.findAll();
        for (Student s : students) {
            s.setPassword(null);
        }
        ResponseUtil.sendSuccess(resp, students);
    }

    public void findStudentById(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String studentId = req.getParameter("studentId");
        if (studentId == null || studentId.trim().isEmpty()) {
            ResponseUtil.sendError(resp, "缺少studentId参数");
            return;
        }
        Student student = studentDAO.findById(studentId);
        if (student != null) {
            student.setPassword(null);
            ResponseUtil.sendSuccess(resp, student);
        } else {
            ResponseUtil.sendError(resp, "未找到学号为 " + studentId + " 的学生");
        }
    }

    public void getInfo(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        if (session != null && session.getAttribute("student") != null) {
            Student student = (Student) session.getAttribute("student");
            student = studentDAO.findById(student.getStudentId());
            if (student != null) {
                student.setPassword(null);
                ResponseUtil.sendSuccess(resp, student);
            } else {
                ResponseUtil.sendError(resp, "无法获取用户信息，请重新登录");
            }
        } else {
            ResponseUtil.sendError(resp, "未登录");
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        ResponseUtil.sendError(resp, "暂不支持PUT请求");
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        ResponseUtil.sendError(resp, "暂不支持DELETE请求");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        LOGGER.info("Received POST request with pathInfo: " + pathInfo);
        
        if (pathInfo == null || pathInfo.equals("/")) {
            ResponseUtil.sendError(resp, "请指定操作");
            return;
        }
        
        if (pathInfo.equals("/register")) {
            register(req, resp);
        } else if (pathInfo.equals("/login")) {
            login(req, resp);
        } else if (pathInfo.equals("/logout")) {
            logout(req, resp);
        } else if (pathInfo.equals("/importCourses")) {
            importCourses(req, resp);
        } else {
            ResponseUtil.sendError(resp, "未知操作: " + pathInfo);
        }
    }

    public void importCourses(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("student") == null) {
            ResponseUtil.sendError(resp, "未登录");
            return;
        }
        
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(
                        getClass().getClassLoader().getResourceAsStream("course.csv"), 
                        StandardCharsets.UTF_8))) {
            
            br.readLine();
            
            String line;
            List<Map<String, Object>> coursesList = new ArrayList<>();
            List<String> errors = new ArrayList<>();

            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                if (values.length < 7) {
                    errors.add("行数据不完整: " + line);
                    continue;
                }

                Map<String, Object> courseDetails = new HashMap<>();
                try {
                    courseDetails.put("courseId", values[0].trim());
                    courseDetails.put("courseName", values[1].trim());
                    courseDetails.put("credits", Integer.parseInt(values[2].trim()));
                    courseDetails.put("departmentId", values[3].trim());
                    courseDetails.put("teacherName", values[4].trim());
                    courseDetails.put("maxCapacity", Integer.parseInt(values[5].trim()));
                    StringBuilder descriptionBuilder = new StringBuilder();
                    for (int i = 6; i < values.length; i++) {
                        descriptionBuilder.append(values[i].trim());
                        if (i < values.length - 1) {
                            descriptionBuilder.append(",");
                        }
                    }
                    courseDetails.put("description", descriptionBuilder.toString());

                    coursesList.add(courseDetails);
                } catch (NumberFormatException e) {
                    errors.add("数字格式错误: " + line + " - " + e.getMessage());
                } catch (Exception e) {
                    errors.add("处理行数据时发生未知错误: " + line + " - " + e.getMessage());
                }
            }

            if (!errors.isEmpty()) {
                ResponseUtil.sendError(resp, "处理CSV文件时发生错误: " + String.join("; ", errors));
                return;
            }

            if (coursesList.isEmpty()) {
                ResponseUtil.sendSuccess(resp, "CSV文件为空或无有效数据");
                return;
            }

            try {
                System.out.println("[StudentServlet.importCourses] 成功解析 " + coursesList.size() + " 条课程数据，待导入。");
                ResponseUtil.sendSuccess(resp, "成功解析 " + coursesList.size() + " 条课程数据，导入功能待实现。");
            } catch (Exception e) {
                ResponseUtil.sendError(resp, "导入课程到数据库时发生错误: " + e.getMessage());
            }

        } catch (IOException e) {
            ResponseUtil.sendError(resp, "读取CSV文件失败: " + e.getMessage());
        } catch (NullPointerException e) {
            ResponseUtil.sendError(resp, "找不到CSV文件 (course.csv)，请确保它在classpath中。");
        }
    }
} 