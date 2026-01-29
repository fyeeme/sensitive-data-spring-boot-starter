package com.example.sensitive.strategy;

import com.example.sensitive.enums.SensitiveType;
import com.example.sensitive.strategy.impl.*;

import java.util.EnumMap;
import java.util.Map;

/**
 * 脱敏策略工厂
 * <p>
 * 使用 EnumMap 实现 O(1) 策略查找，支持自定义策略扩展
 * </p>
 *
 * @author example
 */
public final class MaskStrategyFactory {

    /** 默认脱敏字符 */
    private static final char DEFAULT_MASK_CHAR = '*';

    /** 默认策略（通用文本脱敏） */
    private static final SensitiveType DEFAULT_TYPE = SensitiveType.TEXT;

    /** 策略映射表 */
    private static final Map<SensitiveType, MaskStrategy> STRATEGIES = new EnumMap<>(SensitiveType.class);

    static {
        // 注册所有内置策略
        register(new PhoneMaskStrategy());
        register(new IdCardMaskStrategy());
        register(new BankCardMaskStrategy());
        register(new EmailMaskStrategy());
        register(new NameMaskStrategy());
        register(new AddressMaskStrategy());
        register(new IpMaskStrategy());
        register(new TextMaskStrategy());
    }

    /**
     * 私有构造函数，工具类禁止实例化
     */
    private MaskStrategyFactory() {
    }

    /**
     * 注册策略（支持扩展）
     *
     * @param strategy 脱敏策略，若为 null 或类型为 null 则忽略
     */
    public static void register(MaskStrategy strategy) {
        if (strategy == null || strategy.getType() == null) {
            return;
        }
        STRATEGIES.put(strategy.getType(), strategy);
    }

    /**
     * 获取指定类型的脱敏策略
     * <p>
     * 若指定类型不存在，则返回默认的文本脱敏策略
     * </p>
     *
     * @param type 脱敏类型
     * @return 对应的脱敏策略
     */
    public static MaskStrategy getStrategy(SensitiveType type) {
        return STRATEGIES.getOrDefault(type, STRATEGIES.get(DEFAULT_TYPE));
    }

    /**
     * 使用默认掩码字符执行脱敏
     *
     * @param value 原始值
     * @param type  脱敏类型
     * @return 脱敏后的值，若输入为空则返回原值
     */
    public static String mask(String value, SensitiveType type) {
        return mask(value, type, DEFAULT_MASK_CHAR);
    }

    /**
     * 使用指定掩码字符执行脱敏
     *
     * @param value    原始值
     * @param type     脱敏类型
     * @param maskChar 掩码字符
     * @return 脱敏后的值，若输入为空则返回原值
     */
    public static String mask(String value, SensitiveType type, char maskChar) {
        if (isBlank(value)) {
            return value;
        }
        return getStrategy(type).mask(value, maskChar);
    }

    /**
     * 自定义脱敏（指定前后保留长度）
     *
     * @param value        原始值
     * @param prefixLength 前缀保留长度
     * @param suffixLength 后缀保留长度
     * @param maskChar     掩码字符
     * @return 脱敏后的值，若输入为空则返回原值
     */
    public static String maskCustom(String value, int prefixLength, int suffixLength, char maskChar) {
        if (isBlank(value)) {
            return value;
        }
        return new CustomMaskStrategy(prefixLength, suffixLength).mask(value, maskChar);
    }

    /**
     * 判断字符串是否为空或空白
     *
     * @param value 待检查字符串
     * @return 若字符串为 null 或空则为 true
     */
    private static boolean isBlank(String value) {
        return value == null || value.isEmpty();
    }
}
