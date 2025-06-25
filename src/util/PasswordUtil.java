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
            System.out.println("[PasswordUtil.checkPassword] 输入密码或哈希密码为null");
            return false;
        }
        String hashedInputPassword = hashPassword(inputPassword);
        System.out.println("[PasswordUtil.checkPassword] 输入密码哈希: " + hashedInputPassword);
        System.out.println("[PasswordUtil.checkPassword] 数据库密码哈希: " + hashedPasswordFromDB);
        boolean matches = hashedPasswordFromDB.equals(hashedInputPassword);
        System.out.println("[PasswordUtil.checkPassword] 密码匹配结果: " + matches);
        return matches;
    }
    
    /**
     * 用于测试密码哈希功能的主方法
     */
    public static void main(String[] args) {
        // 从命令行读取密码
        java.util.Scanner scanner = new java.util.Scanner(System.in);
        System.out.print("请输入要哈希的密码: ");
        String password = scanner.nextLine();
        scanner.close();
        
        // 生成哈希值
        String hashedPassword = hashPassword(password);
        System.out.println("原始密码: " + password);
        System.out.println("哈希后密码: " + hashedPassword);
        
        // 检查哈希值匹配
        System.out.println("验证结果: " + checkPassword(password, hashedPassword));
        
        // 从数据库中查询的哈希值
        String knownHashFromDB = "jZae727K08KaOmKSgOaGzww/XVqGr/PKEgIMkjrcbJI="; // 123456的哈希值
        System.out.println("与数据库中的哈希值匹配: " + checkPassword(password, knownHashFromDB));
    }
} 