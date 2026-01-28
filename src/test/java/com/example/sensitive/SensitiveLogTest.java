package com.example.sensitive;

import com.example.sensitive.annotation.Sensitive;
import com.example.sensitive.config.SensitiveLogProperties.MaskMode;
import com.example.sensitive.enums.SensitiveType;
import com.example.sensitive.jackson.SensitiveModule;
import com.example.sensitive.strategy.MaskStrategyFactory;
import com.example.sensitive.support.SensitiveEntity;
import com.example.sensitive.support.SensitiveToStringSupport;
import com.example.sensitive.util.SensitiveLogUtils;
import com.example.sensitive.util.SensitiveToStringBuilder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 敏感日志脱敏测试
 */
class SensitiveLogTest {
    
    private static ObjectMapper sensitiveMapper;
    
    @BeforeAll
    static void setup() {
        sensitiveMapper = new ObjectMapper();
        sensitiveMapper.registerModule(new JavaTimeModule());
        sensitiveMapper.registerModule(new SensitiveModule());
        sensitiveMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        sensitiveMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        
        // 初始化日志工具类
        SensitiveLogUtils.init(sensitiveMapper, MaskMode.BOTH);
    }
    
    // ==================== 策略测试 ====================
    
    @Test
    @DisplayName("手机号脱敏: 13812345678 -> 138****5678")
    void testPhoneMask() {
        String masked = MaskStrategyFactory.mask("13812345678", SensitiveType.PHONE);
        assertEquals("138****5678", masked);
    }
    
    @Test
    @DisplayName("身份证脱敏: 110101199001011234 -> 110101********1234")
    void testIdCardMask() {
        String masked = MaskStrategyFactory.mask("110101199001011234", SensitiveType.ID_CARD);
        assertEquals("110101********1234", masked);
    }
    
    @Test
    @DisplayName("银行卡脱敏: 6222021234567890123 -> 6222***********0123")
    void testBankCardMask() {
        String masked = MaskStrategyFactory.mask("6222021234567890123", SensitiveType.BANK_CARD);
        assertEquals("6222***********0123", masked);
    }
    
    @Test
    @DisplayName("邮箱脱敏: test@example.com -> t***@example.com")
    void testEmailMask() {
        String masked = MaskStrategyFactory.mask("test@example.com", SensitiveType.EMAIL);
        assertEquals("t***@example.com", masked);
    }
    
    @Test
    @DisplayName("姓名脱敏: 张三 -> 张*, 张三丰 -> 张*丰")
    void testNameMask() {
        assertEquals("张*", MaskStrategyFactory.mask("张三", SensitiveType.NAME));
        assertEquals("张*丰", MaskStrategyFactory.mask("张三丰", SensitiveType.NAME));
    }
    
    @Test
    @DisplayName("地址脱敏: 北京市朝阳区望京SOHO -> 北京市朝阳区***")
    void testAddressMask() {
        String masked = MaskStrategyFactory.mask("北京市朝阳区望京SOHO", SensitiveType.ADDRESS);
        assertEquals("北京市朝阳区***", masked);
    }
    
    @Test
    @DisplayName("自定义脱敏: 保留前2后3")
    void testCustomMask() {
        String masked = MaskStrategyFactory.maskCustom("1234567890", 2, 3, '*');
        assertEquals("12*****890", masked);
    }
    
    // ==================== Jackson 序列化测试 ====================
    
    @Test
    @DisplayName("Jackson 序列化脱敏")
    void testJacksonSerialization() throws JsonProcessingException {
        UserDTO user = createTestUser();
        
        String json = sensitiveMapper.writeValueAsString(user);
        
        System.out.println("Masked JSON: " + json);
        
        // 验证脱敏结果
        assertTrue(json.contains("138****5678"));
        assertTrue(json.contains("110101********1234"));
        assertTrue(json.contains("t***@example.com"));
        assertTrue(json.contains("6222***********0123"));
        assertTrue(json.contains("张*丰"));
        assertTrue(json.contains("******"));  // 密码
        
        // 原始数据不应该出现
        assertFalse(json.contains("13812345678"));
        assertFalse(json.contains("110101199001011234"));
        assertFalse(json.contains("secret123"));
    }
    
    // ==================== SensitiveEntity 测试 ====================
    
