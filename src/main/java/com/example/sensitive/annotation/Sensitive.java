package com.example.sensitive.annotation;

import com.example.sensitive.enums.SensitiveType;
import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记需要进行脱敏处理的敏感字段。
 *
 * <h2>脱敏模式</h2>
 * <table border="1" summary="脱敏模式对比">
 *   <tr><th>模式</th><th>触发条件</th><th>forApi 值</th></tr>
 *   <tr><td>日志脱敏</td><td>调用 {@code toString()}</td><td>任意</td></tr>
 *   <tr><td>API 脱敏</td><td>Jackson 序列化</td><td>{@code true}</td></tr>
 * </table>
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * // 仅日志脱敏
 * public class UserDTO {
 *     @Sensitive(type = SensitiveType.PHONE)
 *     private String phone;
 * }
 *
 * // 日志和 API 都脱敏
 * public class AdminUserDTO {
 *     @Sensitive(type = SensitiveType.PHONE, forApi = true)
 *     private String phone;
 * }
 *
 * // 自定义脱敏规则
 * public class OrderDTO {
 *     @Sensitive(type = SensitiveType.CUSTOM, prefixLength = 3, suffixLength = 4, maskChar = '#')
 *     private String orderNo;
 * }
 * }</pre>
 *
 * @author example
 * @see SensitiveType
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@JacksonAnnotationsInside
@JsonSerialize(using = com.example.sensitive.jackson.SensitiveJsonSerializer.class)
public @interface Sensitive {

    /**
     * 脱敏类型，决定脱敏的具体规则。
     *
     * @return 脱敏类型，默认为 {@link SensitiveType#TEXT}
     */
    SensitiveType type() default SensitiveType.TEXT;

    /**
     * 前置保留长度。
     * <p>仅当 {@code type = CUSTOM} 时生效，指定脱敏时保留前几位明文。
     *
     * @return 前置保留字符数，默认为 0
     */
    int prefixLength() default 0;

    /**
     * 后置保留长度。
     * <p>仅当 {@code type = CUSTOM} 时生效，指定脱敏时保留后几位明文。
     *
     * @return 后置保留字符数，默认为 0
     */
    int suffixLength() default 0;

    /**
     * 用于替换敏感信息的字符。
     *
     * @return 掩码字符，默认为 '*'
     */
    char maskChar() default '*';

    /**
     * 是否对 API 返回值进行脱敏。
     * <p>
     * <ul>
     *   <li>{@code false} - 仅日志脱敏，API 返回完整数据（默认）</li>
     *   <li>{@code true} - 日志和 API 返回值都脱敏</li>
     * </ul>
     *
     * @return 是否启用 API 脱敏，默认为 {@code false}
     */
    boolean forApi() default false;

}
