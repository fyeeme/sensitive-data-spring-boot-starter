package com.example.sensitive.strategy.impl;

import com.example.sensitive.enums.SensitiveType;
import com.example.sensitive.strategy.MaskStrategy;

/**
 * 邮箱脱敏策略
 * t***@gmail.com
 * 
 * @author example
 */
public class EmailMaskStrategy implements MaskStrategy {
    
    @Override
    public SensitiveType getType() {
        return SensitiveType.EMAIL;
    }
    
    @Override
    public String mask(String value, char maskChar) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        
        int atIndex = value.indexOf('@');
        if (atIndex <= 0) {
            return value;
        }
        
        // 保留首字符 + *** + @后部分
        StringBuilder sb = new StringBuilder();
        sb.append(value.charAt(0));
        
        // 如果@前超过1个字符，添加掩码
        if (atIndex > 1) {
            sb.append(maskChar).append(maskChar).append(maskChar);
        }
        
        sb.append(value.substring(atIndex));
        return sb.toString();
    }
}
