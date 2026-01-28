package com.example.sensitive.strategy.impl;

import com.example.sensitive.enums.SensitiveType;
import com.example.sensitive.strategy.MaskStrategy;

/**
 * 身份证号脱敏策略
 * 110101********1234
 * 
 * @author example
 */
public class IdCardMaskStrategy implements MaskStrategy {
    
    @Override
    public SensitiveType getType() {
        return SensitiveType.ID_CARD;
    }
    
    @Override
    public String mask(String value, char maskChar) {
        if (value == null || value.length() < 10) {
            return value;
        }
        
        int len = value.length();
        
        // 18位身份证：显示前6后4
        // 15位身份证：显示前6后3
        int prefixLen = 6;
        int suffixLen = (len == 18) ? 4 : 3;
        
        if (len <= prefixLen + suffixLen) {
            return value;
        }
        
        char[] chars = value.toCharArray();
        for (int i = prefixLen; i < len - suffixLen; i++) {
            chars[i] = maskChar;
        }
        return new String(chars);
    }
}
