package dao;

import model.Transaction;
import model.Transaction.TransactionType;
import util.DBConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 好友转账数据访问对象
 */
public class TransferDAO {
    private static final Logger LOGGER = Logger.getLogger(TransferDAO.class.getName());
    private final StudentDAO studentDAO = new StudentDAO();
    private final TransactionDAO transactionDAO = new TransactionDAO();
    private final FriendshipDAO friendshipDAO = new FriendshipDAO();

    /**
     * 执行好友转账
     * @param senderId 发送者ID
     * @param receiverId 接收者ID
     * @param amount 转账金额
     * @param description 转账描述
     * @return 转账是否成功
     */
    public boolean transfer(String senderId, String receiverId, BigDecimal amount, String description) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        boolean success = false;
        
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false); // 开启事务
            
            // 检查是否是好友关系
            if (!friendshipDAO.isFriend(senderId, receiverId)) {
                LOGGER.warning("转账失败: " + senderId + " -> " + receiverId + " 不是好友关系");
                return false;
            }
            
            // 检查发送者余额是否足够
            double senderBalance = studentDAO.findById(senderId).getBalance();
            if (senderBalance < amount.doubleValue()) {
                LOGGER.warning("转账失败: " + senderId + " 余额不足");
                return false;
            }
            
            // 扣除发送者余额
            double newSenderBalance = senderBalance - amount.doubleValue();
            boolean updateSender = studentDAO.updateBalance(senderId, newSenderBalance);
            
            if (!updateSender) {
                conn.rollback();
                LOGGER.warning("转账失败: 无法更新发送者余额");
                return false;
            }
            
            // 增加接收者余额
            double receiverBalance = studentDAO.findById(receiverId).getBalance();
            double newReceiverBalance = receiverBalance + amount.doubleValue();
            boolean updateReceiver = studentDAO.updateBalance(receiverId, newReceiverBalance);
            
            if (!updateReceiver) {
                conn.rollback();
                LOGGER.warning("转账失败: 无法更新接收者余额");
                return false;
            }
            
            // 记录发送者交易
            Transaction senderTransaction = new Transaction();
            senderTransaction.setStudentId(senderId);
            senderTransaction.setType(TransactionType.TRANSFER);
            senderTransaction.setAmount(amount.negate());
            senderTransaction.setDescription(description + " (转给: " + receiverId + ")");
            senderTransaction.setRelatedUserId(receiverId);
            
            boolean addSenderTransaction = transactionDAO.add(senderTransaction);
            
            if (!addSenderTransaction) {
                conn.rollback();
                LOGGER.warning("转账失败: 无法记录发送者交易");
                return false;
            }
            
            // 记录接收者交易
            Transaction receiverTransaction = new Transaction();
            receiverTransaction.setStudentId(receiverId);
            receiverTransaction.setType(TransactionType.TRANSFER);
            receiverTransaction.setAmount(amount);
            receiverTransaction.setDescription(description + " (来自: " + senderId + ")");
            receiverTransaction.setRelatedUserId(senderId);
            
            boolean addReceiverTransaction = transactionDAO.add(receiverTransaction);
            
            if (!addReceiverTransaction) {
                conn.rollback();
                LOGGER.warning("转账失败: 无法记录接收者交易");
                return false;
            }
            
            // 提交事务
            conn.commit();
            success = true;
            LOGGER.info("转账成功: " + senderId + " -> " + receiverId + ", 金额: " + amount);
            
        } catch (SQLException e) {
            try {
                if (conn != null) {
                    conn.rollback();
                }
            } catch (SQLException ex) {
                LOGGER.log(Level.SEVERE, "回滚事务失败", ex);
            }
            LOGGER.log(Level.SEVERE, "转账过程中发生错误", e);
        } finally {
            try {
                if (conn != null) {
                    conn.setAutoCommit(true);
                }
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "重置自动提交失败", e);
            }
            DBConnection.close(conn, pstmt, rs);
        }
        
        return success;
    }
} 