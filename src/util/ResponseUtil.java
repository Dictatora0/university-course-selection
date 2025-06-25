package util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

/**
 * HTTP响应工具类
 */
public class ResponseUtil {
    private static final Gson gson = new Gson();
    
    /**
     * API响应封装类
     */
    static class ApiResponse<T> { // 使用泛型以支持不同类型的data
        private boolean success;
        private String message;
        private T data; // data可以是任何类型
        
        public ApiResponse(boolean success, String message, T data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }
        
        // Getters (可以按需添加)
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public T getData() { return data; }
    }
    
    /**
     * 发送JSON格式的响应
     * @param response HTTP响应对象
     * @param object 要序列化为JSON的对象
     */
    private static void writeJson(HttpServletResponse response, Object object) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        String json = gson.toJson(object);
        try (PrintWriter out = response.getWriter()) {
            out.print(json);
            out.flush();
        }
    }
    
    /**
     * 发送成功响应 (带数据)
     * @param response HTTP响应对象
     * @param message 成功消息
     * @param data 响应数据
     */
    public static void sendSuccessResponse(HttpServletResponse response, String message, Object data) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        ApiResponse<Object> apiResponse = new ApiResponse<>(true, message, data);
        writeJson(response, apiResponse);
    }
    
    /**
     * 发送成功响应 (不带数据)
     * @param response HTTP响应对象
     * @param message 成功消息
     */
    public static void sendSuccessResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        ApiResponse<Object> apiResponse = new ApiResponse<>(true, message, null); // data 为 null
        writeJson(response, apiResponse);
    }
    
    /**
     * 发送错误响应
     * @param response HTTP响应对象
     * @param statusCode HTTP状态码
     * @param message 错误消息
     */
    public static void sendErrorResponse(HttpServletResponse response, int statusCode, String message) throws IOException {
        response.setStatus(statusCode);
        ApiResponse<Object> apiResponse = new ApiResponse<>(false, message, null); // data 为 null
        writeJson(response, apiResponse);
    }
    
    /**
     * 从请求中读取JSON数据并转换为JsonObject
     * @param request HttpServletRequest对象
     * @return JsonObject 如果成功解析则返回JsonObject，否则返回null
     */
    public static JsonObject readRequestJson(HttpServletRequest request) throws IOException {
        StringBuilder sb = new StringBuilder();
        String line;
        try (BufferedReader reader = request.getReader()) {
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        
        String requestBody = sb.toString();
        if (requestBody.isEmpty()) {
            return new JsonObject(); // 或者根据需求返回null
        }
        
        try {
            return gson.fromJson(requestBody, JsonObject.class);
        } catch (JsonSyntaxException e) {
            // 可以记录日志
            System.err.println("Error parsing JSON request string: " + e.getMessage());
            // 根据实际需求，这里可以抛出自定义异常或返回null/空JsonObject
            // 为简单起见，如果JSON无效则返回一个空的JsonObject，或者根据需要抛出异常
            return new JsonObject(); // 或者 throw new IOException("Invalid JSON format", e);
        }
    }
    
    /**
     * 发送成功响应（兼容旧代码）
     * 
     * @param response HTTP响应对象
     * @param data 响应数据
     * @throws IOException 如果写入响应失败
     */
    public static void sendSuccess(HttpServletResponse response, Object data) throws IOException {
        sendSuccessResponse(response, "操作成功", data);
    }
    
    /**
     * 发送错误响应（兼容旧代码）
     * 
     * @param response HTTP响应对象
     * @param errorMessage 错误消息
     * @throws IOException 如果写入响应失败
     */
    public static void sendError(HttpServletResponse response, String errorMessage) throws IOException {
        sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, errorMessage);
    }
    
    /**
     * 发送服务器错误响应
     * 
     * @param response HTTP响应对象
     * @param e 异常对象
     * @throws IOException 如果写入响应失败
     */
    public static void sendServerError(HttpServletResponse response, Exception e) throws IOException {
        String errorMessage = "服务器内部错误: " + e.getMessage();
        sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, errorMessage);
    }
} 