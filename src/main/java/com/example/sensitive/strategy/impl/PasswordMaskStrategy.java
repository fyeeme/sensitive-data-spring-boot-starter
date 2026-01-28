package com.example.sensitive.strategy.impl;

import com.example.sensitive.enums.SensitiveType;
import com.example.sensitive.strategy.MaskStrategy;

/**
 * 密码脱敏策略
 * ******
 * 
 * @author example
 */
public class PasswordMaskStrategy implements MaskStrategy {
    
    private static final String MASKED = "******";
    
    @Override
    public SensitiveType getType() {
        return SensitiveType.PASSWORD;
    }
    
    @Override
    public String mask(String value, char maskChar) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        // 密码统一显示为固定长度掩码
        return MASKED;
    }
}
