package com.example.sensitive.util;

import com.example.sensitive.annotation.Sensitive;
import com.example.sensitive.enums.SensitiveType;
import com.example.sensitive.strategy.MaskStrategyFactory;

import java.lang.reflect.Field;
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
        if (obj == null) {
            return "null";
        }
        
        Class<?> clazz = obj.getClass();
        List<FieldMeta> fields = getFieldMetas(clazz);
        
        StringJoiner joiner = new StringJoiner(", ", clazz.getSimpleName() + "(", ")");
        
        for (FieldMeta meta : fields) {
            try {
                Object value = meta.field.get(obj);
                String strValue = formatValue(value, meta);
                joiner.add(meta.name + "=" + strValue);
            } catch (IllegalAccessException e) {
                joiner.add(meta.name + "=<access denied>");
            }
        }
        
        return joiner.toString();
    }
    
    /**
     * 构建脱敏后的 toString 字符串（包含指定字段）
     *
     * @param obj        对象
     * @param fieldNames 要包含的字段名
     * @return 脱敏后的字符串表示
     */
    public static String buildWith(Object obj, String... fieldNames) {
        if (obj == null) {
            return "null";
        }
        
        if (fieldNames == null || fieldNames.length == 0) {
            return build(obj);
        }
        
        Class<?> clazz = obj.getClass();
        List<FieldMeta> allFields = getFieldMetas(clazz);
        
        // 过滤指定字段
        List<String> targetFields = List.of(fieldNames);
        
        StringJoiner joiner = new StringJoiner(", ", clazz.getSimpleName() + "(", ")");
        
        for (FieldMeta meta : allFields) {
            if (targetFields.contains(meta.name)) {
                try {
                    Object value = meta.field.get(obj);
                    String strValue = formatValue(value, meta);
                    joiner.add(meta.name + "=" + strValue);
                } catch (IllegalAccessException e) {
                    joiner.add(meta.name + "=<access denied>");
                }
            }
        }
        
        return joiner.toString();
    }
    
    /**
     * 构建脱敏后的 toString 字符串（排除指定字段）
     *
     * @param obj        对象
     * @param fieldNames 要排除的字段名
     * @return 脱敏后的字符串表示
     */
    public static String buildWithout(Object obj, String... fieldNames) {
        if (obj == null) {
            return "null";
        }
        
        Class<?> clazz = obj.getClass();
        List<FieldMeta> allFields = getFieldMetas(clazz);
        
        // 要排除的字段
        List<String> excludeFields = (fieldNames != null) ? List.of(fieldNames) : List.of();
        
        StringJoiner joiner = new StringJoiner(", ", clazz.getSimpleName() + "(", ")");
        
        for (FieldMeta meta : allFields) {
            if (!excludeFields.contains(meta.name)) {
                try {
                    Object value = meta.field.get(obj);
                    String strValue = formatValue(value, meta);
                    joiner.add(meta.name + "=" + strValue);
                } catch (IllegalAccessException e) {
                    joiner.add(meta.name + "=<access denied>");
                }
            }
        }
        
        return joiner.toString();
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
        
        // 遍历类层次结构（包括父类）
        Class<?> currentClass = clazz;
        while (currentClass != null && currentClass != Object.class) {
            Field[] declaredFields = currentClass.getDeclaredFields();
            
            for (Field field : declaredFields) {
                // 跳过静态字段和合成字段
                if (java.lang.reflect.Modifier.isStatic(field.getModifiers()) ||
                    field.isSynthetic()) {
                    continue;
                }
                
                field.setAccessible(true);
                
                Sensitive sensitive = field.getAnnotation(Sensitive.class);
                FieldMeta meta = new FieldMeta(
                        field,
                        field.getName(),
                        sensitive != null,
                        sensitive != null ? sensitive.type() : null,
                        sensitive != null ? sensitive.prefixLength() : 0,
                        sensitive != null ? sensitive.suffixLength() : 0,
                        sensitive != null ? sensitive.maskChar() : '*'
                );
                
                result.add(meta);
            }
            
            currentClass = currentClass.getSuperclass();
        }
        
        return result;
    }
    
    /**
     * 格式化字段值
     */
    private static String formatValue(Object value, FieldMeta meta) {
        if (value == null) {
            return "null";
        }
        
        String strValue = value.toString();
        
        // 如果标记了脱敏注解
        if (meta.hasSensitive && meta.type != null) {
            if (meta.type == SensitiveType.CUSTOM) {
                return MaskStrategyFactory.maskCustom(strValue, meta.prefixLength, 
                        meta.suffixLength, meta.maskChar);
            } else {
                return MaskStrategyFactory.mask(strValue, meta.type, meta.maskChar);
            }
        }
        
        // 字符串类型加引号
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
