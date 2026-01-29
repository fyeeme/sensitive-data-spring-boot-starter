package com.example.sensitive.support;

import com.example.sensitive.util.SensitiveToStringBuilder;

/**
 * 敏感数据 toString 支持接口
 * <p>
 * 当实体类已经有父类无法继承 {@link SensitiveEntity} 时，可以实现此接口
 * <p>
 * <b>注意：实现此接口后，需要手动覆写 toString() 方法调用 toSensitiveString()</b>
 * 
 * <p>使用示例:
 * <pre>
 * public class UserDTO extends BaseEntity implements SensitiveSupport {
 *     
 *     &#64;Sensitive(type = SensitiveType.PHONE)
 *     private String phone;
 *     
 *     &#64;Override
 *     public String toString() {
 *         return toSensitiveString();  // 调用接口默认方法
 *     }
 * }
 * </pre>
 * 
 * <p>配合 Lombok 使用:
 * <pre>
 * &#64;Data
 * &#64;ToString(callSuper = false)  // 禁用 Lombok 的 toString
 * public class UserDTO extends BaseEntity implements SensitiveSupport {
 *     
 *     &#64;Sensitive(type = SensitiveType.PHONE)
 *     private String phone;
 *     
 *     &#64;Override
 *     public String toString() {
 *         return toSensitiveString();
 *     }
 * }
 * </pre>
 * 
 * @author example
 * @see SensitiveEntity
 * @see SensitiveToStringBuilder
 */
public interface SensitiveSupport {
    
    /**
     * 返回脱敏后的字符串表示
     * <p>
     * 此方法会扫描当前对象的所有字段，对带有 @Sensitive 注解的字段进行脱敏处理
     * 
     * @return 脱敏后的字符串
     */
    default String toSensitiveString() {
        return SensitiveToStringBuilder.build(this);
    }
    
    /**
     * 返回包含指定字段的脱敏字符串
     * 
     * @param fieldNames 要包含的字段名
     * @return 脱敏后的字符串
     */
    default String toSensitiveStringWith(String... fieldNames) {
        return SensitiveToStringBuilder.buildWith(this, fieldNames);
    }
    
    /**
     * 返回排除指定字段的脱敏字符串
     * 
     * @param fieldNames 要排除的字段名
     * @return 脱敏后的字符串
     */
    default String toSensitiveStringWithout(String... fieldNames) {
        return SensitiveToStringBuilder.buildWithout(this, fieldNames);
    }
}
