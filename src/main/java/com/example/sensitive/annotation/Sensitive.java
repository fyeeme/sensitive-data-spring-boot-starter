package com.example.sensitive.annotation;

import com.example.sensitive.enums.SensitiveType;
import com.example.sensitive.jackson.SensitiveSerializer;
import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 敏感数据标记注解
 * <p>
 * 使用示例:
 * <pre>
 * public class UserDTO {
 *     &#64;Sensitive(type = SensitiveType.PHONE)
 *     private String phone;
 *     
 *     &#64;Sensitive(type = SensitiveType.ID_CARD)
 *     private String idCard;
 * }
 * </pre>
 * 
 * @author example
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@JacksonAnnotationsInside
@JsonSerialize(using = SensitiveSerializer.class)
public @interface Sensitive {
    
    /**
     * 脱敏类型
     */
    SensitiveType type() default SensitiveType.DEFAULT;
    
    /**
     * 前置保留长度（仅 CUSTOM 类型生效）
     */
    int prefixLength() default 0;
    
    /**
     * 后置保留长度（仅 CUSTOM 类型生效）
     */
    int suffixLength() default 0;
    
    /**
     * 替换字符
     */
    char maskChar() default '*';
    
    /**
     * 是否启用（可通过配置动态控制）
     */
    boolean enabled() default true;
}
