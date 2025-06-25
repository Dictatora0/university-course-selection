package servlet;

import com.google.gson.Gson;
import dao.DepartmentDAO;
import model.Department;
import util.ResponseUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 处理院系相关的请求
 */
@WebServlet("/api/department/*")
public class DepartmentServlet extends BaseServlet {
    private static final Logger LOGGER = Logger.getLogger(DepartmentServlet.class.getName());
    private DepartmentDAO departmentDAO = new DepartmentDAO();
    private Gson gson = new Gson();
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        LOGGER.info("Received GET request with pathInfo: " + pathInfo);
        
        // /api/department/list
        if (pathInfo == null || pathInfo.equals("/") || pathInfo.equals("/list")) {
            listAllDepartments(req, resp);
        }
        // /api/department/{deptId}
        else {
            String deptId = pathInfo.substring(1);
            getDepartmentById(req, resp, deptId);
        }
    }
    
    /**
     * 获取所有院系
     */
    private void listAllDepartments(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            List<Department> departments = departmentDAO.findAll();
            ResponseUtil.sendSuccess(resp, departments);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "获取院系列表时发生错误", e);
            ResponseUtil.sendError(resp, "获取院系列表时发生错误: " + e.getMessage());
        }
    }
    
    /**
     * 根据ID获取院系
     */
    private void getDepartmentById(HttpServletRequest req, HttpServletResponse resp, String deptId) throws IOException {
        try {
            Department department = departmentDAO.findById(deptId);
            if (department != null) {
                ResponseUtil.sendSuccess(resp, department);
            } else {
                ResponseUtil.sendError(resp, "找不到ID为 " + deptId + " 的院系");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "获取院系信息时发生错误", e);
            ResponseUtil.sendError(resp, "获取院系信息时发生错误: " + e.getMessage());
        }
    }
} 