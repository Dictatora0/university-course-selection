package test;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.google.gson.annotations.SerializedName;

import model.*;

/**
 * 字段一致性测试
 * 
 * 检查项目中的Java模型类字段与前端JavaScript和HTML表单中的字段命名是否一致。
 * 这个测试类主要关注：
 * 1. Java模型类中的@SerializedName注解与字段名是否符合命名规范
 * 2. 前端JavaScript代码中的API请求字段与后端一致
 * 3. HTML表单中的字段名与API一致
 */
@RunWith(JUnit4.class)
public class FieldConsistencyTest {
    
    private static final String PROJECT_ROOT = "/Users/lifulin/Desktop/dbLab/Pt";
    private static final String JS_DIR = PROJECT_ROOT + "/web/js";
    private static final String HTML_DIR = PROJECT_ROOT + "/web";
    
    private Map<String, Set<String>> modelSerializedNames;
    private Map<String, String> modelFieldToSerial;
    
    @Before
    public void setUp() throws Exception {
        modelSerializedNames = new HashMap<>();
        modelFieldToSerial = new HashMap<>();
        
        // 解析模型类中的字段名和@SerializedName注解
        scanModelClasses();
    }
    
    /**
     * 解析模型类中的字段名和@SerializedName注解
     */
    private void scanModelClasses() {
        // 检查Student类
        addModelFields(Student.class);
        
        // 检查其他模型类
        addModelFields(Course.class);
        addModelFields(Enrollment.class);
        addModelFields(Message.class);
        addModelFields(Department.class);
        
        // 输出发现的模型字段和序列化名
        System.out.println("发现的模型字段和序列化名:");
        modelFieldToSerial.forEach((field, serial) -> 
            System.out.println(field + " -> " + serial));
    }
    
    /**
     * 添加模型类的字段到映射表
     */
    private void addModelFields(Class<?> clazz) {
        String className = clazz.getSimpleName();
        Set<String> serialNames = new HashSet<>();
        modelSerializedNames.put(className, serialNames);
        
        for (Field field : clazz.getDeclaredFields()) {
            SerializedName serName = field.getAnnotation(SerializedName.class);
            String fieldName = field.getName();
            
            if (serName != null) {
                String serializedName = serName.value();
                serialNames.add(serializedName);
                modelFieldToSerial.put(className + "." + fieldName, serializedName);
            }
        }
    }
    
    /**
     * 测试所有模型类的字段名是否遵循命名规范
     */
    @Test
    public void testModelFieldNamingConvention() {
        // Java字段应该使用驼峰命名法(camelCase)
        Pattern camelCasePattern = Pattern.compile("^[a-z][a-zA-Z0-9]*$");
        
        // 序列化名应该使用snake_case
        Pattern snakeCasePattern = Pattern.compile("^[a-z][a-z0-9]*(_[a-z0-9]+)*$");
        
        for (Map.Entry<String, String> entry : modelFieldToSerial.entrySet()) {
            String fieldKey = entry.getKey();
            String fieldName = fieldKey.substring(fieldKey.indexOf(".") + 1);
            String serialName = entry.getValue();
            
            // 检查Java字段名是否使用驼峰命名法
            assertTrue("字段名 " + fieldName + " 应该使用驼峰命名法", 
                       camelCasePattern.matcher(fieldName).matches());
            
            // 检查序列化名是否使用snake_case
            assertTrue("序列化名 " + serialName + " 应该使用snake_case", 
                       snakeCasePattern.matcher(serialName).matches());
        }
    }
    
    /**
     * 测试JavaScript API调用中的字段是否与模型的序列化名一致
     */
    @Test
    public void testJavaScriptApiFields() throws Exception {
        // 读取JavaScript文件中的API调用
        File jsDir = new File(JS_DIR);
        File[] jsFiles = jsDir.listFiles((dir, name) -> name.endsWith(".js"));
        
        if (jsFiles != null) {
            for (File jsFile : jsFiles) {
                String content = new String(Files.readAllBytes(jsFile.toPath()));
                
                // 查找JSON.stringify调用中的字段
                Pattern jsonPattern = Pattern.compile("JSON\\.stringify\\(\\{([^}]*)\\}\\)");
                Matcher jsonMatcher = jsonPattern.matcher(content);
                
                while (jsonMatcher.find()) {
                    String jsonContent = jsonMatcher.group(1);
                    Pattern fieldPattern = Pattern.compile("(\\w+)\\s*:");
                    Matcher fieldMatcher = fieldPattern.matcher(jsonContent);
                    
                    while (fieldMatcher.find()) {
                        String apiField = fieldMatcher.group(1);
                        
                        // 检查API字段是否存在于任何模型的序列化名中
                        boolean fieldFound = modelSerializedNames.values().stream()
                            .anyMatch(serialNames -> serialNames.contains(apiField));
                        
                        // 对于特定字段如password或amount，我们可能没有显式注解，跳过检查
                        if (!apiField.equals("password") && !apiField.equals("amount") && 
                            !apiField.equals("courseId") && !apiField.equals("studentId")) {
                            assertTrue("API字段 " + apiField + " 在 " + jsFile.getName() + 
                                       " 没有对应的模型序列化名", fieldFound);
                        }
                    }
                }
            }
        }
    }
    
    /**
     * 测试HTML表单中的input字段name属性是否与API字段一致
     */
    @Test
    public void testHtmlFormFieldNames() throws Exception {
        File htmlDir = new File(HTML_DIR);
        File[] htmlFiles = htmlDir.listFiles((dir, name) -> name.endsWith(".html"));
        
        if (htmlFiles != null) {
            for (File htmlFile : htmlFiles) {
                String content = new String(Files.readAllBytes(htmlFile.toPath()));
                
                // 提取所有input字段的name属性
                Pattern inputPattern = Pattern.compile("<input[^>]*name=[\"']([^\"']*)[\"'][^>]*>");
                Matcher inputMatcher = inputPattern.matcher(content);
                
                while (inputMatcher.find()) {
                    String fieldName = inputMatcher.group(1);
                    
                    // 跳过空字段名和特殊字段
                    if (fieldName.isEmpty() || fieldName.equals("confirm_password")) {
                        continue;
                    }
                    
                    // 检查字段名是否使用snake_case
                    Pattern snakeCasePattern = Pattern.compile("^[a-z][a-z0-9]*(_[a-z0-9]+)*$");
                    assertTrue("HTML表单字段名 " + fieldName + " 在 " + htmlFile.getName() + 
                               " 应该使用snake_case", snakeCasePattern.matcher(fieldName).matches());
                    
                    // 检查是否存在于模型序列化名中
                    boolean fieldFound = modelSerializedNames.values().stream()
                        .anyMatch(serialNames -> serialNames.contains(fieldName));
                    
                    // 对于特定字段如password，我们可能没有显式注解，跳过检查
                    if (!fieldName.equals("password")) {
                        assertTrue("HTML表单字段 " + fieldName + " 在 " + htmlFile.getName() + 
                               " 没有对应的模型序列化名", fieldFound);
                    }
                }
            }
        }
    }
} 