package com.example.sensitive.support;

import com.example.sensitive.annotation.Sensitive;
import com.example.sensitive.enums.SensitiveType;
import com.example.sensitive.strategy.MaskStrategyFactory;

/**
 * Lombok 配合脱敏的使用示例
 * <p>
 * 提供多种与 Lombok 配合使用的方式，不影响 API 返回
 * 
 * @author example
 */
public class LombokUsageExamples {
    
    // ==================== 方式1：继承 SensitiveEntity（推荐）====================
    
    /**
     * 方式1：继承 SensitiveEntity
     * <p>
     * 优点：最简单，零代码
     * 缺点：需要继承，如果已有父类则不适用
     * 
     * <pre>
     * &#64;Data
     * public class UserDTO extends SensitiveEntity {
     *     private Long id;
     *     
     *     &#64;Sensitive(type = SensitiveType.PHONE)
     *     private String phone;
     * }
     * 
     * // 使用
     * log.info("user: {}", user);  // 自动脱敏
     * </pre>
     */
    public static class Example1 {}
    
    // ==================== 方式2：实现接口 + 覆写 toString ====================
    
    /**
     * 方式2：实现 SensitiveToStringSupport 接口
     * <p>
     * 优点：不占用继承位置
     * 缺点：需要手动覆写 toString()
     * 
     * <pre>
     * &#64;Data
     * public class UserDTO implements SensitiveToStringSupport {
     *     private Long id;
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
     */
    public static class Example2 {}
    
    // ==================== 方式3：Lombok @ToString + 手动脱敏字段 ====================
    
    /**
     * 方式3：使用 Lombok @ToString.Exclude + @ToString.Include 手动控制
     * <p>
     * 优点：细粒度控制，可以自定义格式
     * 缺点：需要为每个敏感字段写方法
     * 
     * <pre>
     * &#64;Data
     * &#64;ToString
     * public class UserDTO {
     *     private Long id;
     *     
     *     &#64;ToString.Exclude  // 排除原字段
     *     &#64;Sensitive(type = SensitiveType.PHONE)
     *     private String phone;
     *     
     *     &#64;ToString.Include(name = "phone")  // 包含脱敏后的值
     *     private String maskedPhone() {
     *         return MaskStrategyFactory.mask(phone, SensitiveType.PHONE);
     *     }
     * }
     * 
     * // Lombok 生成的 toString 会调用 maskedPhone() 方法
     * </pre>
     */
    public static class Example3 {}
    
    // ==================== 方式4：Lombok @ToString(callSuper=false) + 手动覆写 ====================
    
    /**
     * 方式4：禁用 Lombok toString + 手动覆写
     * <p>
     * 优点：完全控制
     * 缺点：Lombok 的 toString 功能浪费了
     * 
     * <pre>
     * &#64;Data
     * &#64;ToString(callSuper = false, onlyExplicitlyIncluded = true)  // 禁用自动 toString
     * public class UserDTO {
     *     private Long id;
     *     
     *     &#64;Sensitive(type = SensitiveType.PHONE)
     *     private String phone;
     *     
     *     &#64;Override
     *     public String toString() {
     *         return SensitiveToStringBuilder.build(this);
     *     }
     * }
     * </pre>
     */
    public static class Example4 {}
    
    // ==================== 方式5：使用 @Accessors(chain=true) + 静态工厂 ====================
    
    /**
     * 方式5：结合 Builder 模式
     * <p>
     * <pre>
     * &#64;Data
     * &#64;Builder
     * public class UserDTO extends SensitiveEntity {
     *     private Long id;
     *     
     *     &#64;Sensitive(type = SensitiveType.PHONE)
     *     private String phone;
     * }
     * 
     * // 使用
     * UserDTO user = UserDTO.builder()
     *     .id(1L)
     *     .phone("13812345678")
     *     .build();
     * 
     * log.info("user: {}", user);  // 自动脱敏
     * </pre>
     */
    public static class Example5 {}
}
