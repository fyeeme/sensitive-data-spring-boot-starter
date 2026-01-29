package com.example.sensitive.integration;

import com.example.sensitive.support.SensitiveEntity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 日志脱敏集成测试
 * <p>
 * 验证 API 脱敏功能的引入不影响现有的日志脱敏功能
 */
class LogDesensitizationIntegrationTest {

    /**
     * 测试用的 DTO 类 - 继承 SensitiveEntity
     */
    static class TestUserDto extends SensitiveEntity {
        @com.example.sensitive.annotation.Sensitive(
            type = com.example.sensitive.enums.SensitiveType.PHONE,
            forApi = true
        )
        private String phone = "13812345678";

        @com.example.sensitive.annotation.Sensitive(
            type = com.example.sensitive.enums.SensitiveType.ID_CARD,
            forApi = false
        )
        private String idCard = "110101199001011234";

        @com.example.sensitive.annotation.Sensitive(
            type = com.example.sensitive.enums.SensitiveType.EMAIL
        ) // 默认 forApi = false
        private String email = "test@example.com";

        public String getPhone() { return phone; }
        public String getIdCard() { return idCard; }
        public String getEmail() { return email; }
    }

    @Test
    void testLogDesensitizationStillWorks() {
        // 测试日志脱敏功能仍然正常工作
        TestUserDto user = new TestUserDto();

        String toString = user.toString();

        System.out.println("日志输出 (toString):");
        System.out.println(toString);

        // 日志中所有敏感字段都应该脱敏(无论 forApi 是 true 还是 false)
        assertTrue(toString.contains("138****5678"), "phone 应该在日志中脱敏");
        assertTrue(toString.contains("110101********1234"), "idCard 应该在日志中脱敏");
        assertTrue(toString.contains("t***@example.com"), "email 应该在日志中脱敏");

        // 日志中不应该包含完整数据
        assertFalse(toString.contains("13812345678"), "日志中不应该包含完整手机号");
        assertFalse(toString.contains("110101199001011234"), "日志中不应该包含完整身份证");
        assertFalse(toString.contains("test@example.com"), "日志中不应该包含完整邮箱");
    }

    @Test
    void testToStringWithAllForApiTrue() {
        // 测试即使所有字段都设置 forApi=true,日志仍然脱敏
        class AllForApiTrueDto extends SensitiveEntity {
            @com.example.sensitive.annotation.Sensitive(
                type = com.example.sensitive.enums.SensitiveType.PHONE,
                forApi = true
            )
            private String phone = "13812345678";

            @com.example.sensitive.annotation.Sensitive(
                type = com.example.sensitive.enums.SensitiveType.NAME,
                forApi = true
            )
            private String name = "张三丰";

            public String getPhone() { return phone; }
            public String getName() { return name; }
        }

        AllForApiTrueDto dto = new AllForApiTrueDto();
        String toString = dto.toString();

        System.out.println("所有字段 forApi=true 时的日志输出:");
        System.out.println(toString);

        // 日志中仍然应该脱敏
        assertTrue(toString.contains("138****5678"), "phone 应该在日志中脱敏");
        assertTrue(toString.contains("张*丰"), "name 应该在日志中脱敏");
    }

    @Test
    void testToStringWithAllForApiFalse() {
        // 测试所有字段都设置 forApi=false,日志也脱敏
        class AllForApiFalseDto extends SensitiveEntity {
            @com.example.sensitive.annotation.Sensitive(
                type = com.example.sensitive.enums.SensitiveType.PHONE,
                forApi = false
            )
            private String phone = "13812345678";

            @com.example.sensitive.annotation.Sensitive(
                type = com.example.sensitive.enums.SensitiveType.NAME,
                forApi = false
            )
            private String name = "张三丰";

            public String getPhone() { return phone; }
            public String getName() { return name; }
        }

        AllForApiFalseDto dto = new AllForApiFalseDto();
        String toString = dto.toString();

        System.out.println("所有字段 forApi=false 时的日志输出:");
        System.out.println(toString);

        // 日志中仍然应该脱敏
        assertTrue(toString.contains("138****5678"), "phone 应该在日志中脱敏");
        assertTrue(toString.contains("张*丰"), "name 应该在日志中脱敏");
    }

