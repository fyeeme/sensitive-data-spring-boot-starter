package com.example.sensitive.util;

import com.example.sensitive.config.SensitiveLogProperties;
import com.example.sensitive.config.SensitiveLogProperties.MaskMode;
import com.example.sensitive.context.SensitiveContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 敏感日志工具类
 * <p>
 * 提供便捷的日志脱敏方法，支持多种使用场景和配置模式
 * 
 * <h3>模式说明：</h3>
 * <ul>
 *   <li><b>JACKSON</b>: 使用 Jackson 序列化脱敏，适合需要 JSON 格式的场景</li>
 *   <li><b>TO_STRING</b>: 使用 toString() 脱敏，最简单，直接打印对象</li>
 *   <li><b>BOTH</b>: 同时支持两种方式（默认）</li>
 * </ul>
 * 
 * <h3>使用示例：</h3>
 * <pre>
 * // 方式1：Jackson 序列化（任何模式下都可用）
 * log.info("用户信息: {}", SensitiveLogUtils.toJson(user));
 * 
 * // 方式2：toString（需要实体类继承 SensitiveEntity 或实现 SensitiveToStringSupport）
 * log.info("用户信息: {}", user);
 * 
 * // 方式3：统一入口（根据配置自动选择）
 * log.info("用户信息: {}", SensitiveLogUtils.mask(user));
 * 
 * // 方式4：临时禁用脱敏
 * try (var scope = SensitiveLogUtils.disableMask()) {
 *     log.debug("完整用户信息: {}", SensitiveLogUtils.toJson(user));
 * }
 * </pre>
 * 
 * @author example
 */
public class SensitiveLogUtils {
    
    private static final Logger log = LoggerFactory.getLogger(SensitiveLogUtils.class);
    
    private static ObjectMapper sensitiveMapper;
    private static SensitiveLogProperties properties;
    private static MaskMode currentMode = MaskMode.BOTH;
    
    /**
     * 构造函数（由 Spring 注入）
     */
    public SensitiveLogUtils(ObjectMapper sensitiveMapper, SensitiveLogProperties properties) {
        SensitiveLogUtils.sensitiveMapper = sensitiveMapper;
        SensitiveLogUtils.properties = properties;
        SensitiveLogUtils.currentMode = properties.getMode();
        log.info("SensitiveLogUtils initialized with mode: {}", currentMode);
    }
    
    /**
     * 统一脱敏入口（根据配置模式自动选择）
     * <p>
     * - JACKSON 模式：返回 JSON 字符串
     * - TO_STRING 模式：返回 toString() 结果
     * - BOTH 模式：优先使用 toString()（如果对象支持），否则使用 JSON
     *
     * @param obj 要脱敏的对象
     * @return 脱敏后的字符串
     */
    public static String mask(Object obj) {
        if (obj == null) {
            return "null";
        }
        
        return switch (currentMode) {
            case JACKSON -> toJson(obj);
            case TO_STRING -> toStringMask(obj);
            case BOTH -> smartMask(obj);
        };
    }
    
    /**
     * 智能选择脱敏方式
     */
    private static String smartMask(Object obj) {
        // 如果对象实现了我们的接口或继承了基类，优先使用 toString
        // 否则使用 Jackson
        Class<?> clazz = obj.getClass();
        
        // 检查是否有自定义的 toString（不是 Object 的默认实现）
        try {
            if (clazz.getMethod("toString").getDeclaringClass() != Object.class) {
                return obj.toString();
            }
        } catch (NoSuchMethodException ignored) {
        }
        
        // 降级到 Jackson
        return toJson(obj);
    }
    
    /**
     * 使用 toString 进行脱敏
     * <p>
     * 要求对象继承 SensitiveEntity 或实现 SensitiveToStringSupport
     */
    private static String toStringMask(Object obj) {
        return obj.toString();
    }
    
    /**
     * 将对象转换为 JSON 字符串（脱敏）
     *
     * @param obj 要序列化的对象
     * @return 脱敏后的 JSON 字符串
     */
    public static String toJson(Object obj) {
        if (obj == null) {
            return "null";
        }
        
        if (sensitiveMapper == null) {
            log.warn("SensitiveLogUtils not initialized, falling back to toString()");
            return obj.toString();
        }
        
        try {
            return sensitiveMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize object to JSON: {}", e.getMessage());
            return obj.toString();
        }
    }
    
    /**
     * 将对象转换为格式化的 JSON 字符串（脱敏）
     *
     * @param obj 要序列化的对象
     * @return 格式化后的脱敏 JSON 字符串
     */
    public static String toPrettyJson(Object obj) {
        if (obj == null) {
            return "null";
        }
        
        if (sensitiveMapper == null) {
            log.warn("SensitiveLogUtils not initialized, falling back to toString()");
            return obj.toString();
        }
        
        try {
            return sensitiveMapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize object to pretty JSON: {}", e.getMessage());
            return obj.toString();
        }
    }
    
    /**
     * 将对象转换为 JSON 字符串（不脱敏）
     *
     * @param obj 要序列化的对象
     * @return 原始 JSON 字符串（不脱敏）
     */
    public static String toRawJson(Object obj) {
        if (obj == null) {
            return "null";
        }
        
        if (sensitiveMapper == null) {
            log.warn("SensitiveLogUtils not initialized, falling back to toString()");
            return obj.toString();
        }
        
        try (SensitiveContext.DisableScope scope = SensitiveContext.disable()) {
            return sensitiveMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize object to raw JSON: {}", e.getMessage());
            return obj.toString();
        }
    }
    
    /**
     * 临时禁用脱敏
     * <p>
     * 使用 try-with-resources 语法：
     * <pre>
     * try (var scope = SensitiveLogUtils.disableMask()) {
     *     // 此代码块内的序列化不会脱敏
     * }
     * </pre>
     *
     * @return DisableScope 作用域对象
     */
    public static SensitiveContext.DisableScope disableMask() {
        return SensitiveContext.disable();
    }
    
    /**
     * 检查是否已初始化
     */
    public static boolean isInitialized() {
        return sensitiveMapper != null;
    }
    
    /**
     * 获取当前脱敏模式
     */
    public static MaskMode getCurrentMode() {
        return currentMode;
    }
    
    /**
     * 设置脱敏模式（运行时动态切换）
     */
    public static void setMode(MaskMode mode) {
        currentMode = mode;
        log.info("SensitiveLogUtils mode changed to: {}", mode);
    }
    
    /**
     * 手动初始化（用于非 Spring 环境）
     */
    public static void init(ObjectMapper mapper) {
        SensitiveLogUtils.sensitiveMapper = mapper;
    }
    
    /**
     * 手动初始化（用于非 Spring 环境，指定模式）
     */
    public static void init(ObjectMapper mapper, MaskMode mode) {
        SensitiveLogUtils.sensitiveMapper = mapper;
        SensitiveLogUtils.currentMode = mode;
    }
}
