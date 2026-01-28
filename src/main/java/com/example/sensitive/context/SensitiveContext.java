package com.example.sensitive.context;

/**
 * 敏感数据脱敏上下文
 * <p>
 * 提供线程级别的脱敏开关控制，支持在特定场景下临时禁用脱敏
 * 
 * <p>使用示例:
 * <pre>
 * // 临时禁用脱敏
 * try (SensitiveContext.DisableScope scope = SensitiveContext.disable()) {
 *     String json = objectMapper.writeValueAsString(user);
 *     // 此时 json 包含完整敏感数据
 * }
 * // 离开作用域后自动恢复脱敏
 * </pre>
 * 
 * @author example
 */
public final class SensitiveContext {
    
    /**
     * 线程本地变量：是否禁用脱敏
     */
    private static final ThreadLocal<Boolean> DISABLED = ThreadLocal.withInitial(() -> Boolean.FALSE);
    
    private SensitiveContext() {
        // 工具类禁止实例化
    }
    
    /**
     * 检查当前线程是否禁用脱敏
     */
    public static boolean isDisabled() {
        return Boolean.TRUE.equals(DISABLED.get());
    }
    
    /**
     * 在当前线程禁用脱敏
     * 
     * @return DisableScope 作用域对象，用于 try-with-resources
     */
    public static DisableScope disable() {
        DISABLED.set(Boolean.TRUE);
        return new DisableScope();
    }
    
    /**
     * 在当前线程启用脱敏（恢复默认）
     */
    public static void enable() {
        DISABLED.set(Boolean.FALSE);
    }
    
    /**
     * 清除线程本地变量（防止内存泄漏）
     */
    public static void clear() {
        DISABLED.remove();
    }
    
    /**
     * 禁用脱敏作用域
     * 支持 try-with-resources 自动恢复
     */
    public static class DisableScope implements AutoCloseable {
        
        @Override
        public void close() {
            enable();
        }
    }
}
