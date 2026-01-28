package com.example.sensitive.strategy.impl;

import com.example.sensitive.enums.SensitiveType;
import com.example.sensitive.strategy.MaskStrategy;

/**
 * 地址脱敏策略
 * 北京市朝阳区***
 * 
 * @author example
 */
public class AddressMaskStrategy implements MaskStrategy {
    
    private static final int DEFAULT_PREFIX_LENGTH = 6;
    
    @Override
    public SensitiveType getType() {
        return SensitiveType.ADDRESS;
    }
    
    @Override
    public String mask(String value, char maskChar) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        
        int len = value.length();
        
        if (len <= DEFAULT_PREFIX_LENGTH) {
            return value;
        }
        
        // 保留前6个字符，后面用***代替
        return value.substring(0, DEFAULT_PREFIX_LENGTH) + 
               String.valueOf(maskChar).repeat(3);
    }
}
