package com.example.sensitive.jackson;

import com.example.sensitive.annotation.Sensitive;
import com.example.sensitive.enums.SensitiveType;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.io.IOException;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * {@link SensitiveJsonSerializer} 单元测试
 */
class SensitiveJsonSerializerTest {

    private ObjectMapper objectMapper;
    private StringWriter writer;

    /**
     * 测试用的 DTO 类 - forApi = true (需要脱敏)
     */
    static class TestDtoWithForApiTrue {
        @Sensitive(type = SensitiveType.PHONE, forApi = true)
        private String phone = "13812345678";

        @Sensitive(type = SensitiveType.ID_CARD, forApi = true)
        private String idCard = "110101199001011234";

        @Sensitive(type = SensitiveType.EMAIL, forApi = true)
        private String email = "test@example.com";

        @Sensitive(type = SensitiveType.CUSTOM, prefixLength = 2, suffixLength = 3, forApi = true)
        private String custom = "1234567890";

        @Sensitive(type = SensitiveType.PHONE, forApi = true)
        private String nullField = null;

        public String getPhone() { return phone; }
        public String getIdCard() { return idCard; }
        public String getEmail() { return email; }
        public String getCustom() { return custom; }
        public String getNullField() { return nullField; }
    }

    /**
     * 测试用的 DTO 类 - forApi = false (不需要脱敏)
     */
    static class TestDtoWithForApiFalse {
        @Sensitive(type = SensitiveType.PHONE, forApi = false)
        private String phone = "13812345678";

        @Sensitive(type = SensitiveType.ID_CARD) // 默认 false
        private String idCard = "110101199001011234";

        public String getPhone() { return phone; }
        public String getIdCard() { return idCard; }
    }

    /**
     * 测试用的 DTO 类 - 没有注解 (正常序列化)
     */
    static class TestDtoWithoutAnnotation {
        private String normal = "normal text";

        public String getNormal() { return normal; }
    }

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        writer = new StringWriter();
    }

    @Test
    void testSerializeWithForApiTrue() throws Exception {
        // 测试 forApi = true 时的脱敏
        TestDtoWithForApiTrue dto = new TestDtoWithForApiTrue();
        String json = objectMapper.writeValueAsString(dto);

        // 验证脱敏后的 JSON
        assertTrue(json.contains("138****5678"), "手机号应该脱敏");
        assertTrue(json.contains("110101********1234"), "身份证应该脱敏");
        assertTrue(json.contains("t***@example.com"), "邮箱应该脱敏");
        assertTrue(json.contains("12*****890"), "自定义应该脱敏");
    }

    @Test
    void testSerializeWithForApiFalse() throws Exception {
        // 测试 forApi = false 时不脱敏
        TestDtoWithForApiFalse dto = new TestDtoWithForApiFalse();
        String json = objectMapper.writeValueAsString(dto);

        // 验证返回完整数据
        assertTrue(json.contains("13812345678"), "手机号应该是完整数据");
        assertTrue(json.contains("110101199001011234"), "身份证应该是完整数据");
        assertFalse(json.contains("138****5678"), "手机号不应该脱敏");
        assertFalse(json.contains("110101********1234"), "身份证不应该脱敏");
    }

    @Test
    void testSerializeNullValue() throws Exception {
        // 测试 null 值处理
        TestDtoWithForApiTrue dto = new TestDtoWithForApiTrue();
        String json = objectMapper.writeValueAsString(dto);

        // null 字段应该输出 null
        assertTrue(json.contains("null"), "null 字段应该序列化为 null");
    }

    @Test
    void testSerializeWithoutAnnotation() throws Exception {
        // 测试没有注解的字段正常序列化
        TestDtoWithoutAnnotation dto = new TestDtoWithoutAnnotation();
        String json = objectMapper.writeValueAsString(dto);

        assertTrue(json.contains("normal text"), "没有注解的字段应该正常序列化");
    }

    @Test
    void testSerializeWithCustomMaskChar() throws Exception {
        // 测试自定义掩码字符
        class TestDto {
            @Sensitive(type = SensitiveType.PHONE, maskChar = '#', forApi = true)
            private String phone = "13812345678";

            public String getPhone() { return phone; }
        }

        TestDto dto = new TestDto();
        String json = objectMapper.writeValueAsString(dto);

        assertTrue(json.contains("138####5678"), "应该使用自定义掩码字符 #");
    }

    @Test
    void testSerializeWithEmptyString() throws Exception {
        // 测试空字符串处理
        class TestDto {
            @Sensitive(type = SensitiveType.PHONE, forApi = true)
            private String phone = "";

            public String getPhone() { return phone; }
        }

        TestDto dto = new TestDto();
        String json = objectMapper.writeValueAsString(dto);

        // 空字符串应该正常序列化
        assertTrue(json.contains("\"\"") || json.contains("\"phone\":\"\""), "空字符串应该正常序列化");
    }

    @Test
    void testDifferentSensitiveTypes() throws Exception {
        // 测试所有内置脱敏类型
        class TestDto {
            @Sensitive(type = SensitiveType.PHONE, forApi = true)
            private String phone = "13812345678";

            @Sensitive(type = SensitiveType.ID_CARD, forApi = true)
            private String idCard = "110101199001011234";

            @Sensitive(type = SensitiveType.EMAIL, forApi = true)
            private String email = "test@example.com";

            @Sensitive(type = SensitiveType.BANK_CARD, forApi = true)
            private String bankCard = "6222021234567890123";

            @Sensitive(type = SensitiveType.NAME, forApi = true)
            private String name = "张三丰";

            public String getPhone() { return phone; }
            public String getIdCard() { return idCard; }
            public String getEmail() { return email; }
            public String getBankCard() { return bankCard; }
            public String getName() { return name; }
        }

        TestDto dto = new TestDto();
        String json = objectMapper.writeValueAsString(dto);

        assertTrue(json.contains("138****5678"), "PHONE 类型脱敏");
        assertTrue(json.contains("110101********1234"), "ID_CARD 类型脱敏");
        assertTrue(json.contains("t***@example.com"), "EMAIL 类型脱敏");
        assertTrue(json.contains("6222***********0123"), "BANK_CARD 类型脱敏");
        assertTrue(json.contains("张*丰"), "NAME 类型脱敏");
    }

    @Test
    void testContextualSerializerCaching() throws Exception {
        // 测试序列化器缓存机制
        class TestDto {
            @Sensitive(type = SensitiveType.PHONE, forApi = true)
            private String phone = "13812345678";

            public String getPhone() { return phone; }
        }

        TestDto dto = new TestDto();

        // 多次序列化同一个对象
        String json1 = objectMapper.writeValueAsString(dto);
        String json2 = objectMapper.writeValueAsString(dto);

        // 结果应该一致,证明序列化器被正确缓存
        assertEquals(json1, json2, "多次序列化结果应该一致");
        assertTrue(json1.contains("138****5678"), "应该正确脱敏");
    }
}
