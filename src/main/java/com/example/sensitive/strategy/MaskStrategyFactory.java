package com.example.sensitive.strategy;

import com.example.sensitive.enums.SensitiveType;
import com.example.sensitive.strategy.impl.*;

import java.util.EnumMap;
import java.util.Map;

/**
 * 脱敏策略工厂
 * 使用 EnumMap 实现 O(1) 策略查找
 * 
 * @author example
 */
public final class MaskStrategyFactory {
    
    private static final Map<SensitiveType, MaskStrategy> STRATEGIES = new EnumMap<>(SensitiveType.class);
    
    static {
        // 注册所有内置策略
        register(new PhoneMaskStrategy());
        register(new IdCardMaskStrategy());
        register(new BankCardMaskStrategy());
        register(new EmailMaskStrategy());
        register(new NameMaskStrategy());
        register(new AddressMaskStrategy());
        register(new PasswordMaskStrategy());
        register(new CarNumberMaskStrategy());
        register(new IpAddressMaskStrategy());
        register(new DefaultMaskStrategy());
    }
    
    private MaskStrategyFactory() {
        // 工具类禁止实例化
    }
    
    /**
     * 注册策略（支持扩展）
     */
    public static void register(MaskStrategy strategy) {
        if (strategy != null && strategy.getType() != null) {
            STRATEGIES.put(strategy.getType(), strategy);
        }
    }
    
    /**
     * 获取策略
     */
    public static MaskStrategy getStrategy(SensitiveType type) {
        return STRATEGIES.getOrDefault(type, STRATEGIES.get(SensitiveType.DEFAULT));
    }
    
    /**
     * 执行脱敏
     */
    public static String mask(String value, SensitiveType type) {
        return mask(value, type, '*');
    }
    
    /**
     * 执行脱敏（指定掩码字符）
     */
    public static String mask(String value, SensitiveType type, char maskChar) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        
        MaskStrategy strategy = getStrategy(type);
        return strategy.mask(value, maskChar);
    }
    
    /**
     * 自定义脱敏
     */
    public static String maskCustom(String value, int prefixLength, int suffixLength, char maskChar) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        
        CustomMaskStrategy strategy = new CustomMaskStrategy(prefixLength, suffixLength);
        return strategy.mask(value, maskChar);
    }
}
