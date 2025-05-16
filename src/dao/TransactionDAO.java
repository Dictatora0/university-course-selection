package dao;

import model.Transaction;
import model.Student; 
import util.DBUtil; // 确保这是您项目中正确的数据库连接工具类

import java.sql.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class TransactionDAO {

    /**
     * 添加一个新的交易记录到数据库。
     * 交易对象应已设置 studentId, type, amount, 和 description。
     * transactionDate 如果未提供，则自动设置。
     */
    public boolean add(Transaction transaction) {
        // 请确保表名 "Transactions" 和列名与您的数据库结构一致
        String sql = "INSERT INTO Transactions (student_id, type, amount, description, transaction_date, related_student_id) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        Connection conn = null;
        PreparedStatement pstmt = null;
        boolean success = false;

        try {
            conn = DBUtil.getConnection(); 
            pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

            pstmt.setString(1, transaction.getStudentId());
            pstmt.setString(2, transaction.getType().name()); // 将枚举名存为字符串
            pstmt.setBigDecimal(3, transaction.getAmount());
            pstmt.setString(4, transaction.getDescription());
            
            if (transaction.getTransactionDate() == null) {
                pstmt.setTimestamp(5, new Timestamp(System.currentTimeMillis()));
            } else {
                pstmt.setTimestamp(5, new Timestamp(transaction.getTransactionDate().getTime()));
            }
            pstmt.setString(6, transaction.getRelatedStudentId()); //可以为 null

            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        transaction.setTransactionId(generatedKeys.getLong(1));
                    }
                }
                success = true;
            }
        } catch (SQLException e) {
            e.printStackTrace(); // 考虑更健壮的日志记录
        } finally {
            // 正确关闭资源
            try { if (pstmt != null) pstmt.close(); } catch (SQLException e) { e.printStackTrace(); }
            try { if (conn != null) conn.close(); } catch (SQLException e) { e.printStackTrace(); }
        }
        return success;
    }

    /**
     * 根据 studentId 查找所有交易记录，按日期降序排列。
     * 通过 JOIN 填充 studentName 和 relatedStudentName。
     */
    public List<Transaction> findByStudentId(String studentId) {
        List<Transaction> transactions = new ArrayList<>();
        // 请确保表名 (Transactions, Student) 和列名与您的数据库结构一致
        String sql = "SELECT t.*, s.name as student_name, rs.name as related_student_name " +
                     "FROM Transactions t " +
                     "JOIN Student s ON t.student_id = s.student_id " +
                     "LEFT JOIN Student rs ON t.related_student_id = rs.student_id " + // LEFT JOIN 因为 related_student_id 可能为 null
                     "WHERE t.student_id = ? ORDER BY t.transaction_date DESC";
        
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DBUtil.getConnection(); 
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, studentId);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                Transaction transaction = new Transaction();
                transaction.setTransactionId(rs.getLong("transaction_id"));
                transaction.setStudentId(rs.getString("student_id"));
                
                String typeStr = rs.getString("type");
                if (typeStr != null) {
                    try {
                        transaction.setType(Transaction.TransactionType.valueOf(typeStr.toUpperCase()));
                    } catch (IllegalArgumentException e) {
                        System.err.println("数据库中存在无效的交易类型，ID: " + rs.getLong("transaction_id") + ", 类型: " + typeStr);
                        // 可选：设置一个默认类型或跳过此记录
                        // transaction.setType(Transaction.TransactionType.UNKNOWN); 
                        continue; // 跳过此格式错误的交易
                    }
                } else {
                     System.err.println("数据库中存在空的交易类型，ID: " + rs.getLong("transaction_id"));
                    continue; // 如果类型是必需的且为 null，则跳过
                }
                
                transaction.setAmount(rs.getBigDecimal("amount"));
                transaction.setTransactionDate(rs.getTimestamp("transaction_date"));
                transaction.setDescription(rs.getString("description"));
                transaction.setRelatedStudentId(rs.getString("related_student_id")); //可能为 null
                
                // 从 JOIN 中填充姓名
                transaction.setStudentName(rs.getString("student_name"));
                transaction.setRelatedStudentName(rs.getString("related_student_name")); // 如果没有 related_student_id，则为 null
                
                transactions.add(transaction);
            }
        } catch (SQLException e) {
            e.printStackTrace(); // 考虑更健壮的日志记录
        } finally {
            // 正确关闭资源
            try { if (rs != null) rs.close(); } catch (SQLException e) { e.printStackTrace(); }
            try { if (pstmt != null) pstmt.close(); } catch (SQLException e) { e.printStackTrace(); }
            try { if (conn != null) conn.close(); } catch (SQLException e) { e.printStackTrace(); }
        }
        return transactions;
    }
}