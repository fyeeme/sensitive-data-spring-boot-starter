package com.example.sensitive.util;

import com.example.sensitive.annotation.Sensitive;
import com.example.sensitive.enums.SensitiveType;
import com.example.sensitive.strategy.MaskStrategyFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 敏感数据 toString 构建器
 * <p>
 * 使用反射 + 缓存实现高性能的对象脱敏 toString
 * 
 * <p>使用示例:
 * <pre>
 * public class UserDTO {
 *     &#64;Sensitive(type = SensitiveType.PHONE)
 *     private String phone;
 *     
 *     &#64;Override
 *     public String toString() {
 *         return SensitiveToStringBuilder.build(this);
 *     }
 * }
 * </pre>
 * 
 * @author example
 */
public final class SensitiveToStringBuilder {
    
    /**
     * 字段元数据缓存
     * Key: Class
     * Value: 字段元数据列表
     */
    private static final Map<Class<?>, List<FieldMeta>> FIELD_CACHE = new ConcurrentHashMap<>();
    
    /**
     * 最大缓存容量（防止内存泄漏）
     */
    private static final int MAX_CACHE_SIZE = 10000;
    
    private SensitiveToStringBuilder() {
        // 工具类禁止实例化
    }
    
    /**
     * 构建脱敏后的 toString 字符串
     *
     * @param obj 对象
     * @return 脱敏后的字符串表示
     */
    public static String build(Object obj) {
        return buildInternal(obj, null, false);
    }

    /**
     * 构建脱敏后的 toString 字符串（包含指定字段）
     *
     * @param obj        对象
     * @param fieldNames 要包含的字段名
     * @return 脱敏后的字符串表示
     */
    public static String buildWith(Object obj, String... fieldNames) {
        if (fieldNames == null || fieldNames.length == 0) {
            return build(obj);
        }
        return buildInternal(obj, List.of(fieldNames), true);
    }

    /**
     * 构建脱敏后的 toString 字符串（排除指定字段）
     *
     * @param obj        对象
     * @param fieldNames 要排除的字段名
     * @return 脱敏后的字符串表示
     */
    public static String buildWithout(Object obj, String... fieldNames) {
        List<String> excludeFields = (fieldNames != null) ? List.of(fieldNames) : List.of();
        return buildInternal(obj, excludeFields, false);
    }

    /**
     * 内部构建方法
     *
     * @param obj          对象
     * @param filterFields 过滤字段列表（include模式为包含，exclude模式为排除）
     * @param isInclude    是否为包含模式
     * @return 脱敏后的字符串表示
     */
    private static String buildInternal(Object obj, List<String> filterFields, boolean isInclude) {
        if (obj == null) {
            return "null";
        }

        Class<?> clazz = obj.getClass();
        List<FieldMeta> fields = getFieldMetas(clazz);
        StringJoiner joiner = new StringJoiner(", ", clazz.getSimpleName() + "(", ")");

        for (FieldMeta meta : fields) {
            if (shouldIncludeField(meta.name, filterFields, isInclude)) {
                joiner.add(formatField(obj, meta));
            }
        }

        return joiner.toString();
    }

    /**
     * 判断字段是否应该被包含
     */
    private static boolean shouldIncludeField(String fieldName, List<String> filterFields, boolean isInclude) {
        if (filterFields == null) {
            return true;
        }
        boolean isInFilter = filterFields.contains(fieldName);
        return isInclude ? isInFilter : !isInFilter;
    }

    /**
     * 格式化单个字段
     */
    private static String formatField(Object obj, FieldMeta meta) {
        try {
            Object value = meta.field.get(obj);
            return meta.name + "=" + formatValue(value, meta);
        } catch (IllegalAccessException e) {
            return meta.name + "=<access denied>";
        }
    }
    
    /**
     * 获取类的字段元数据（带缓存）
     */
    private static List<FieldMeta> getFieldMetas(Class<?> clazz) {
        return FIELD_CACHE.computeIfAbsent(clazz, c -> {
            // 防止缓存过大
            if (FIELD_CACHE.size() >= MAX_CACHE_SIZE) {
                FIELD_CACHE.clear();
            }
            return parseFields(c);
        });
    }
    
    /**
     * 解析类的字段
     */
    private static List<FieldMeta> parseFields(Class<?> clazz) {
        List<FieldMeta> result = new ArrayList<>();
        Class<?> currentClass = clazz;

        while (currentClass != null && currentClass != Object.class) {
            parseDeclaredFields(currentClass, result);
            currentClass = currentClass.getSuperclass();
        }

        return result;
    }

    /**
     * 解析当前类声明的字段
     */
    private static void parseDeclaredFields(Class<?> clazz, List<FieldMeta> result) {
        for (Field field : clazz.getDeclaredFields()) {
            if (shouldSkipField(field)) {
                continue;
            }
            field.setAccessible(true);
            result.add(createFieldMeta(field));
        }
    }

    /**
     * 判断是否应该跳过该字段
     */
    private static boolean shouldSkipField(Field field) {
        return Modifier.isStatic(field.getModifiers()) || field.isSynthetic();
    }

    /**
     * 创建字段元数据
     */
    private static FieldMeta createFieldMeta(Field field) {
        Sensitive sensitive = field.getAnnotation(Sensitive.class);

        boolean hasSensitive = sensitive != null;
        SensitiveType type = hasSensitive ? sensitive.type() : null;
        int prefixLength = hasSensitive ? sensitive.prefixLength() : 0;
        int suffixLength = hasSensitive ? sensitive.suffixLength() : 0;
        char maskChar = hasSensitive ? sensitive.maskChar() : '*';

        return new FieldMeta(field, field.getName(), hasSensitive, type,
                prefixLength, suffixLength, maskChar);
    }
    
    /**
     * 格式化字段值
     */
    private static String formatValue(Object value, FieldMeta meta) {
        if (value == null) {
            return "null";
        }

        String strValue = value.toString();
        String maskedValue = maskValue(strValue, meta);
        return quoteStringValue(value, maskedValue);
    }

    /**
     * 对敏感值进行脱敏处理
     */
    private static String maskValue(String strValue, FieldMeta meta) {
        if (!meta.hasSensitive || meta.type == null) {
            return strValue;
        }

        if (meta.type == SensitiveType.CUSTOM) {
            return MaskStrategyFactory.maskCustom(strValue, meta.prefixLength,
                    meta.suffixLength, meta.maskChar);
        }
        return MaskStrategyFactory.mask(strValue, meta.type, meta.maskChar);
    }

    /**
     * 为字符串值添加引号
     */
    private static String quoteStringValue(Object value, String strValue) {
        if (value instanceof String) {
            return "\"" + strValue + "\"";
        }
        return strValue;
    }
    
    /**
     * 清除缓存
     */
    public static void clearCache() {
        FIELD_CACHE.clear();
    }
    
    /**
     * 获取缓存大小
     */
    public static int getCacheSize() {
        return FIELD_CACHE.size();
    }
    
    /**
     * 字段元数据
     */
    private record FieldMeta(
            Field field,
            String name,
            boolean hasSensitive,
            SensitiveType type,
            int prefixLength,
            int suffixLength,
            char maskChar
    ) {}
}
