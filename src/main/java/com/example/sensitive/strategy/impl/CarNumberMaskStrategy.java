package com.example.sensitive.strategy.impl;

import com.example.sensitive.enums.SensitiveType;
import com.example.sensitive.strategy.MaskStrategy;

/**
 * 车牌号脱敏策略
 * 京A****8
 * 
 * @author example
 */
public class CarNumberMaskStrategy implements MaskStrategy {
    
    @Override
    public SensitiveType getType() {
        return SensitiveType.CAR_NUMBER;
    }
    
    @Override
    public String mask(String value, char maskChar) {
        if (value == null || value.length() < 4) {
            return value;
        }
        
        int len = value.length();
        
        // 保留前2后1
        StringBuilder sb = new StringBuilder(len);
        sb.append(value, 0, 2);
        for (int i = 2; i < len - 1; i++) {
            sb.append(maskChar);
        }
        sb.append(value.charAt(len - 1));
        return sb.toString();
    }
}
