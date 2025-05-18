package servlet;

import java.io.IOException;
import java.lang.reflect.Method;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import util.ResponseUtil;

public abstract class BaseServlet extends HttpServlet {
    
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // 解决跨域问题
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
        
        // OPTIONS请求直接返回
        if (req.getMethod().equals("OPTIONS")) {
            resp.setStatus(HttpServletResponse.SC_OK);
            return;
        }
        
        // 获取请求路径中的方法名
        String requestURI = req.getRequestURI();
        System.out.println("[BaseServlet] 收到请求: " + requestURI);
        
        // 检查是否是特殊的路径模式，如/check/{id}或/conversation/{id}
        // 这些请求应该由各自的doGet, doPost等方法处理
        if (requestURI.contains("/check/") || 
            requestURI.contains("/conversation/") || 
            requestURI.contains("/read/") || 
            requestURI.contains("/delete/") ||
            requestURI.contains("/accept/") ||
            requestURI.contains("/reject/")) {
            System.out.println("[BaseServlet] 特殊路径请求，转发到对应的HTTP方法处理");
            super.service(req, resp);
            return;
        }
        
        String methodName = requestURI.substring(requestURI.lastIndexOf("/") + 1);
        System.out.println("[BaseServlet] 调用方法: " + methodName);
        
        // 处理空路径或根路径
        if (methodName.isEmpty()) {
            methodName = "root"; // 尝试调用root方法
            System.out.println("[BaseServlet] 空路径，尝试调用root方法");
        }
        
        try {
            Method method = this.getClass().getMethod(methodName, HttpServletRequest.class, HttpServletResponse.class);
            method.invoke(this, req, resp);
        } catch (NoSuchMethodException e) {
            // 如果root方法不存在，尝试index方法
            if (methodName.equals("root")) {
                try {
                    System.out.println("[BaseServlet] root方法不存在，尝试调用index方法");
                    Method indexMethod = this.getClass().getMethod("index", HttpServletRequest.class, HttpServletResponse.class);
                    indexMethod.invoke(this, req, resp);
                    return;
                } catch (Exception ex) {
                    // 如果index方法也不存在，则转给原有的HTTP方法处理
                    System.out.println("[BaseServlet] index方法也不存在，转发到对应的HTTP方法处理");
                    super.service(req, resp);
                }
            } else {
                System.err.println("[BaseServlet] 未找到方法 " + methodName);
                super.service(req, resp);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("[BaseServlet] 调用方法 " + methodName + " 失败: " + e.getMessage());
            ResponseUtil.sendServerError(resp, e);
        }
    }
} 