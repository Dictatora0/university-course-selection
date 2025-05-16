package dao;

import model.Transaction;
import util.DBConnection;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 交易数据访问对象，处理与Transaction表相关的数据库操作
 */
public class TransactionDAO {
    
    /**
     * 创建交易记录
     */
    public boolean add(Transaction transaction) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        boolean success = false;
        
        try {
            conn = DBConnection.getConnection();
            String sql = "INSERT INTO Transaction (from_student_id, to_student_id, amount, transaction_time) " +
                    "VALUES (?, ?, ?, ?)";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, transaction.getFromStudentId());
            pstmt.setString(2, transaction.getToStudentId());
            pstmt.setBigDecimal(3, transaction.getAmount());
            
            if (transaction.getTransactionTime() == null) {
                pstmt.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
            } else {
                pstmt.setTimestamp(4, new Timestamp(transaction.getTransactionTime().getTime()));
            }
            
            int rowsAffected = pstmt.executeUpdate();
            success = rowsAffected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBConnection.close(conn, pstmt, null);
        }
        
        return success;
    }
    
    /**
     * 查询学生的转账记录（作为发送方或接收方）
     */
    public List<Transaction> findByStudentId(String studentId) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        List<Transaction> transactions = new ArrayList<>();
        
        try {
            conn = DBConnection.getConnection();
            String sql = "SELECT t.*, s1.name as from_name, s2.name as to_name FROM Transaction t " +
                    "JOIN Student s1 ON t.from_student_id = s1.student_id " +
                    "JOIN Student s2 ON t.to_student_id = s2.student_id " +
                    "WHERE t.from_student_id = ? OR t.to_student_id = ? " +
                    "ORDER BY t.transaction_time DESC";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, studentId);
            pstmt.setString(2, studentId);
            rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Transaction transaction = new Transaction();
                transaction.setTransactionId(rs.getLong("transaction_id"));
                transaction.setFromStudentId(rs.getString("from_student_id"));
                transaction.setToStudentId(rs.getString("to_student_id"));
                transaction.setAmount(rs.getBigDecimal("amount"));
                transaction.setTransactionTime(rs.getTimestamp("transaction_time"));
                transaction.setFromStudentName(rs.getString("from_name"));
                transaction.setToStudentName(rs.getString("to_name"));
                transactions.add(transaction);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBConnection.close(conn, pstmt, rs);
        }
        
        return transactions;
    }
    
    /**
     * 查询两个学生之间的交易记录
     */
    public List<Transaction> findBetweenStudents(String studentId1, String studentId2) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        List<Transaction> transactions = new ArrayList<>();
        
        try {
            conn = DBConnection.getConnection();
            String sql = "SELECT t.*, s1.name as from_name, s2.name as to_name FROM Transaction t " +
                    "JOIN Student s1 ON t.from_student_id = s1.student_id " +
                    "JOIN Student s2 ON t.to_student_id = s2.student_id " +
                    "WHERE (t.from_student_id = ? AND t.to_student_id = ?) " +
                    "OR (t.from_student_id = ? AND t.to_student_id = ?) " +
                    "ORDER BY t.transaction_time DESC";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, studentId1);
            pstmt.setString(2, studentId2);
            pstmt.setString(3, studentId2);
            pstmt.setString(4, studentId1);
            rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Transaction transaction = new Transaction();
                transaction.setTransactionId(rs.getLong("transaction_id"));
                transaction.setFromStudentId(rs.getString("from_student_id"));
                transaction.setToStudentId(rs.getString("to_student_id"));
                transaction.setAmount(rs.getBigDecimal("amount"));
                transaction.setTransactionTime(rs.getTimestamp("transaction_time"));
                transaction.setFromStudentName(rs.getString("from_name"));
                transaction.setToStudentName(rs.getString("to_name"));
                transactions.add(transaction);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBConnection.close(conn, pstmt, rs);
        }
        
        return transactions;
    }
    
    /**
     * 获取学生的交易总额
     */
    public BigDecimal getTotalAmountSent(String studentId) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        BigDecimal totalAmount = BigDecimal.ZERO;
        
        try {
            conn = DBConnection.getConnection();
            String sql = "SELECT SUM(amount) as total_amount FROM Transaction WHERE from_student_id = ?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, studentId);
            rs = pstmt.executeQuery();
            
            if (rs.next() && rs.getBigDecimal("total_amount") != null) {
                totalAmount = rs.getBigDecimal("total_amount");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBConnection.close(conn, pstmt, rs);
        }
        
        return totalAmount;
    }
    
    /**
     * 获取学生收到的交易总额
     */
    public BigDecimal getTotalAmountReceived(String studentId) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        BigDecimal totalAmount = BigDecimal.ZERO;
        
        try {
            conn = DBConnection.getConnection();
            String sql = "SELECT SUM(amount) as total_amount FROM Transaction WHERE to_student_id = ?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, studentId);
            rs = pstmt.executeQuery();
            
            if (rs.next() && rs.getBigDecimal("total_amount") != null) {
                totalAmount = rs.getBigDecimal("total_amount");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBConnection.close(conn, pstmt, rs);
        }
        
        return totalAmount;
    }
} 