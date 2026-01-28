package com.example.sensitive.jackson;

import com.example.sensitive.annotation.Sensitive;
import com.example.sensitive.context.SensitiveContext;
import com.example.sensitive.enums.SensitiveType;
import com.example.sensitive.strategy.MaskStrategyFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;

import java.io.IOException;
import java.util.Objects;

/**
 * 敏感数据序列化器
 * <p>
 * 基于 Jackson ContextualSerializer 实现上下文感知的脱敏序列化
 * 
 * @author example
 */
public class SensitiveSerializer extends JsonSerializer<String> implements ContextualSerializer {
    
    private SensitiveType type;
    private int prefixLength;
    private int suffixLength;
    private char maskChar;
    private boolean enabled;
    
    /**
     * 默认构造函数（Jackson 需要）
     */
    public SensitiveSerializer() {
        this.type = SensitiveType.DEFAULT;
        this.maskChar = '*';
        this.enabled = true;
    }
    
    /**
     * 带参数构造函数
     */
    public SensitiveSerializer(SensitiveType type, int prefixLength, int suffixLength, 
                               char maskChar, boolean enabled) {
        this.type = type;
        this.prefixLength = prefixLength;
        this.suffixLength = suffixLength;
        this.maskChar = maskChar;
        this.enabled = enabled;
    }
    
    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) 
            throws IOException {
        if (value == null) {
            gen.writeNull();
            return;
        }
        
        // 检查是否全局禁用或当前注解禁用
        if (!enabled || SensitiveContext.isDisabled()) {
            gen.writeString(value);
            return;
        }
        
        // 执行脱敏
        String maskedValue;
        if (type == SensitiveType.CUSTOM) {
            maskedValue = MaskStrategyFactory.maskCustom(value, prefixLength, suffixLength, maskChar);
        } else {
            maskedValue = MaskStrategyFactory.mask(value, type, maskChar);
        }
        
        gen.writeString(maskedValue);
    }
    
    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property) 
            throws JsonMappingException {
        if (property == null) {
            return this;
        }
        
        // 从属性上获取 @Sensitive 注解
        Sensitive sensitive = property.getAnnotation(Sensitive.class);
        if (sensitive == null) {
            sensitive = property.getContextAnnotation(Sensitive.class);
        }
        
        if (sensitive != null) {
            return new SensitiveSerializer(
                    sensitive.type(),
                    sensitive.prefixLength(),
                    sensitive.suffixLength(),
                    sensitive.maskChar(),
                    sensitive.enabled()
            );
        }
        
        return this;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SensitiveSerializer that = (SensitiveSerializer) o;
        return prefixLength == that.prefixLength && 
               suffixLength == that.suffixLength && 
               maskChar == that.maskChar && 
               enabled == that.enabled && 
               type == that.type;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(type, prefixLength, suffixLength, maskChar, enabled);
    }
}
