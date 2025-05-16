package servlet;

import java.io.BufferedReader;
import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.google.gson.Gson;
import dao.StudentDAO;
import model.Student;
import util.PasswordUtil;
import util.ResponseUtil;

@WebServlet("/api/student/*")
public class StudentServlet extends BaseServlet {
    
    private StudentDAO studentDAO = new StudentDAO();
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
} 