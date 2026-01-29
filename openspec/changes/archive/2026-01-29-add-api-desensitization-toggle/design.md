## Context

### 当前状态
- 项目已实现基于 `toString()` 的日志脱敏方案
- Jackson 序列化器代码已被删除(`SensitiveModule.java`, `SensitiveSerializer.java`)
- 现有架构使用 `MaskStrategyFactory` 进行脱敏策略处理
- Spring Boot Starter 项目,需要自动配置支持

### 约束条件
- 用户明确要求"不要过度设计,不需要配置花"
- 需要保持向后兼容,默认不影响现有 API 返回
- 必须使用默认的 Jackson 序列化机制
- 零侵入性,不影响现有日志脱敏功能

### 利益相关者
- API 使用者:需要灵活控制 API 返回值是否脱敏
- 系统维护者:代码简洁易维护,配置简单

## Goals / Non-Goals

**Goals:**
- 通过注解属性控制是否对 API 返回值脱敏
- 复用现有的 `MaskStrategyFactory` 脱敏策略,避免重复代码
- 提供简单的全局开关(可选),默认启用 API 脱敏功能
- 保持向后兼容,默认不启用 API 脱敏

**Non-Goals:**
- 不实现复杂的角色权限控制(哪个 API 脱敏,哪个不脱敏)
- 不实现动态配置热更新
- 不支持复杂的序列化场景(如 XML、Protobuf 等)
- 不改动现有的日志脱敏逻辑

## Decisions

### 1. 注解设计: `forApi` 属性

**决策**: 在 `@Sensitive` 注解中添加 `boolean forApi() default false` 属性

**理由**:
- ✅ **最简单直观**: 开发者一眼就能看出该字段是否在 API 中脱敏
- ✅ **字段级细粒度控制**: 不同字段可以有不同行为
- ✅ **向后兼容**: 默认 `false` 不影响现有代码
- ✅ **符合用户需求**: 用户明确要求"根据属性是否启用需要对返回值进行脱敏启用"

**替代方案及拒绝理由**:
- ❌ **全局配置文件控制**: 无法实现字段级差异化,不够灵活
- ❌ **单独的 `@ApiSensitive` 注解**: 需要同时标记两个注解,代码冗余
- ❌ **AOP 切面方式**: 过度设计,增加复杂度,用户明确不要过度设计

### 2. Jackson 序列化器设计

**决策**: 创建自定义 `SensitiveJsonSerializer extends StdSerializer<String> implements ContextualSerializer`,使用 `@JacksonAnnotationsInside` 元注解指定序列化器

**实现要点**:
```java
// 1. 注解定义 - 添加元注解
@JacksonAnnotationsInside  // 告诉 Jackson 这是包含其他 Jackson 注解的元注解
@JsonSerialize(using = SensitiveJsonSerializer.class)  // 指定序列化器
public @interface Sensitive {
    boolean forApi() default false;
    // ... 其他属性
}

// 2. 序列化器实现
public class SensitiveJsonSerializer extends StdSerializer<String>
        implements ContextualSerializer {

    private Sensitive annotation;  // 缓存注解实例

    // 无参构造器
    public SensitiveJsonSerializer() {
        super(String.class);
    }

    // 带注解的构造器
    private SensitiveJsonSerializer(Sensitive annotation) {
        super(String.class);
        this.annotation = annotation;
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property) {
        // 获取字段上的 @Sensitive 注解
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
            // 脱敏: 调用 MaskStrategyFactory
            String masked = MaskStrategyFactory.mask(
                value, annotation.type(), annotation.maskChar()
            );
            gen.writeString(masked);
        } else {
            // 不脱敏: 返回原值
            gen.writeString(value);
        }
    }
}
```

**理由**:
- ✅ **零配置**: 不需要注册 Module 或 Bean,注解即生效
- ✅ **复用现有逻辑**: 直接调用 `MaskStrategyFactory`,无重复代码
- ✅ **注解驱动**: 通过 `@Sensitive.forApi` 属性控制,符合用户预期
- ✅ **性能良好**: Jackson 序列化器级别的处理,只处理标记注解的字段
- ✅ **符合"不过度设计"**: 用户明确要求"不要配置花"

**关键技术**:
- **`@JacksonAnnotationsInside`**: Jackson 元注解,表示这个注解包含其他 Jackson 注解
- **`ContextualSerializer`**: Jackson 接口,允许序列化器访问字段上下文(注解)
- **工作原理**: Jackson 发现 `@Sensitive` → 解析内部的 `@JsonSerialize` → 使用 `SensitiveJsonSerializer` → `createContextual` 读取注解

### 3. 不需要自动配置和 Module

**决策**: 不创建 Jackson Module 和 Spring Boot 自动配置类,完全通过注解驱动

**实现要点**:
- `@Sensitive` 注解添加 `@JacksonAnnotationsInside` 和 `@JsonSerialize`
- Jackson 自动发现注解并使用指定的序列化器
- **无需** `SensitiveJsonModule.java`
- **无需** `SensitiveLogAutoConfiguration.java`
- **无需** 在 `META-INF/spring.factories` 中注册

**理由**:
- ✅ **真正的零配置**: 不需要任何 Bean 配置,添加注解即生效
- ✅ **更简洁**: 减少配置类,降低维护成本
- ✅ **框架无关**: 不依赖 Spring Boot,任何使用 Jackson 的项目都能用
- ✅ **符合用户需求**: 用户明确要求"不要配置花"
- ✅ **按需处理**: 只有标记 `@Sensitive` 的字段才进入自定义序列化器

