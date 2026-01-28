package com.example.sensitive.strategy.impl;

import com.example.sensitive.enums.SensitiveType;
import com.example.sensitive.strategy.MaskStrategy;

/**
 * 银行卡号脱敏策略
 * 6222****1234
 * 
 * @author example
 */
public class BankCardMaskStrategy implements MaskStrategy {
    
    @Override
    public SensitiveType getType() {
        return SensitiveType.BANK_CARD;
    }
    
    @Override
    public String mask(String value, char maskChar) {
        if (value == null || value.length() < 8) {
            return value;
        }
        
        int len = value.length();
        
        // 保留前4后4
        StringBuilder sb = new StringBuilder(len);
        sb.append(value, 0, 4);
        for (int i = 4; i < len - 4; i++) {
            sb.append(maskChar);
        }
        sb.append(value, len - 4, len);
        return sb.toString();
    }
}