    @Test
    @DisplayName("SensitiveEntity 继承方式脱敏")
    void testSensitiveEntity() {
        UserEntityDTO user = new UserEntityDTO();
        user.setId(1L);
        user.setPhone("13812345678");
        user.setIdCard("110101199001011234");
        user.setRealName("张三丰");
        
        String result = user.toString();
        System.out.println("SensitiveEntity.toString: " + result);
        
        // 验证脱敏
        assertTrue(result.contains("138****5678"));
        assertTrue(result.contains("110101********1234"));
        assertTrue(result.contains("张*丰"));
        
        // 原始数据不应该出现
        assertFalse(result.contains("13812345678"));
        assertFalse(result.contains("110101199001011234"));
    }
    
    @Test
    @DisplayName("SensitiveEntity 不影响 Jackson 序列化（模拟 API 返回）")
    void testSensitiveEntityNotAffectJackson() throws JsonProcessingException {
        UserEntityDTO user = new UserEntityDTO();
        user.setId(1L);
        user.setPhone("13812345678");
        
        // 使用普通 ObjectMapper（模拟 Spring MVC 默认行为）
        ObjectMapper normalMapper = new ObjectMapper();
        String json = normalMapper.writeValueAsString(user);
        
        System.out.println("Normal Jackson (API return): " + json);
        
        // API 返回应该包含完整数据
        assertTrue(json.contains("13812345678"));
    }
    
    // ==================== SensitiveToStringSupport 接口测试 ====================
    
    @Test
    @DisplayName("SensitiveToStringSupport 接口方式脱敏")
    void testSensitiveToStringSupport() {
        UserInterfaceDTO user = new UserInterfaceDTO();
        user.setId(1L);
        user.setPhone("13812345678");
        user.setEmail("test@example.com");
        
        String result = user.toString();
        System.out.println("SensitiveToStringSupport.toString: " + result);
        
        // 验证脱敏
        assertTrue(result.contains("138****5678"));
        assertTrue(result.contains("t***@example.com"));
    }
    
    // ==================== SensitiveLogUtils 测试 ====================
    
    @Test
    @DisplayName("SensitiveLogUtils.toJson 脱敏")
    void testSensitiveLogUtils() {
        UserDTO user = createTestUser();
        
        String json = SensitiveLogUtils.toJson(user);
        System.out.println("SensitiveLogUtils.toJson: " + json);
        
        assertTrue(json.contains("138****5678"));
        assertFalse(json.contains("13812345678"));
    }
    
    @Test
    @DisplayName("SensitiveLogUtils.toRawJson 不脱敏")
    void testSensitiveLogUtilsRaw() {
        UserDTO user = createTestUser();
        
        String json = SensitiveLogUtils.toRawJson(user);
        System.out.println("SensitiveLogUtils.toRawJson: " + json);
        
        // 原始数据应该存在
        assertTrue(json.contains("13812345678"));
    }
    
    @Test
    @DisplayName("SensitiveLogUtils.mask 统一入口")
    void testSensitiveLogUtilsMask() {
        UserEntityDTO user = new UserEntityDTO();
        user.setId(1L);
        user.setPhone("13812345678");
        
        String result = SensitiveLogUtils.mask(user);
        System.out.println("SensitiveLogUtils.mask: " + result);
        
        // 应该调用 toString() 因为有自定义实现
        assertTrue(result.contains("138****5678"));
    }
    
    @Test
    @DisplayName("模式切换测试")
    void testModeSwitch() {
        UserDTO user = createTestUser();
        
        // Jackson 模式
        SensitiveLogUtils.setMode(MaskMode.JACKSON);
        String jsonResult = SensitiveLogUtils.mask(user);
        assertTrue(jsonResult.startsWith("{"));  // JSON 格式
        
        // toString 模式
        SensitiveLogUtils.setMode(MaskMode.TO_STRING);
        String toStringResult = SensitiveLogUtils.mask(user);
        assertTrue(toStringResult.contains("UserDTO"));  // toString 格式
        
        // 恢复默认
        SensitiveLogUtils.setMode(MaskMode.BOTH);
    }
    
    // ==================== SensitiveToStringBuilder 测试 ====================
    
    @Test
    @DisplayName("SensitiveToStringBuilder 脱敏")
    void testSensitiveToStringBuilder() {
        UserDTO user = createTestUser();
        
        String result = SensitiveToStringBuilder.build(user);
        System.out.println("SensitiveToStringBuilder.build: " + result);
        
        assertTrue(result.contains("138****5678"));
        assertTrue(result.contains("张*丰"));
        assertFalse(result.contains("13812345678"));
    }
    
