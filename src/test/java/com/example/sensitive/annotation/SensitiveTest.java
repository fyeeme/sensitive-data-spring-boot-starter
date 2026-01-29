package com.example.sensitive.annotation;

import com.example.sensitive.enums.SensitiveType;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link Sensitive} 注解测试
 */
class SensitiveTest {

    /**
     * 测试用的 DTO 类
     */
    static class TestDto {
        @Sensitive(type = SensitiveType.PHONE, forApi = true)
        private String phone;

        @Sensitive(type = SensitiveType.ID_CARD) // forApi = false (默认)
        private String idCard;

        @Sensitive(type = SensitiveType.EMAIL, forApi = false)
        private String email;
    }

    @Test
    void testDefaultForApiValue() throws NoSuchFieldException {
        // 测试默认值为 false
        Field field = TestDto.class.getDeclaredField("idCard");
        Sensitive annotation = field.getAnnotation(Sensitive.class);

        assertNotNull(annotation, "注解应该存在");
        assertFalse(annotation.forApi(), "默认 forApi 应该为 false");
        assertEquals(SensitiveType.ID_CARD, annotation.type());
    }

    @Test
    void testExplicitForApiTrue() throws NoSuchFieldException {
        // 测试显式设置为 true
        Field field = TestDto.class.getDeclaredField("phone");
        Sensitive annotation = field.getAnnotation(Sensitive.class);

        assertNotNull(annotation, "注解应该存在");
        assertTrue(annotation.forApi(), "forApi 应该为 true");
        assertEquals(SensitiveType.PHONE, annotation.type());
    }

    @Test
    void testExplicitForApiFalse() throws NoSuchFieldException {
        // 测试显式设置为 false
        Field field = TestDto.class.getDeclaredField("email");
        Sensitive annotation = field.getAnnotation(Sensitive.class);

        assertNotNull(annotation, "注解应该存在");
        assertFalse(annotation.forApi(), "forApi 应该为 false");
        assertEquals(SensitiveType.EMAIL, annotation.type());
    }

    @Test
    void testAnnotationExists() {
        // 测试注解存在
        Annotation[] annotations = TestDto.class.getDeclaredFields()[0].getAnnotations();
        boolean hasSensitive = false;
        for (Annotation ann : annotations) {
            if (ann instanceof Sensitive) {
                hasSensitive = true;
                break;
            }
        }
        assertTrue(hasSensitive, "字段应该有 @Sensitive 注解");
    }

    @Test
    void testCustomParameters() throws NoSuchFieldException {
        // 测试自定义参数
        Field field = TestDto.class.getDeclaredField("phone");
        Sensitive annotation = field.getAnnotation(Sensitive.class);

        assertEquals(0, annotation.prefixLength(), "默认 prefixLength 应该为 0");
        assertEquals(0, annotation.suffixLength(), "默认 suffixLength 应该为 0");
        assertEquals('*', annotation.maskChar(), "默认 maskChar 应该为 *");
    }
}
