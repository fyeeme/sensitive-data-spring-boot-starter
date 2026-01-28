package com.example.sensitive.jackson;

import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * 敏感数据 Jackson Module
 * <p>
 * 用于注册敏感数据相关的序列化器和反序列化器
 * 
 * @author example
 */
public class SensitiveModule extends SimpleModule {
    
    private static final String MODULE_NAME = "SensitiveModule";
    
    public SensitiveModule() {
        super(MODULE_NAME);
    }
    
    @Override
    public void setupModule(SetupContext context) {
        super.setupModule(context);
        // 可以在这里注册额外的序列化器
        // context.addBeanSerializerModifier(...);
    }
}
