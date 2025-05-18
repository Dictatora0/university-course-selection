package servlet;

import java.io.IOException;
import java.util.List;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import dao.CourseDAO;
import model.Course;
import model.Student;
import util.ResponseUtil;

@WebServlet("/api/course/*")
public class CourseServlet extends BaseServlet {
    
    private CourseDAO courseDAO = new CourseDAO();
    
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
} 