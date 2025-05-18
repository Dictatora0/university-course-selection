package dao;

import model.Transaction;
import model.Student; 
import util.DBConnection; // 使用正确的数据库连接工具类

import java.sql.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TransactionDAO {

    /**
     * 添加一个新的交易记录到数据库。
     * 交易对象应已设置 studentId, type, amount, 和 description。
     * transactionDate 如果未提供，则自动设置。
     */
    public boolean add(Transaction transaction) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        CallableStatement cstmt = null;
        boolean success = false;

        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);
            
            // 首先生成交易ID
            cstmt = conn.prepareCall("{CALL GenerateTransactionId(?)}");
            cstmt.registerOutParameter(1, Types.VARCHAR);
            cstmt.execute();
            String transactionId = cstmt.getString(1);
            
            if (transactionId == null || transactionId.isEmpty()) {
                // 如果存储过程失败，使用UUID作为备选方案
                transactionId = "TRX" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
            }
            
            // 插入交易记录
            String sql = "INSERT INTO Transaction (transaction_id, student_id, related_student_id, amount, type, description) " +
                         "VALUES (?, ?, ?, ?, ?, ?)";
            
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, transactionId);
            pstmt.setString(2, transaction.getStudentId());
            pstmt.setString(3, transaction.getRelatedStudentId()); // 可以为null
            pstmt.setBigDecimal(4, transaction.getAmount());
            pstmt.setString(5, transaction.getType().name()); // 将枚举名存为字符串
            pstmt.setString(6, transaction.getDescription());

            int rowsAffected = pstmt.executeUpdate();
            
            if (rowsAffected > 0) {
                transaction.setTransactionId(transactionId);
                conn.commit();
                success = true;
            } else {
                conn.rollback();
            }
        } catch (SQLException e) {
            try {
                if (conn != null) conn.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
        } finally {
            try { if (cstmt != null) cstmt.close(); } catch (SQLException e) { e.printStackTrace(); }
            try { if (pstmt != null) pstmt.close(); } catch (SQLException e) { e.printStackTrace(); }
            try { 
                if (conn != null) {
                    conn.setAutoCommit(true);
                    conn.close(); 
                } 
            } catch (SQLException e) { e.printStackTrace(); }
        }
        return success;
    }

    /**
     * 根据 studentId 查找所有交易记录，按日期降序排列。
     * 通过 JOIN 填充 studentName 和 relatedStudentName。
     */
    public List<Transaction> findByStudentId(String studentId) {
        List<Transaction> transactions = new ArrayList<>();
        String sql = "SELECT t.*, s.name as student_name, rs.name as related_student_name " +
                     "FROM Transaction t " +
                     "JOIN Student s ON t.student_id = s.student_id " +
                     "LEFT JOIN Student rs ON t.related_student_id = rs.student_id " +
                     "WHERE t.student_id = ? ORDER BY t.transaction_time DESC";
        
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DBConnection.getConnection(); 
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, studentId);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                Transaction transaction = new Transaction();
                transaction.setTransactionId(rs.getString("transaction_id"));
                transaction.setStudentId(rs.getString("student_id"));
                
                String typeStr = rs.getString("type");
                if (typeStr != null) {
                    try {
                        transaction.setType(Transaction.TransactionType.valueOf(typeStr.toUpperCase()));
                    } catch (IllegalArgumentException e) {
                        System.err.println("数据库中存在无效的交易类型，ID: " + rs.getString("transaction_id") + ", 类型: " + typeStr);
                        continue; // 跳过此格式错误的交易
                    }
                } else {
                     System.err.println("数据库中存在空的交易类型，ID: " + rs.getString("transaction_id"));
                    continue; // 如果类型是必需的且为 null，则跳过
                }
                
                transaction.setAmount(rs.getBigDecimal("amount"));
                transaction.setTransactionDate(rs.getTimestamp("transaction_time"));
                transaction.setDescription(rs.getString("description"));
                transaction.setRelatedStudentId(rs.getString("related_student_id")); // 可能为 null
                
                // 从 JOIN 中填充姓名
                transaction.setStudentName(rs.getString("student_name"));
                transaction.setRelatedStudentName(rs.getString("related_student_name")); // 如果没有 related_student_id，则为 null
                
                transactions.add(transaction);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try { if (rs != null) rs.close(); } catch (SQLException e) { e.printStackTrace(); }
            try { if (pstmt != null) pstmt.close(); } catch (SQLException e) { e.printStackTrace(); }
            try { if (conn != null) conn.close(); } catch (SQLException e) { e.printStackTrace(); }
        }
        return transactions;
    }

    /**
     * 获取最近的交易记录
     * @param limit 返回的记录数量限制
     * @return 按交易时间排序的最近交易记录列表
     */
    public List<Transaction> getRecentTransactions(int limit) {
        List<Transaction> transactions = new ArrayList<>();
        String sql = "SELECT t.*, s.name as student_name, rs.name as related_student_name " +
                     "FROM Transaction t " +
                     "JOIN Student s ON t.student_id = s.student_id " +
                     "LEFT JOIN Student rs ON t.related_student_id = rs.student_id " +
                     "ORDER BY t.transaction_time DESC LIMIT ?";
        
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DBConnection.getConnection(); 
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, limit);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                Transaction transaction = new Transaction();
                transaction.setTransactionId(rs.getString("transaction_id"));
                transaction.setStudentId(rs.getString("student_id"));
                
                String typeStr = rs.getString("type");
                if (typeStr != null) {
                    try {
                        transaction.setType(Transaction.TransactionType.valueOf(typeStr.toUpperCase()));
                    } catch (IllegalArgumentException e) {
                        System.err.println("数据库中存在无效的交易类型，ID: " + rs.getString("transaction_id") + ", 类型: " + typeStr);
                        continue; // 跳过此格式错误的交易
                    }
                } else {
                    System.err.println("数据库中存在空的交易类型，ID: " + rs.getString("transaction_id"));
                    continue; // 如果类型是必需的且为 null，则跳过
                }
                
                transaction.setAmount(rs.getBigDecimal("amount"));
                transaction.setTransactionDate(rs.getTimestamp("transaction_time"));
                transaction.setDescription(rs.getString("description"));
                transaction.setRelatedStudentId(rs.getString("related_student_id"));
                
                // 从 JOIN 中填充姓名
                transaction.setStudentName(rs.getString("student_name"));
                transaction.setRelatedStudentName(rs.getString("related_student_name"));
                
                transactions.add(transaction);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try { if (rs != null) rs.close(); } catch (SQLException e) { e.printStackTrace(); }
            try { if (pstmt != null) pstmt.close(); } catch (SQLException e) { e.printStackTrace(); }
            try { if (conn != null) conn.close(); } catch (SQLException e) { e.printStackTrace(); }
        }
        return transactions;
    }

    /**
     * 获取指定用户当日的转账总金额
     * @param studentId 学生ID
     * @param date 日期，只考虑日期部分
     * @return 当日转账总金额
     */
    public BigDecimal getDailyTransferSum(String studentId, java.util.Date date) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        BigDecimal sum = BigDecimal.ZERO;

        try {
            conn = DBConnection.getConnection();
            String sql = "SELECT SUM(ABS(amount)) FROM Transaction " +
                        "WHERE student_id = ? AND type = 'TRANSFER' " +
                        "AND DATE(transaction_time) = DATE(?)";
            
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, studentId);
            pstmt.setTimestamp(2, new Timestamp(date.getTime()));
            
            rs = pstmt.executeQuery();
            if (rs.next()) {
                BigDecimal result = rs.getBigDecimal(1);
                if (result != null) {
                    sum = result;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try { if (rs != null) rs.close(); } catch (SQLException e) { e.printStackTrace(); }
            try { if (pstmt != null) pstmt.close(); } catch (SQLException e) { e.printStackTrace(); }
            try { if (conn != null) conn.close(); } catch (SQLException e) { e.printStackTrace(); }
        }
        return sum;
    }

    /**
     * 获取指定用户当日的转账次数
     * @param studentId 学生ID
     * @param date 日期，只考虑日期部分
     * @return 当日转账次数
     */
    public int getDailyTransferCount(String studentId, java.util.Date date) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        int count = 0;

        try {
            conn = DBConnection.getConnection();
            String sql = "SELECT COUNT(*) FROM Transaction " +
                        "WHERE student_id = ? AND type = 'TRANSFER' " +
                        "AND DATE(transaction_time) = DATE(?)";
            
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, studentId);
            pstmt.setTimestamp(2, new Timestamp(date.getTime()));
            
            rs = pstmt.executeQuery();
            if (rs.next()) {
                count = rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try { if (rs != null) rs.close(); } catch (SQLException e) { e.printStackTrace(); }
            try { if (pstmt != null) pstmt.close(); } catch (SQLException e) { e.printStackTrace(); }
            try { if (conn != null) conn.close(); } catch (SQLException e) { e.printStackTrace(); }
        }
        return count;
    }
}