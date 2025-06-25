package util;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * 数据库连接工具类 (DBUtil的别名)
 */
public class DBConnection {
    
    /**
     * 获取数据库连接
     * @return 数据库连接对象
     * @throws SQLException 如果获取连接失败
     */
    public static Connection getConnection() throws SQLException {
        return DBUtil.getConnection();
    }
    
    /**
     * 关闭数据库连接和相关资源
     */
    public static void close(AutoCloseable... closeables) {
        DBUtil.close(closeables);
    }
} 