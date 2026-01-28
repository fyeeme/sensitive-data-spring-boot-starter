package com.example.sensitive.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * 敏感日志配置属性
 * 
 * @author example
 */
@ConfigurationProperties(prefix = "sensitive.log")
public class SensitiveLogProperties {
    
    /**
     * 是否启用脱敏功能
     */
    private boolean enabled = true;
    
    /**
     * 脱敏模式
     * - JACKSON: 基于 Jackson 序列化脱敏（需手动调用 SensitiveLogUtils.toJson()）
     * - TO_STRING: 基于 toString() 脱敏（实体类继承 SensitiveEntity 或实现 toString）
     * - BOTH: 同时支持两种方式（默认）
     */
    private MaskMode mode = MaskMode.BOTH;
    
    /**
     * 默认掩码字符
     */
    private char maskChar = '*';
    
    /**
     * 是否启用日志专用 ObjectMapper
     */
    private boolean enableLoggingMapper = true;
    
    /**
     * 自定义脱敏规则配置
     * key: 规则名称, value: 规则配置
     */
    private Map<String, CustomRule> customRules = new HashMap<>();
    
    /**
     * toString 脱敏配置
     */
    private ToStringConfig toStringConfig = new ToStringConfig();
    
    /**
     * 脱敏模式枚举
     */
    public enum MaskMode {
        /**
         * 基于 Jackson 序列化脱敏
         * 优点：精确控制，支持嵌套对象
         * 使用：log.info("user: {}", SensitiveLogUtils.toJson(user))
         */
        JACKSON,
        
        /**
         * 基于 toString() 脱敏
         * 优点：最简单，日志直接打印对象即可
         * 使用：log.info("user: {}", user)
         */
        TO_STRING,
        
        /**
         * 同时支持两种方式
         */
        BOTH
    }
    
    // Getters and Setters
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public MaskMode getMode() {
        return mode;
    }
    
    public void setMode(MaskMode mode) {
        this.mode = mode;
    }
    
    public char getMaskChar() {
        return maskChar;
    }
    
    public void setMaskChar(char maskChar) {
        this.maskChar = maskChar;
    }
    
    public boolean isEnableLoggingMapper() {
        return enableLoggingMapper;
    }
    
    public void setEnableLoggingMapper(boolean enableLoggingMapper) {
        this.enableLoggingMapper = enableLoggingMapper;
    }
    
    public Map<String, CustomRule> getCustomRules() {
        return customRules;
    }
    
    public void setCustomRules(Map<String, CustomRule> customRules) {
        this.customRules = customRules;
    }
    
    public ToStringConfig getToStringConfig() {
        return toStringConfig;
    }
    
    public void setToStringConfig(ToStringConfig toStringConfig) {
        this.toStringConfig = toStringConfig;
    }
    
    /**
     * 自定义规则配置
     */
    public static class CustomRule {
        
        private int prefixLength = 0;
        private int suffixLength = 0;
        private char maskChar = '*';
        
        public int getPrefixLength() {
            return prefixLength;
        }
        
        public void setPrefixLength(int prefixLength) {
            this.prefixLength = prefixLength;
        }
        
        public int getSuffixLength() {
            return suffixLength;
        }
        
        public void setSuffixLength(int suffixLength) {
            this.suffixLength = suffixLength;
        }
        
        public char getMaskChar() {
            return maskChar;
        }
        
        public void setMaskChar(char maskChar) {
            this.maskChar = maskChar;
        }
    }
    
    /**
     * toString 脱敏配置
     */
    public static class ToStringConfig {
        
        /**
         * 是否启用反射缓存
         */
        private boolean enableCache = true;
        
        /**
         * 缓存最大容量
         */
        private int cacheMaxSize = 1000;
        
        public boolean isEnableCache() {
            return enableCache;
        }
        
        public void setEnableCache(boolean enableCache) {
            this.enableCache = enableCache;
        }
        
        public int getCacheMaxSize() {
            return cacheMaxSize;
        }
        
        public void setCacheMaxSize(int cacheMaxSize) {
            this.cacheMaxSize = cacheMaxSize;
        }
    }
}
