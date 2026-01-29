package com.example.sensitive.jackson;

import com.example.sensitive.annotation.Sensitive;
import com.example.sensitive.enums.SensitiveType;
import com.example.sensitive.strategy.MaskStrategyFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

/**
 * 敏感数据 JSON 序列化器
 * <p>
 * 根据 {@link Sensitive} 注解的 {@code forApi} 属性决定是否对 API 返回值进行脱敏:
 * <ul>
 *   <li>{@code forApi = true}: 脱敏输出</li>
 *   <li>{@code forApi = false}: 输出完整数据</li>
 * </ul>
 * <p>
 * 通过 {@link Sensitive} 注解的元注解 {@code @JsonSerialize} 自动应用,
 * 无需手动配置 Jackson Module。
 * <p>
 * 工作原理:
 * <ol>
 *   <li>字段标记 {@code @Sensitive} 注解</li>
 *   <li>Jackson 发现注解的元注解 {@code @JsonSerialize(using = SensitiveJsonSerializer.class)}</li>
 *   <li>调用 {@code createContextual()} 获取字段上的注解配置</li>
 *   <li>调用 {@code serialize()} 根据 {@code forApi} 属性决定是否脱敏</li>
 * </ol>
 *
 * @author example
 * @see Sensitive
 * @see ContextualSerializer
 */
public class SensitiveJsonSerializer extends StdSerializer<String> implements ContextualSerializer {

    /**
     * 缓存的注解实例(在 createContextual 中设置)
     */
    private Sensitive annotation;

    /**
     * 无参构造器(用于 Jackson 实例化)
     */
    public SensitiveJsonSerializer() {
        super(String.class);
    }

    /**
     * 带注解的构造器(用于 createContextual 返回带上下文的实例)
     *
     * @param annotation 字段上的 {@link Sensitive} 注解
     */
    private SensitiveJsonSerializer(Sensitive annotation) {
        super(String.class);
        this.annotation = annotation;
    }

    /**
     * 创建序列化器上下文
     * <p>
     * Jackson 在首次序列化字段时调用此方法获取注解配置,
     * 后续序列化复用返回的实例。
     *
     * @param prov    Serializer provider
     * @param property 字段属性
     * @return 带注解上下文的序列化器实例
     */
    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property) {
        Sensitive ann = property.getAnnotation(Sensitive.class);
        return ann != null ? new SensitiveJsonSerializer(ann) : this;
    }

    /**
     * 序列化字符串值
     * <p>
     * 根据注解的 {@code forApi} 属性决定是否脱敏:
     * <ul>
     *   <li>{@code forApi = true}: 调用 {@link MaskStrategyFactory#mask(String, SensitiveType, char)} 进行脱敏</li>
     *   <li>{@code forApi = false}: 返回原始值</li>
     * </ul>
     *
     * @param value   要序列化的字符串值
     * @param gen     JSON 生成器
     * @param provider 序列器提供者
     * @throws IOException 如果发生 I/O 错误
     */
    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider provider)
            throws IOException {
        if (value == null) {
            gen.writeNull();
            return;
        }

        if (shouldMask()) {
            gen.writeString(maskValue(value));
        } else {
            gen.writeString(value);
        }
    }

    /**
     * 判断是否需要脱敏
     *
     * @return true 如果需要脱敏
     */
    private boolean shouldMask() {
        return annotation != null && annotation.forApi();
    }

    /**
     * 对值进行脱敏处理
     *
     * @param value 原始值
     * @return 脱敏后的值
     */
    private String maskValue(String value) {
        SensitiveType type = annotation.type();
        char maskChar = annotation.maskChar();

        if (type == SensitiveType.CUSTOM) {
            return MaskStrategyFactory.maskCustom(
                    value,
                    annotation.prefixLength(),
                    annotation.suffixLength(),
                    maskChar
            );
        }
        return MaskStrategyFactory.mask(value, type, maskChar);
    }
}