**使用示例**:
```java
public class UserDTO {
    @Sensitive(type = SensitiveType.PHONE, forApi = true)
    private String phone;
}

// API 返回自动脱敏,无需任何配置
@GetMapping("/user")
public UserDTO getUser() {
    return user;  // {"phone": "138****5678"}
}
```

### 4. 全局开关设计(已移除)

**决策**: 不提供全局配置开关

**理由**:
- ✅ **简单优先**: 添加全局开关需要额外配置复杂度
- ✅ **注解驱动**: 字段级别的 `forApi` 属性已经提供了足够的控制
- ✅ **符合"不过度设计"**: 用户明确要求不要配置花

**替代方案**: 如果需要在测试环境禁用 API 脱敏:
- 选项1: 不使用 `forApi = true`
- 选项2: 使用不同的 DTO (测试 DTO vs 生产 DTO)
- 选项3: 移除注解(可以结合 profiles 使用不同的 DTO 类)

### 5. 实现复杂度控制

**决策**: 采用最小可行实现,不引入以下复杂功能

**明确不做**:
- ❌ 不支持复杂的嵌套对象处理(如 `List<UserDTO>`) - 留待后续迭代
- ❌ 不支持自定义序列化器 - 必须使用 Jackson
- ❌ 不支持条件脱敏(如某些角色看脱敏数据,某些看完整数据)
- ❌ 不支持运行时动态修改脱敏规则

**理由**: 用户明确要求"不要过度设计"

## Risks / Trade-offs

### Risk 1: Jackson 版本兼容性
**风险**: 不同 Spring Boot 版本使用的 Jackson 版本可能不一致

**缓解措施**:
- 使用 Jackson 稳定 API (`StdSerializer`, `ContextualSerializer`, `@JacksonAnnotationsInside`)
- 不依赖 Jackson 实验性或已废弃的 API
- 在多版本 Spring Boot 中测试(2.7+, 3.x)
- 这些 API 在 Jackson 2.x 和 3.x 中都是稳定的

### Risk 2: 性能影响
**风险**: 每次序列化都要检查注解,可能影响性能

**缓解措施**:
- Jackson 本身有缓存机制,`createContextual` 只在字段第一次序列化时执行
- 只有标记了 `@Sensitive` 的字段才进入自定义序列化器,其他字段不受影响
- 序列化器实例会被 Jackson 缓存,后续序列化直接使用
- 预期性能影响 < 1%(相比 Module 方案更优)
- 可选添加性能测试用例验证

### Risk 3: 向后兼容性
**风险**: 新增注解属性可能影响现有代码

**缓解措施**:
- 默认 `forApi = false`,现有代码行为完全不变
- 只修改注解定义,不修改现有脱敏逻辑
- 文档中明确说明新增属性的默认行为
- 升级后无需修改现有代码

### Risk 4: 序列化器优先级冲突
**风险**: 如果字段已有其他 `@JsonSerialize` 注解,可能产生冲突

**缓解措施**:
- `@JsonSerialize` 不能在同一字段重复使用,编译期就会报错
- 用户需要选择: 使用 `@Sensitive(forApi=true)` 或自定义序列化器
- 文档中说明如何处理冲突(使用组合模式或在序列化器中手动调用脱敏逻辑)

### Risk 5: 无全局开关
**风险**: 移除全局开关后,测试环境可能无法查看完整数据

**缓解措施**:
- 默认 `forApi = false`,不影响现有字段
- 测试环境可以不使用 `forApi = true`
- 可以使用不同的 DTO 类(测试 DTO 不标记 `forApi`)
- 文档中说明测试环境的最佳实践

## Migration Plan

### 部署步骤
1. **开发阶段**:
   - 修改 `@Sensitive` 注解,添加 `@JacksonAnnotationsInside` 和 `@JsonSerialize` 元注解
   - 添加 `forApi` 属性
   - 实现 `SensitiveJsonSerializer` (实现 `ContextualSerializer` 接口)
   - **不需要**创建 Module 和自动配置类
   - 编写单元测试和集成测试

2. **测试验证**:
   - 验证 `forApi = false` 时 API 返回完整数据(向后兼容)
   - 验证 `forApi = true` 时 API 返回脱敏数据
   - 验证日志脱敏功能不受影响
   - 性能测试对比(预期性能影响 < 1%)
   - 测试不同 Jackson 版本兼容性

3. **发布**:
   - 更新版本号(如 1.1.0 或 2.0.0)
   - 更新 README 和文档,说明新功能
   - 添加使用示例
   - 强调"零配置"特性

4. **监控**:
   - 收集用户反馈
   - 观察是否有序列化异常
   - 关注性能指标

### 回滚策略
- 由于只是注解增强,回滚很简单:移除 `forApi` 属性和元注解即可
- 或直接回退到上一个版本
- 默认 `forApi = false`,不影响现有功能,风险极低

## Open Questions

1. **是否需要支持嵌套对象脱敏?**
   - 当前设计只处理简单 String 字段
   - 建议: 第一版不支持,后续根据需求迭代

2. **是否需要支持集合类型脱敏?**
   - 如 `List<@Sensitive String> phones`
   - 建议: 第一版不支持,留待后续

3. **是否需要添加全局开关?**
   - 已决定: 不添加全局开关,完全由注解控制
   - 理由: 符合"不过度设计"原则

4. **是否需要支持 Spring Boot 2.x 和 3.x 两个版本?**
   - 建议: 先支持当前主流版本(如 Spring Boot 3.x),后续适配 2.x
   - 由于不使用自动配置,框架兼容性更好
