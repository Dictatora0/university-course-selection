package servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.google.gson.Gson;
import dao.EnrollmentDAO;
import model.Enrollment;
import model.Student;
import util.ResponseUtil;

@WebServlet("/api/enrollment/*")
public class EnrollmentServlet extends BaseServlet {
    
    private EnrollmentDAO enrollmentDAO = new EnrollmentDAO();
    private Gson gson = new Gson();
    
    public void list(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        if (session != null && session.getAttribute("student") != null) {
            Student student = (Student) session.getAttribute("student");
            List<Enrollment> enrollments = enrollmentDAO.findByStudentId(student.getStudentId());
            ResponseUtil.sendSuccess(resp, enrollments);
        } else {
            ResponseUtil.sendError(resp, "未登录");
        }
    }
    
    public void add(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        if (session != null && session.getAttribute("student") != null) {
            BufferedReader reader = req.getReader();
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            
            Enrollment enrollment = gson.fromJson(builder.toString(), Enrollment.class);
            
            Student student = (Student) session.getAttribute("student");
            enrollment.setStudentId(student.getStudentId());
            enrollment.setEnrollmentDate(new Date());
            
            if (enrollmentDAO.isEnrolled(student.getStudentId(), enrollment.getCourseId())) {
                ResponseUtil.sendError(resp, "已经选过该课程");
                return;
            }
            
            boolean success = enrollmentDAO.add(enrollment);
            
            if (success) {
                ResponseUtil.sendSuccess(resp, null);
            } else {
                ResponseUtil.sendError(resp, "选课失败");
            }
        } else {
            ResponseUtil.sendError(resp, "未登录");
        }
    }
    
    /**
     * 选课方法 (别名，与add方法功能相同)
     */
    public void enroll(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        add(req, resp);
    }
    
    public void drop(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        if (session != null && session.getAttribute("student") != null) {
            String courseId = req.getParameter("courseId");
            
            if (courseId == null || courseId.isEmpty()) {
                ResponseUtil.sendError(resp, "课程ID不能为空");
                return;
            }
            
            Student student = (Student) session.getAttribute("student");
            boolean success = enrollmentDAO.delete(student.getStudentId(), courseId);
            
            if (success) {
                ResponseUtil.sendSuccess(resp, null);
            } else {
                ResponseUtil.sendError(resp, "退课失败");
            }
        } else {
            ResponseUtil.sendError(resp, "未登录");
        }
    }
} 