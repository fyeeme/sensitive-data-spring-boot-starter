package com.example.sensitive.strategy;

import com.example.sensitive.enums.SensitiveType;

/**
 * 脱敏策略接口
 * 
 * @author example
 */
public interface MaskStrategy {
    
    /**
     * 获取支持的脱敏类型
     */
    SensitiveType getType();
    
    /**
     * 执行脱敏
     *
     * @param value    原始值
     * @param maskChar 脱敏字符
     * @return 脱敏后的值
     */
    String mask(String value, char maskChar);
    
    /**
     * 使用默认脱敏字符执行脱敏
     */
    default String mask(String value) {
        return mask(value, '*');
    }
}
