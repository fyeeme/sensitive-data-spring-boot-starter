package com.example.sensitive.strategy.impl;

import com.example.sensitive.enums.SensitiveType;
import com.example.sensitive.strategy.MaskStrategy;

/**
 * 姓名脱敏策略
 * 张* / 张*三
 * 
 * @author example
 */
public class NameMaskStrategy implements MaskStrategy {
    
    @Override
    public SensitiveType getType() {
        return SensitiveType.NAME;
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
            // 两个字：张*
            return value.charAt(0) + String.valueOf(maskChar);
        }
        
        // 三个字及以上：张*三
        StringBuilder sb = new StringBuilder(len);
        sb.append(value.charAt(0));
        for (int i = 1; i < len - 1; i++) {
            sb.append(maskChar);
        }
        sb.append(value.charAt(len - 1));
        return sb.toString();
    }
}
