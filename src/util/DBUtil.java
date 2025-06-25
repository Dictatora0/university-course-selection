package util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 数据库连接工具类
 */
public class DBUtil {
    private static final Logger LOGGER = Logger.getLogger(DBUtil.class.getName());
    private static String url;
    private static String username;
    private static String password;
    private static String driver;
    
    static {
        try {
            Properties prop = new Properties();
            // 尝试从classpath加载配置文件
            InputStream inputStream = DBUtil.class.getClassLoader().getResourceAsStream("config.properties");
            if (inputStream != null) {
                prop.load(inputStream);
                driver = prop.getProperty("jdbc.driver");
                url = prop.getProperty("jdbc.url");
                username = prop.getProperty("jdbc.username");
                password = prop.getProperty("jdbc.password");
                
                // 加载JDBC驱动
                Class.forName(driver);
                LOGGER.info("数据库驱动加载成功: " + driver);
            } else {
                LOGGER.severe("无法找到配置文件 config.properties");
                throw new RuntimeException("无法找到配置文件 config.properties");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "初始化数据库连接失败", e);
            throw new RuntimeException("初始化数据库连接失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取数据库连接
     * @return 数据库连接对象
     * @throws SQLException 如果获取连接失败
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }
    
    /**
     * 关闭数据库连接和相关资源
     * @param autoCloseable 需要关闭的资源
     */
    public static void close(AutoCloseable... autoCloseable) {
        if (autoCloseable != null) {
            for (AutoCloseable closeable : autoCloseable) {
                try {
                    if (closeable != null) {
                        closeable.close();
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "关闭资源失败", e);
                }
            }
        }
    }
} 