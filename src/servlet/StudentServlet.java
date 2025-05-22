package servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
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
        
        // 添加调试输出
        System.out.println("[StudentServlet.register] 尝试解析JSON数据，长度: " + jsonData.length());
        
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
            
            // 检查各字段是否为null或空
            System.out.println("[StudentServlet.register] 字段检查:");
            System.out.println("  studentId 是否为空: " + (registerInfo.getStudentId() == null || registerInfo.getStudentId().trim().isEmpty()));
            System.out.println("  name 是否为空: " + (registerInfo.getName() == null || registerInfo.getName().trim().isEmpty()));
            System.out.println("  password 是否为空: " + (registerInfo.getPassword() == null || registerInfo.getPassword().trim().isEmpty()));
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
            Student sessionStudent = (Student) session.getAttribute("student");
            // 从数据库获取最新的、完整的学生信息，包括关联的院系名称
            Student studentDetails = studentDAO.findById(sessionStudent.getStudentId()); 
            
            if (studentDetails != null) {
                studentDetails.setPassword(null); // 不返回密码
                ResponseUtil.sendSuccess(resp, studentDetails);
            } else {
                // 如果根据session中的ID找不到用户（例如用户被删），则清理session并报错
                session.removeAttribute("student");
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
        } else if (pathInfo.equals("/updateInfo")) {
            updateInfo(req, resp);
        } else if (pathInfo.equals("/changePassword")) {
            changePassword(req, resp);
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

    /**
     * 更新学生个人信息
     */
    public void updateInfo(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // 检查用户是否已登录
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("student") == null) {
            ResponseUtil.sendError(resp, "未登录");
            return;
        }
        
        try {
            // 获取当前登录的学生信息
            Student currentStudent = (Student) session.getAttribute("student");
            String studentId = currentStudent.getStudentId();
            
            // 解析请求体中的JSON数据
            BufferedReader reader = req.getReader();
            JsonObject jsonObject = gson.fromJson(reader, JsonObject.class);
            
            // 从数据库中获取学生完整信息
            Student student = studentDAO.findById(studentId);
            if (student == null) {
                ResponseUtil.sendError(resp, "找不到学生信息");
                return;
            }
            
            // 更新学生信息，只更新请求中包含的字段
            boolean updated = false;
            
            // 处理院系ID字段
            if (jsonObject.has("deptId")) {
                String deptId = jsonObject.get("deptId").getAsString();
                if (!deptId.isEmpty()) {
                    // 验证院系是否存在
                    Department dept = departmentDAO.findById(deptId);
                    if (dept != null) {
                        student.setDeptId(deptId);
                        student.setDeptName(dept.getDeptName());
                        updated = true;
                    } else {
                        ResponseUtil.sendError(resp, "指定的院系不存在");
                        return;
                    }
                }
            }
            
            // 处理出生日期字段
            if (jsonObject.has("birthDate") && !jsonObject.get("birthDate").isJsonNull()) {
                String birthDateStr = jsonObject.get("birthDate").getAsString();
                if (!birthDateStr.isEmpty()) {
                    try {
                        // 解析日期字符串（格式：yyyy-MM-dd）
                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                        Date birthDate = dateFormat.parse(birthDateStr);
                        student.setBirthDate(birthDate);
                        updated = true;
                    } catch (ParseException e) {
                        ResponseUtil.sendError(resp, "出生日期格式无效，请使用 yyyy-MM-dd 格式");
                        return;
                    }
                }
            }
            
            // 处理身份证号字段
            if (jsonObject.has("idCard") && !jsonObject.get("idCard").isJsonNull()) {
                String idCard = jsonObject.get("idCard").getAsString();
                student.setIdCard(idCard);
                updated = true;
            }
            
            // 处理地址字段
            if (jsonObject.has("address") && !jsonObject.get("address").isJsonNull()) {
                String address = jsonObject.get("address").getAsString();
                student.setAddress(address);
                updated = true;
            }
            
            // 处理email字段
            if (jsonObject.has("email") && !jsonObject.get("email").isJsonNull()) {
                String email = jsonObject.get("email").getAsString();
                student.setEmail(email);
                updated = true;
            }
            
            // 处理phone字段
            if (jsonObject.has("phone") && !jsonObject.get("phone").isJsonNull()) {
                String phone = jsonObject.get("phone").getAsString();
                student.setPhone(phone);
                updated = true;
            }
            
            if (updated) {
                // 更新数据库
                boolean success = studentDAO.update(student);
                if (success) {
                    // 更新会话中的学生信息
                    session.setAttribute("student", student);
                    // 准备响应数据
                    Map<String, Object> responseData = new HashMap<>();
                    responseData.put("studentId", student.getStudentId());
                    responseData.put("name", student.getName());
                    responseData.put("deptId", student.getDeptId());
                    responseData.put("deptName", student.getDeptName());
                    responseData.put("birthDate", student.getBirthDate());
                    responseData.put("idCard", student.getIdCard());
                    responseData.put("address", student.getAddress());
                    responseData.put("balance", student.getBalance());
                    responseData.put("email", student.getEmail());
                    responseData.put("phone", student.getPhone());
                    
                    ResponseUtil.sendSuccess(resp, responseData);
                } else {
                    ResponseUtil.sendError(resp, "更新学生信息失败");
                }
            } else {
                ResponseUtil.sendError(resp, "没有提供有效的更新字段");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "更新学生信息时发生错误", e);
            ResponseUtil.sendError(resp, "更新学生信息时发生错误: " + e.getMessage());
        }
    }

    /**
     * 修改学生密码
     */
    public void changePassword(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // 检查用户是否已登录
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("student") == null) {
            ResponseUtil.sendError(resp, "未登录");
            return;
        }
        
        try {
            // 获取当前登录的学生信息
            Student currentStudent = (Student) session.getAttribute("student");
            String studentId = currentStudent.getStudentId();
            LOGGER.info("[changePassword] 当前登录用户: " + studentId);
            
            // 解析请求体中的JSON数据
            BufferedReader reader = req.getReader();
            JsonObject jsonObject = gson.fromJson(reader, JsonObject.class);
            
            // 检查请求参数
            if (!jsonObject.has("currentPassword") || !jsonObject.has("newPassword")) {
                ResponseUtil.sendError(resp, "请提供当前密码和新密码");
                return;
            }
            
            String currentPassword = jsonObject.get("currentPassword").getAsString();
            String newPassword = jsonObject.get("newPassword").getAsString();
            LOGGER.info("[changePassword] 收到修改密码请求, 当前密码长度: " + currentPassword.length() + ", 新密码长度: " + newPassword.length());
            
            // 验证密码格式
            if (newPassword.length() < 6) {
                ResponseUtil.sendError(resp, "新密码长度不能少于6位");
                return;
            }
            
            // 验证当前密码是否正确
            LOGGER.info("[changePassword] 验证当前密码...");
            Student student = studentDAO.validateLogin(studentId, currentPassword);
            if (student == null) {
                LOGGER.warning("[changePassword] 当前密码验证失败");
                ResponseUtil.sendError(resp, "当前密码不正确");
                return;
            }
            LOGGER.info("[changePassword] 当前密码验证成功，准备更新为新密码");
            
            // 设置新密码
            student.setPassword(newPassword);
            
            // 输出更多信息用于调试
            LOGGER.info("[changePassword] 更新前的用户信息: 学号=" + student.getStudentId() + ", 姓名=" + student.getName() + 
                      ", 院系ID=" + student.getDeptId() + ", 地址=" + student.getAddress() + 
                      ", 邮箱=" + student.getEmail() + ", 电话=" + student.getPhone());
            
            boolean success = studentDAO.update(student);
            LOGGER.info("[changePassword] 密码更新结果: " + success);
            
            if (success) {
                // 更新会话中的学生信息
                session.setAttribute("student", student);
                ResponseUtil.sendSuccess(resp, "密码修改成功");
            } else {
                ResponseUtil.sendError(resp, "密码修改失败");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "修改密码时发生错误", e);
            ResponseUtil.sendError(resp, "修改密码时发生错误: " + e.getMessage());
        }
    }
} 