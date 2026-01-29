package com.example.sensitive.support;

import com.example.sensitive.util.SensitiveToStringBuilder;

/**
 * 敏感数据实体基类。
 * <p>
 * 继承此类后，toString() 方法会自动对带有 @Sensitive 注解的字段进行脱敏。
 * <p>
 * <b>重要：此方式不会影响接口返回值！</b>
 * <ul>
 *   <li>Spring MVC 返回 JSON 时使用 Jackson 序列化（调用 getter），不调用 toString()</li>
 *   <li>日志打印时调用 toString()，会进行脱敏</li>
 * </ul>
 * <p>
 * 使用示例:
 * <pre>
 * // 方式1：继承 SensitiveEntity
 * public class UserDTO extends SensitiveEntity {
 *     &#64;Sensitive(type = SensitiveType.PHONE)
 *     private String phone;
 *     
 *     // getter/setter...
 * }
 * 
 * // 日志打印（自动脱敏）
 * log.info("用户: {}", user);
 * // 输出: 用户: UserDTO(phone=138****5678)
 * 
 * // API 返回（不脱敏，返回完整数据）
 * &#64;GetMapping("/user")
 * public UserDTO getUser() {
 *     return user;  // JSON 返回完整手机号
 * }
 * </pre>
 * <p>
 * 如果实体类已经有父类，可以使用 {@link SensitiveToStringBuilder#build(Object)} 方法：
 * <pre>
 * public class UserDTO extends BaseEntity {
 *     &#64;Override
 *     public String toString() {
 *         return SensitiveToStringBuilder.build(this);
 *     }
 * }
 * </pre>
 *
 * @author example
 * @see SensitiveToStringBuilder
 */
public abstract class SensitiveEntity {

    /**
     * 返回脱敏后的字符串表示。
     * <p>
     * 此方法会扫描当前对象的所有字段，对带有 @Sensitive 注解的字段进行脱敏处理。
     *
     * @return 脱敏后的字符串
     */
    @Override
    public String toString() {
        return SensitiveToStringBuilder.build(this);
    }

    /**
     * 返回包含指定字段的脱敏字符串。
     *
     * @param fieldNames 要包含的字段名
     * @return 脱敏后的字符串
     */
    protected String toStringWith(String... fieldNames) {
        return SensitiveToStringBuilder.buildWith(this, fieldNames);
    }

    /**
     * 返回排除指定字段的脱敏字符串。
     *
     * @param fieldNames 要排除的字段名
     * @return 脱敏后的字符串
     */
    protected String toStringWithout(String... fieldNames) {
        return SensitiveToStringBuilder.buildWithout(this, fieldNames);
    }
}
