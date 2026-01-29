package com.example.sensitive.integration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * API 脱敏集成测试
 * <p>
 * 使用纯 Jackson 测试,验证 API 返回值脱敏功能
 */
class ApiDesensitizationIntegrationTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    void testApiDesensitizationWithMixedForApiValues() throws JsonProcessingException {
        // 测试混合配置:部分字段 forApi=true,部分 forApi=false
        TestDto user = new TestDto(
                1L,
                "13812345678",      // phone: forApi=true,应该脱敏
                "110101199001011234", // idCard: forApi=false,不应该脱敏
                "test@example.com",  // email: 默认false,不应该脱敏
                "张三丰",            // name: forApi=true,应该脱敏
                "6222021234567890123" // bankCard: forApi=true,应该脱敏
        );

        String json = objectMapper.writeValueAsString(user);

        System.out.println("API 返回 JSON:");
        System.out.println(json);

        // 验证: forApi=true 的字段应该脱敏
        assertTrue(json.contains("138****5678"), "phone 应该脱敏");
        assertTrue(json.contains("张*丰"), "name 应该脱敏");
        assertTrue(json.contains("6222***********0123"), "bankCard 应该脱敏");

        // 验证: forApi=false 的字段不应该脱敏
        assertTrue(json.contains("110101199001011234"), "idCard 不应该脱敏");
        assertTrue(json.contains("test@example.com"), "email 不应该脱敏");

        // 验证: 完整数据不存在
        assertFalse(json.contains("13812345678"), "不应该包含完整手机号");
        assertFalse(json.contains("张三丰"), "不应该包含完整姓名");
    }

    @Test
    void testApiDesensitizationWithAllForApiTrue() throws JsonProcessingException {
        // 测试所有字段都设置 forApi=true
        class AllForApiTrueDto {
            @com.example.sensitive.annotation.Sensitive(
                type = com.example.sensitive.enums.SensitiveType.PHONE,
                forApi = true
            )
            private String phone = "13812345678";

            @com.example.sensitive.annotation.Sensitive(
                type = com.example.sensitive.enums.SensitiveType.ID_CARD,
                forApi = true
            )
            private String idCard = "110101199001011234";

            @com.example.sensitive.annotation.Sensitive(
                type = com.example.sensitive.enums.SensitiveType.EMAIL,
                forApi = true
            )
            private String email = "test@example.com";

            public String getPhone() { return phone; }
            public String getIdCard() { return idCard; }
            public String getEmail() { return email; }
        }

        AllForApiTrueDto dto = new AllForApiTrueDto();
        String json = objectMapper.writeValueAsString(dto);

        System.out.println("所有字段 forApi=true:");
        System.out.println(json);

        // 所有字段都应该脱敏
        assertTrue(json.contains("138****5678"), "phone 应该脱敏");
        assertTrue(json.contains("110101********1234"), "idCard 应该脱敏");
        assertTrue(json.contains("t***@example.com"), "email 应该脱敏");
    }

    @Test
    void testApiDesensitizationWithAllForApiFalse() throws JsonProcessingException {
        // 测试所有字段都设置 forApi=false 或省略
        class AllForApiFalseDto {
            @com.example.sensitive.annotation.Sensitive(
                type = com.example.sensitive.enums.SensitiveType.PHONE,
                forApi = false
            )
            private String phone = "13812345678";

            @com.example.sensitive.annotation.Sensitive(
                type = com.example.sensitive.enums.SensitiveType.ID_CARD
            ) // 默认 false
            private String idCard = "110101199001011234";

            public String getPhone() { return phone; }
            public String getIdCard() { return idCard; }
        }

        AllForApiFalseDto dto = new AllForApiFalseDto();
        String json = objectMapper.writeValueAsString(dto);

        System.out.println("所有字段 forApi=false:");
        System.out.println(json);

        // 所有字段都不应该脱敏,返回完整数据
        assertTrue(json.contains("13812345678"), "phone 不应该脱敏");
        assertTrue(json.contains("110101199001011234"), "idCard 不应该脱敏");

        // 不应该包含脱敏后的数据
        assertFalse(json.contains("138****5678"), "不应该包含脱敏手机号");
        assertFalse(json.contains("110101********1234"), "不应该包含脱敏身份证");
    }

    @Test
    void testDifferentSensitiveTypes() throws JsonProcessingException {
        // 测试所有内置脱敏类型
        class AllTypesDto {
            @com.example.sensitive.annotation.Sensitive(type = com.example.sensitive.enums.SensitiveType.PHONE, forApi = true)
            private String phone = "13812345678";

            @com.example.sensitive.annotation.Sensitive(type = com.example.sensitive.enums.SensitiveType.ID_CARD, forApi = true)
            private String idCard = "110101199001011234";

            @com.example.sensitive.annotation.Sensitive(type = com.example.sensitive.enums.SensitiveType.EMAIL, forApi = true)
            private String email = "test@example.com";

            @com.example.sensitive.annotation.Sensitive(type = com.example.sensitive.enums.SensitiveType.BANK_CARD, forApi = true)
            private String bankCard = "6222021234567890123";

            @com.example.sensitive.annotation.Sensitive(type = com.example.sensitive.enums.SensitiveType.NAME, forApi = true)
            private String name = "张三丰";

            @com.example.sensitive.annotation.Sensitive(type = com.example.sensitive.enums.SensitiveType.ADDRESS, forApi = true)
            private String address = "北京市朝阳区某某街道123号";

            public String getPhone() { return phone; }
            public String getIdCard() { return idCard; }
            public String getEmail() { return email; }
            public String getBankCard() { return bankCard; }
            public String getName() { return name; }
            public String getAddress() { return address; }
        }

        AllTypesDto dto = new AllTypesDto();
        String json = objectMapper.writeValueAsString(dto);

        System.out.println("所有脱敏类型:");
        System.out.println(json);

        // 验证每种类型的脱敏效果
        assertTrue(json.contains("138****5678"), "PHONE 脱敏");
        assertTrue(json.contains("110101********1234"), "ID_CARD 脱敏");
        assertTrue(json.contains("t***@example.com"), "EMAIL 脱敏");
        assertTrue(json.contains("6222***********0123"), "BANK_CARD 脱敏");
        assertTrue(json.contains("张*丰"), "NAME 脱敏");
        assertTrue(json.contains("北京市朝阳区***"), "ADDRESS 脱敏");
    }

    @Test
    void testJsonFormatIsValid() throws JsonProcessingException {
        // 测试 JSON 格式正确性
        TestDto user = new TestDto(
                1L,
                "13812345678",
                "110101199001011234",
                "test@example.com",
                "张三丰",
                "6222021234567890123"
        );

        String json = objectMapper.writeValueAsString(user);

        // 验证是有效的 JSON 格式
        assertNotNull(json);
        assertFalse(json.isEmpty());

        // 验证包含基本的 JSON 结构
        assertTrue(json.startsWith("{"), "应该以 { 开始");
        assertTrue(json.endsWith("}"), "应该以 } 结束");
        assertTrue(json.contains("\"id\":1"), "应该包含 id 字段");
        assertTrue(json.contains("\"phone\":"), "应该包含 phone 字段");
        assertTrue(json.contains("\"idCard\":"), "应该包含 idCard 字段");
    }
}
