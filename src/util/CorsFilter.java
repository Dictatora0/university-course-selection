package util;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class CorsFilter implements Filter {
    
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // 初始化过滤器
    }
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        // 获取请求的Origin
        String origin = httpRequest.getHeader("Origin");
        
        // 允许的域名
        if (origin != null) {
            httpResponse.setHeader("Access-Control-Allow-Origin", origin);
        } else {
            httpResponse.setHeader("Access-Control-Allow-Origin", "*");
        }
        
        // 允许的HTTP方法
        httpResponse.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        
        // 允许的HTTP头
        httpResponse.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With");
        
        // 允许Cookie
        httpResponse.setHeader("Access-Control-Allow-Credentials", "true");
        
        // 预检请求的缓存时间
        httpResponse.setHeader("Access-Control-Max-Age", "3600");
        
        // 如果是预检请求，直接返回
        if ("OPTIONS".equalsIgnoreCase(httpRequest.getMethod())) {
            httpResponse.setStatus(HttpServletResponse.SC_OK);
            return;
        }
        
        // 记录会话ID，用于调试
        if (httpRequest.getSession(false) != null) {
            System.out.println("[CorsFilter] 请求包含会话ID: " + httpRequest.getSession().getId());
        } else {
            System.out.println("[CorsFilter] 请求不包含会话ID");
        }
        
        // 继续执行过滤器链
        chain.doFilter(request, response);
    }
    
    @Override
    public void destroy() {
        // 销毁过滤器
    }
} 