    @Test
    @DisplayName("SensitiveToStringBuilder 选择字段")
    void testSensitiveToStringBuilderWithFields() {
        UserDTO user = createTestUser();
        
        String result = SensitiveToStringBuilder.buildWith(user, "id", "phone", "realName");
        System.out.println("buildWith: " + result);
        
        assertTrue(result.contains("id"));
        assertTrue(result.contains("phone"));
        assertTrue(result.contains("realName"));
        assertFalse(result.contains("email"));
    }
    
    @Test
    @DisplayName("SensitiveToStringBuilder 排除字段")
    void testSensitiveToStringBuilderWithoutFields() {
        UserDTO user = createTestUser();
        
        String result = SensitiveToStringBuilder.buildWithout(user, "password", "bankCard");
        System.out.println("buildWithout: " + result);
        
        assertFalse(result.contains("password"));
        assertFalse(result.contains("bankCard"));
        assertTrue(result.contains("phone"));
    }
    
    // ==================== 性能测试 ====================
    // 注意: 性能测试已迁移至 JMH (Java Microbenchmark Harness)
    //
    // 迁移原因:
    // 1. JMH 能正确处理 JIT 编译优化,避免死代码消除
    // 2. 自动预热机制,确保测试在稳定状态下进行
    // 3. 提供更详细的统计分析(平均值、标准差、置信区间等)
    // 4. 避免手动计时的常见陷阱
    //
    // 运行方式:
    // - 直接运行: 运行 SensitiveLogBenchmark 类的 main 方法
    // - Maven: mvn clean test -Dbenchmark=SensitiveLogBenchmark
    // - 命令行参数: java -jar ... jmh -rf json -rff results.json
    //
    // @see SensitiveLogBenchmark

    // ==================== 辅助方法 ====================
    
    private UserDTO createTestUser() {
        UserDTO user = new UserDTO();
        user.setId(1L);
        user.setUsername("testuser");
        user.setPhone("13812345678");
        user.setIdCard("110101199001011234");
        user.setEmail("test@example.com");
        user.setBankCard("6222021234567890123");
        user.setRealName("张三丰");
        user.setPassword("secret123");
        user.setCreateTime(LocalDateTime.now());
        return user;
    }
    
    // ==================== 测试 DTO ====================
    
    /**
     * 测试用 DTO（普通类）
     */
    public static class UserDTO {
        
        private Long id;
        private String username;
        
        @Sensitive(type = SensitiveType.PHONE)
        private String phone;
        
        @Sensitive(type = SensitiveType.ID_CARD)
        private String idCard;
        
        @Sensitive(type = SensitiveType.EMAIL)
        private String email;
        
        @Sensitive(type = SensitiveType.BANK_CARD)
        private String bankCard;
        
        @Sensitive(type = SensitiveType.NAME)
        private String realName;
        
        @Sensitive(type = SensitiveType.PASSWORD)
        private String password;
        
        private LocalDateTime createTime;
        
        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getIdCard() { return idCard; }
        public void setIdCard(String idCard) { this.idCard = idCard; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getBankCard() { return bankCard; }
        public void setBankCard(String bankCard) { this.bankCard = bankCard; }
        public String getRealName() { return realName; }
        public void setRealName(String realName) { this.realName = realName; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public LocalDateTime getCreateTime() { return createTime; }
        public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
        
        @Override
        public String toString() {
            return SensitiveToStringBuilder.build(this);
        }
    }
    
    /**
     * 测试用 DTO（继承 SensitiveEntity）
     */
    public static class UserEntityDTO extends SensitiveEntity {
        
        private Long id;
        
        @Sensitive(type = SensitiveType.PHONE)
        private String phone;
        
        @Sensitive(type = SensitiveType.ID_CARD)
        private String idCard;
        
        @Sensitive(type = SensitiveType.NAME)
        private String realName;
        
        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getIdCard() { return idCard; }
        public void setIdCard(String idCard) { this.idCard = idCard; }
        public String getRealName() { return realName; }
        public void setRealName(String realName) { this.realName = realName; }
    }
    
    /**
     * 测试用 DTO（实现 SensitiveToStringSupport 接口）
     */
    public static class UserInterfaceDTO implements SensitiveToStringSupport {
        
        private Long id;
        
        @Sensitive(type = SensitiveType.PHONE)
        private String phone;
        
        @Sensitive(type = SensitiveType.EMAIL)
        private String email;
        
        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        
        @Override
        public String toString() {
            return toSensitiveString();  // 调用接口默认方法
        }
    }
}
