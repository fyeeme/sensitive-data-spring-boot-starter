package com.example.sensitive.strategy.impl;

import com.example.sensitive.enums.SensitiveType;
import com.example.sensitive.strategy.MaskStrategy;

/**
 * 默认脱敏策略
 * 保留首尾，中间脱敏
 * 
 * @author example
 */
public class TextMaskStrategy implements MaskStrategy {
    
    @Override
    public SensitiveType getType() {
        return SensitiveType.TEXT;
    }
    
    @Override
    public String mask(String value, char maskChar) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        
        int len = value.length();
        
        if (len == 1) {
            return String.valueOf(maskChar);
        }
        
        if (len == 2) {
            return String.valueOf(value.charAt(0)) + maskChar;
        }
        
        // 保留首尾
        StringBuilder sb = new StringBuilder(len);
        sb.append(value.charAt(0));
        for (int i = 1; i < len - 1; i++) {
            sb.append(maskChar);
        }
        sb.append(value.charAt(len - 1));
        return sb.toString();
    }
}
