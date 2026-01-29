# 技术方案对比评估: Module注册 vs 元注解

## 方案对比

### 方案A: 自动配置注册 Jackson Module (当前方案)

**实现方式:**
```java
@Configuration
@ConditionalOnProperty(name = "sensitive.log.api-enabled", havingValue = "true", matchIfMissing = true)
public class SensitiveLogAutoConfiguration {
    @Bean
    public SensitiveJsonModule sensitiveJsonModule() {
        return new SensitiveJsonModule();
    }
}

// Module 中注册序列化器
public class SensitiveJsonModule extends SimpleModule {
    public SensitiveJsonModule() {
        addSerializer(String.class, new SensitiveJsonSerializer());
    }
}
```

**工作原理:**
1. Spring Boot 启动时自动注册 Jackson Module
2. Module 将自定义序列化器注册到 ObjectMapper
3. 所有 String 类型字段序列化时都会经过 `SensitiveJsonSerializer`
4. 序列化器检查字段是否有 `@Sensitive(forApi=true)` 注解
5. 如果有则脱敏,否则返回原值

---

### 方案B: 使用 @JacksonAnnotationsInside 元注解 (用户建议方案)

**实现方式:**
```java
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@JacksonAnnotationsInside  // Jackson 元注解
@JsonSerialize(using = SensitiveJsonSerializer.class)  // 指定序列化器
public @interface Sensitive {
    SensitiveType type() default SensitiveType.DEFAULT;
    int prefixLength() default 0;
    int suffixLength() default 0;
    char maskChar() default '*';
    boolean forApi() default false;  // 新增属性
}
```

**工作原理:**
1. `@JacksonAnnotationsInside` 告诉 Jackson 这是一个包含其他 Jackson 注解的元注解
2. `@JsonSerialize` 指定使用 `SensitiveJsonSerializer`
3. 当字段标记 `@Sensitive` 注解时,Jackson 自动使用指定的序列化器
4. 序列化器读取注解的 `forApi` 属性,决定是否脱敏
5. **不需要自动配置**,无需注册 Module

**序列化器实现:**
```java
public class SensitiveJsonSerializer extends StdSerializer<String>
        implements ContextualSerializer {

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider provider)
            throws IOException {
        // 从注入的注解中读取 forApi 属性
        if (annotation != null && annotation.forApi()) {
            String masked = MaskStrategyFactory.mask(value, annotation.type(), annotation.maskChar());
            gen.writeString(masked);
        } else {
            gen.writeString(value);
        }
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property) {
        // 获取字段上的 @Sensitive 注解
        Sensitive annotation = property.getAnnotation(Sensitive.class);
        return new SensitiveJsonSerializer(annotation);
    }
}
```

---

## 详细对比

| 对比维度 | 方案A: Module注册 | 方案B: 元注解 |
|---------|-----------------|-------------|
| **侵入性** | 低(自动配置) | 极低(注解自带) |
| **需要Bean配置** | ✅ 需要 | ❌ 不需要 |
| **需要Spring Boot** | ✅ 必需 | ❌ 非必需 |
| **全局开关** | ✅ 支持(`sensitive.log.api-enabled`) | ❌ 需手动移除注解 |
| **性能影响范围** | 所有String字段检查注解 | 仅标记注解的字段处理 |
| **代码简洁性** | 多个配置类 | 注解包含序列化器信息 |
| **灵活性** | 可统一管理 | 完全由注解控制 |
| **适用场景** | Spring Boot项目 | 任何Jackson项目 |

---

## 技术可行性分析

### 方案B 的关键问题

#### Q1: `@JacksonAnnotationsInside` + `@JsonSerialize` 是否真的不需要Module?
**答案**: ✅ **不需要**

Jackson的 `@JsonSerialize` 注解会直接关联序列化器,无需Module注册:
- Jackson扫描类时发现 `@Sensitive` 注解
- 由于 `@JacksonAnnotationsInside`,Jackson解析内部的 `@JsonSerialize`
- 直接使用指定的 `SensitiveJsonSerializer`
- **完全独立于自动配置**

#### Q2: 序列化器如何读取 `forApi` 属性?
**答案**: ✅ **通过 `ContextualSerializer` 接口**

```java
public class SensitiveJsonSerializer extends StdSerializer<String>
        implements ContextualSerializer {

    private Sensitive annotation;  // 缓存注解

    // 用于创建contextual实例
    private SensitiveJsonSerializer(Sensitive annotation) {
        super(String.class);
        this.annotation = annotation;
    }

    // 无参构造器(用于注册)
    public SensitiveJsonSerializer() {
        super(String.class);
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property) {
        Sensitive ann = property.getAnnotation(Sensitive.class);
        if (ann != null) {
            // 返回带注解上下文的新实例
            return new SensitiveJsonSerializer(ann);
        }
        return this;
    }

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider provider) {
        if (annotation != null && annotation.forApi()) {
            // 脱敏
            gen.writeString(MaskStrategyFactory.mask(value, annotation.type(), annotation.maskChar()));
        } else {
            // 不脱敏
            gen.writeString(value);
        }
    }
}
```

#### Q3: 两个方案的性能差异?
**答案**: 🤔 **差异不大**

- **方案A**: 所有String字段都会检查注解(利用缓存)
- **方案B**: 只有标记 `@Sensitive` 的字段才进入序列化器
- **实际差异**: Jackson有强大的序列化器缓存机制,第一次序列化后配置被缓存
- **结论**: 性能差异可忽略不计(< 1%)

