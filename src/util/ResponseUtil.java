package util;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.http.HttpServletResponse;
import com.google.gson.Gson;

/**
 * HTTP响应工具类
 */
public class ResponseUtil {
    private static final Gson gson = new Gson();
    
    /**
     * 发送JSON格式的响应
     * @param response HTTP响应对象
     * @param object 要序列化为JSON的对象
     */
    public static void writeJson(HttpServletResponse response, Object object) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        String json = gson.toJson(object);
        
        PrintWriter out = response.getWriter();
        out.print(json);
        out.flush();
    }
    
    /**
     * 发送成功响应
     * @param response HTTP响应对象
     * @param data 响应数据
     */
    public static void sendSuccess(HttpServletResponse response, Object data) throws IOException {
        ApiResponse apiResponse = new ApiResponse(true, "操作成功", data);
        writeJson(response, apiResponse);
    }
    
    /**
     * 发送错误响应
     * @param response HTTP响应对象
     * @param message 错误消息
     */
    public static void sendError(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        ApiResponse apiResponse = new ApiResponse(false, message, null);
        writeJson(response, apiResponse);
    }
    
    /**
     * 发送服务器错误响应
     * @param response HTTP响应对象
     */
    public static void sendServerError(HttpServletResponse response, Exception e) throws IOException {
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        ApiResponse apiResponse = new ApiResponse(false, "服务器内部错误: " + e.getMessage(), null);
        writeJson(response, apiResponse);
    }
    
    /**
     * API响应封装类
     */
    static class ApiResponse {
        private boolean success;
        private String message;
        private Object data;
        
        public ApiResponse(boolean success, String message, Object data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public String getMessage() {
            return message;
        }
        
        public Object getData() {
            return data;
        }
    }
} 