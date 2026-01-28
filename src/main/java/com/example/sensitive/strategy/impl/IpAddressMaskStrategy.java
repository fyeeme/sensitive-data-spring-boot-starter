package com.example.sensitive.strategy.impl;

import com.example.sensitive.enums.SensitiveType;
import com.example.sensitive.strategy.MaskStrategy;

/**
 * IP地址脱敏策略
 * 192.168.*.*
 * 
 * @author example
 */
public class IpAddressMaskStrategy implements MaskStrategy {
    
    @Override
    public SensitiveType getType() {
        return SensitiveType.IP_ADDRESS;
    }
    
    @Override
    public String mask(String value, char maskChar) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        
        // IPv4: 192.168.1.1 -> 192.168.*.*
        // IPv6: 简单处理
        String[] parts = value.split("\\.");
        if (parts.length == 4) {
            return parts[0] + "." + parts[1] + "." + maskChar + "." + maskChar;
        }
        
        // 非标准格式，返回原值或部分脱敏
        return value;
    }
}
