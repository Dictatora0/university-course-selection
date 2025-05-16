package util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 数据库连接工具类
 */
public class DBConnection {
    private static final String CONFIG_FILE = "config.properties";
    private static String jdbcUrl;
    private static String jdbcUsername;
    private static String jdbcPassword;
    private static boolean initialized = false;

    static {
        try {
            loadConfig();
            Class.forName("com.mysql.cj.jdbc.Driver");
            initialized = true;
        } catch (ClassNotFoundException e) {
            System.err.println("MySQL JDBC Driver not found: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Failed to load database configuration: " + e.getMessage());
        }
    }

    /**
     * 加载数据库配置
     */
    private static void loadConfig() throws IOException {
        Properties prop = new Properties();
        
        // 首先尝试从类路径加载
        InputStream inputStream = DBConnection.class.getClassLoader().getResourceAsStream(CONFIG_FILE);
        
        // 如果类路径中没有找到，则尝试从文件系统加载
        if (inputStream == null) {
            inputStream = new FileInputStream(CONFIG_FILE);
        }
        
        if (inputStream != null) {
            prop.load(inputStream);
            jdbcUrl = prop.getProperty("jdbc.url");
            jdbcUsername = prop.getProperty("jdbc.username");
            jdbcPassword = prop.getProperty("jdbc.password");
            inputStream.close();
        } else {
            // 使用默认配置
            jdbcUrl = "jdbc:mysql://localhost:3306/course_selection?useSSL=false&serverTimezone=UTC";
            jdbcUsername = "root";
            jdbcPassword = "password";
            System.out.println("使用默认数据库配置。");
        }
    }

    /**
     * 获取数据库连接
     */
    public static Connection getConnection() throws SQLException {
        if (!initialized) {
            throw new SQLException("数据库连接初始化失败");
        }
        return DriverManager.getConnection(jdbcUrl, jdbcUsername, jdbcPassword);
    }

    /**
     * 关闭数据库资源
     */
    public static void close(Connection conn, PreparedStatement pstmt, ResultSet rs) {
        try {
            if (rs != null) {
                rs.close();
            }
            if (pstmt != null) {
                pstmt.close();
            }
            if (conn != null) {
                conn.close();
            }
        } catch (SQLException e) {
            System.err.println("关闭数据库资源时出错: " + e.getMessage());
        }
    }

    /**
     * 设置数据库连接信息
     */
    public static void setConnectionInfo(String url, String username, String password) {
        jdbcUrl = url;
        jdbcUsername = username;
        jdbcPassword = password;
    }
} 