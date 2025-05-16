package util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * 密码工具类，用于密码的加密和验证
 */
public class PasswordUtil {
    
    private static final String HASH_ALGORITHM = "SHA-256";

    /**
     * 使用SHA-256算法对密码进行加密
     * @param password 原始密码
     * @return 加密后的密码字符串
     */
    public static String encryptPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            System.err.println("加密算法不可用: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 验证密码是否匹配
     * @param inputPassword 输入的密码
     * @param storedPassword 存储的加密密码
     * @return 密码是否匹配
     */
    public static boolean verifyPassword(String inputPassword, String storedPassword) {
        // 如果存储的密码是明文的（仅用于测试）
        if (storedPassword.equals(inputPassword)) {
            return true;
        }
        
        // 正常情况下，加密输入的密码并与存储的加密密码比较
        String encryptedInput = encryptPassword(inputPassword);
        return encryptedInput != null && encryptedInput.equals(storedPassword);
    }

    /**
     * 对密码进行哈希处理
     * @param password 明文密码
     * @return 哈希后的密码 (Base64编码)
     */
    public static String hashPassword(String password) {
        if (password == null || password.isEmpty()) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] hashedBytes = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashedBytes);
        } catch (NoSuchAlgorithmException e) {
            // 在实际应用中，这里应该记录严重错误，因为SHA-256是标准算法
            e.printStackTrace(); // 或者抛出一个自定义的运行时异常
            return null; // 或者抛出异常，不应返回null让程序继续，除非有特定错误处理
        }
    }

    /**
     * 验证输入的密码是否与存储的哈希密码匹配
     * @param inputPassword 用户输入的明文密码
     * @param hashedPasswordFromDB数据库中存储的哈希密码
     * @return 如果匹配则返回true，否则返回false
     */
    public static boolean checkPassword(String inputPassword, String hashedPasswordFromDB) {
        if (inputPassword == null || hashedPasswordFromDB == null) {
            return false;
        }
        String hashedInputPassword = hashPassword(inputPassword);
        return hashedPasswordFromDB.equals(hashedInputPassword);
    }
} 