    @Test
    void testForApiDoesNotAffectLogDesensitization() {
        // 测试 forApi 属性不影响日志脱敏
        class ComparisonDto extends SensitiveEntity {
            @com.example.sensitive.annotation.Sensitive(
                type = com.example.sensitive.enums.SensitiveType.PHONE,
                forApi = true
            )
            private String phoneWithForApi = "13812345678";

            @com.example.sensitive.annotation.Sensitive(
                type = com.example.sensitive.enums.SensitiveType.PHONE,
                forApi = false
            )
            private String phoneWithoutForApi = "13987654321";

            public String getPhoneWithForApi() { return phoneWithForApi; }
            public String getPhoneWithoutForApi() { return phoneWithoutForApi; }
        }

        ComparisonDto dto = new ComparisonDto();
        String toString = dto.toString();

        System.out.println("forApi 对日志脱敏的影响:");
        System.out.println(toString);

        // 两个字段在日志中都应该脱敏
        assertTrue(toString.contains("138****5678"), "forApi=true 的字段应该在日志中脱敏");
        assertTrue(toString.contains("139****4321"), "forApi=false 的字段也应该在日志中脱敏");
    }

    @Test
    void testSensitiveEntityStillFunctional() {
        // 测试 SensitiveEntity 的功能没有受影响
        TestUserDto user = new TestUserDto();

        // toString 应该正常工作
        String toString = user.toString();
        assertNotNull(toString, "toString 不应该返回 null");
        assertFalse(toString.isEmpty(), "toString 不应该返回空字符串");

        // 应该包含类名
        assertTrue(toString.contains("TestUserDto"), "toString 应该包含类名");

        // 应该包含脱敏后的字段
        assertTrue(toString.contains("138****5678"), "应该包含脱敏后的 phone");
    }

    @Test
    void testLogAndApiDesensitizationIndependence() {
        // 测试日志脱敏和 API 脱敏的独立性
        TestUserDto user = new TestUserDto();

        // 1. 日志脱敏: 所有字段都脱敏
        String logOutput = user.toString();
        assertTrue(logOutput.contains("138****5678"), "日志中 phone 脱敏");
        assertTrue(logOutput.contains("110101********1234"), "日志中 idCard 脱敏");
        assertTrue(logOutput.contains("t***@example.com"), "日志中 email 脱敏");

        // 2. API 脱敏: 只有 forApi=true 的字段脱敏
        try {
            String apiOutput = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(user);
            System.out.println("API 输出:");
            System.out.println(apiOutput);

            // phone: forApi=true,应该脱敏
            assertTrue(apiOutput.contains("138****5678"), "API 中 phone 应该脱敏");

            // idCard 和 email: forApi=false,不应该脱敏
            assertTrue(apiOutput.contains("110101199001011234"), "API 中 idCard 不应该脱敏");
            assertTrue(apiOutput.contains("test@example.com"), "API 中 email 不应该脱敏");

        } catch (Exception e) {
            fail("API 序列化失败: " + e.getMessage());
        }
    }

    @Test
    void testToStringFormatConsistency() {
        // 测试 toString 格式的一致性
        TestUserDto user1 = new TestUserDto();
        TestUserDto user2 = new TestUserDto();

        String toString1 = user1.toString();
        String toString2 = user2.toString();

        // 相同对象的 toString 输出应该一致
        assertEquals(toString1, toString2, "相同对象的 toString 应该一致");

        // 验证格式: ClassName(field1=value1, field2=value2, ...)
        assertTrue(toString1.matches("TestUserDto\\([^)]+\\)"), "toString 格式应该正确");
    }
}
