package com.example.sensitive.config;

import com.example.sensitive.jackson.SensitiveModule;
import com.example.sensitive.util.SensitiveLogUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 敏感日志自动配置
 * 
 * @author example
 */
@AutoConfiguration
@ConditionalOnClass(ObjectMapper.class)
@EnableConfigurationProperties(SensitiveLogProperties.class)
@ConditionalOnProperty(prefix = "sensitive.log", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SensitiveLogAutoConfiguration {
    
    private static final Logger log = LoggerFactory.getLogger(SensitiveLogAutoConfiguration.class);
    
    /**
     * 注册敏感数据 Jackson Module
     */
    @Bean
    @ConditionalOnMissingBean
    public SensitiveModule sensitiveModule() {
        log.info("Registering SensitiveModule for Jackson serialization");
        return new SensitiveModule();
    }
    
    /**
     * 日志专用 ObjectMapper（启用脱敏）
     */
    @Bean("sensitiveLogObjectMapper")
    @ConditionalOnProperty(prefix = "sensitive.log", name = "enable-logging-mapper", havingValue = "true", matchIfMissing = true)
    public ObjectMapper sensitiveLogObjectMapper(SensitiveModule sensitiveModule) {
        log.info("Creating sensitive log ObjectMapper with masking enabled");
        
        ObjectMapper mapper = new ObjectMapper();
        
        // 注册 Java 8 时间模块
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        // 注册敏感数据模块
        mapper.registerModule(sensitiveModule);
        
        // 其他配置
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        
        return mapper;
    }
    
    /**
     * 日志工具类初始化
     */
    @Bean
    @ConditionalOnMissingBean
    public SensitiveLogUtils sensitiveLogUtils(
            @Qualifier("sensitiveLogObjectMapper") ObjectMapper sensitiveLogObjectMapper,
            SensitiveLogProperties properties) {
        log.info("Initializing SensitiveLogUtils");
        return new SensitiveLogUtils(sensitiveLogObjectMapper, properties);
    }
}
