package com.example.sensitive.strategy.impl;

import com.example.sensitive.enums.SensitiveType;
import com.example.sensitive.strategy.MaskStrategy;

/**
 * 自定义脱敏策略
 * 根据指定的前缀后缀长度进行脱敏
 * 
 * @author example
 */
public class CustomMaskStrategy implements MaskStrategy {
    
    private final int prefixLength;
    private final int suffixLength;
    
    public CustomMaskStrategy(int prefixLength, int suffixLength) {
        this.prefixLength = Math.max(0, prefixLength);
        this.suffixLength = Math.max(0, suffixLength);
    }
    
    @Override
    public SensitiveType getType() {
        return SensitiveType.CUSTOM;
    }
    
    @Override
    public String mask(String value, char maskChar) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        
        int len = value.length();
        
        // 如果保留长度超过总长度，返回原值
        if (prefixLength + suffixLength >= len) {
            return value;
        }
        
        StringBuilder sb = new StringBuilder(len);
        
        // 前缀
        if (prefixLength > 0) {
            sb.append(value, 0, prefixLength);
        }
        
        // 中间掩码
        for (int i = prefixLength; i < len - suffixLength; i++) {
            sb.append(maskChar);
        }
        
        // 后缀
        if (suffixLength > 0) {
            sb.append(value, len - suffixLength, len);
        }
        
        return sb.toString();
    }
}