---

## 优缺点总结

### 方案A: Module注册

**优点:**
1. ✅ 全局开关灵活: `sensitive.log.api-enabled=false` 一键禁用
2. ✅ 统一管理: 所有配置集中在自动配置类
3. ✅ 符合Spring Boot约定: Starter标准做法
4. ✅ 可扩展性: 未来可添加更多Jackson配置

**缺点:**
1. ❌ 需要Spring Boot环境
2. ❌ 需要配置Bean和自动配置类
3. ❌ 所有String字段都会经过检查(虽然有缓存)

---

### 方案B: 元注解

**优点:**
1. ✅ **零配置**: 不需要任何Bean配置
2. ✅ **更简洁**: 注解自包含序列化器信息
3. ✅ **框架无关**: 不依赖Spring Boot,任何Jackson项目可用
4. ✅ **按需处理**: 只有标记注解的字段才进入序列化器
5. ✅ **符合"不过度设计"**: 用户明确要求不要配置花
6. ✅ **更直观**: 看注解就知道用了什么序列化器

**缺点:**
1. ❌ 无全局开关: 只能通过移除注解禁用
2. ❌ 粒度太细: 每个字段都要显式标记

---

## 混合方案 (推荐) ⭐⭐⭐⭐⭐

**最佳实践: 结合两者优点**

```java
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@JacksonAnnotationsInside  // ← 关键:添加元注解
@JsonSerialize(using = SensitiveJsonSerializer.class)  // ← 关键:指定序列化器
public @interface Sensitive {
    SensitiveType type() default SensitiveType.DEFAULT;
    int prefixLength() default 0;
    int suffixLength() default 0;
    char maskChar() default '*';
    boolean forApi() default false;
}
```

**保留自动配置(可选):**
```java
@Configuration
@ConditionalOnProperty(name = "sensitive.log.api-enabled", havingValue = "true", matchIfMissing = true)
public class SensitiveLogAutoConfiguration {
    // 可选: 添加其他Jackson配置,如日期格式、Null处理等
    // 不需要注册Module,因为注解已经指定了序列化器
}
```

**优势:**
1. ✅ **零配置**: 理论上不需要Bean配置,添加注解即生效
2. ✅ **保留灵活性**: 仍然可以通过自动配置添加全局功能
3. ✅ **向后兼容**: 即使没有自动配置,注解也能工作
4. ✅ **符合用户需求**: "不要配置花" → 真正的零配置
5. ✅ **框架无关**: 可以在非Spring Boot项目使用

**使用方式:**
```java
public class UserDTO {
    @Sensitive(type = SensitiveType.PHONE, forApi = true)
    private String phone;
}

// API返回 → 自动脱敏
@GetMapping("/user")
public UserDTO getUser() {
    return user;  // {"phone": "138****5678"}
}
```

---

## 最终推荐

### 🏆 推荐: **混合方案** (方案B + 可选自动配置)

**理由:**
1. **符合"不过度设计"**: 用户明确要求不要配置花,方案B真正做到了零配置
2. **更简洁**: 注解自包含,不需要额外配置类
3. **框架无关**: 更通用,不仅限于Spring Boot
4. **可扩展**: 仍然保留自动配置能力,用于全局配置
5. **性能更优**: 只有标记注解的字段才处理

**实施要点:**
1. ✅ 在 `@Sensitive` 注解上添加 `@JacksonAnnotationsInside` 和 `@JsonSerialize`
2. ✅ 保留 `SensitiveJsonSerializer` 实现,使用 `ContextualSerializer` 读取 `forApi` 属性
3. ✅ 保留自动配置类,但**不注册Module**(用于其他全局配置,如全局开关)
4. ✅ 全局开关通过自定义实现,如 `BeanPostProcessor` 动态修改ObjectMapper

---

## 实施建议

如果采用混合方案,需要调整设计文档中的以下部分:

### 1. 修改注解定义 (方案B核心)
```java
@JacksonAnnotationsInside
@JsonSerialize(using = SensitiveJsonSerializer.class)
public @interface Sensitive {
    boolean forApi() default false;
}
```

### 2. 序列化器使用 ContextualSerializer
```java
public class SensitiveJsonSerializer extends StdSerializer<String>
        implements ContextualSerializer {
    // 读取注解的 forApi 属性
}
```

### 3. 全局开关实现调整
由于不再通过Module注册,全局开关需要其他方式实现:
- **选项1**: 使用 `BeanPostProcessor` 动态修改 ObjectMapper
- **选项2**: 保留自动配置,但不注册Module,仅用于其他配置
- **选项3**: 放弃全局开关,完全由注解控制

---

## 结论

| 方案 | 推荐指数 | 理由 |
|-----|---------|------|
| 方案A: Module注册 | ⭐⭐⭐ | 功能完整但需要配置 |
| 方案B: 元注解 | ⭐⭐⭐⭐⭐ | 零配置,简洁,符合需求 |
| 混合方案 | ⭐⭐⭐⭐⭐ | 兼具两者优点,最推荐 |

**最终建议: 采用方案B(元注解)或混合方案**

这样更符合:
- ✅ 用户需求("不要配置花")
- ✅ 代码简洁性
- ✅ 框架无关性
- ✅ 性能优化
