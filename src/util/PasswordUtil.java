package util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 密码工具类，用于密码的加密和验证
 */
public class PasswordUtil {
    
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
} 