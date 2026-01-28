package com.example.sensitive.strategy.impl;

import com.example.sensitive.enums.SensitiveType;
import com.example.sensitive.strategy.MaskStrategy;

/**
 * 手机号脱敏策略
 * 138****1234
 * 
 * @author example
 */
public class PhoneMaskStrategy implements MaskStrategy {
    
    @Override
    public SensitiveType getType() {
        return SensitiveType.PHONE;
    }
    
    @Override
    public String mask(String value, char maskChar) {
        if (value == null || value.length() < 7) {
            return value;
        }
        
        // 手机号标准长度 11 位
        if (value.length() == 11) {
            char[] chars = value.toCharArray();
            chars[3] = chars[4] = chars[5] = chars[6] = maskChar;
            return new String(chars);
        }
        
        // 非标准长度，保留前3后4
        int len = value.length();
        if (len <= 7) {
            return value;
        }
        
        StringBuilder sb = new StringBuilder(len);
        sb.append(value, 0, 3);
        for (int i = 3; i < len - 4; i++) {
            sb.append(maskChar);
        }
        sb.append(value, len - 4, len);
        return sb.toString();
    }
}
