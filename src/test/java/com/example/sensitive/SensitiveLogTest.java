package com.example.sensitive;

import com.example.sensitive.annotation.Sensitive;
import com.example.sensitive.enums.SensitiveType;
import com.example.sensitive.strategy.MaskStrategyFactory;
import com.example.sensitive.support.SensitiveEntity;
import com.example.sensitive.support.SensitiveSupport;
import com.example.sensitive.util.SensitiveToStringBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 敏感日志脱敏测试
 */
class SensitiveLogTest {

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

    // ==================== SensitiveSupport 接口测试 ====================

    @Test
    @DisplayName("SensitiveSupport 接口方式脱敏")
    void testSensitiveToStringSupport() {
        UserInterfaceDTO user = new UserInterfaceDTO();
        user.setId(1L);
        user.setPhone("13812345678");
        user.setEmail("test@example.com");

        String result = user.toString();
        System.out.println("SensitiveSupport.toString: " + result);

        // 验证脱敏
        assertTrue(result.contains("138****5678"));
        assertTrue(result.contains("t***@example.com"));
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

        // password 字段不脱敏（仅用于测试）
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
     * 测试用 DTO（实现 SensitiveSupport 接口）
     */
    public static class UserInterfaceDTO implements SensitiveSupport {

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
