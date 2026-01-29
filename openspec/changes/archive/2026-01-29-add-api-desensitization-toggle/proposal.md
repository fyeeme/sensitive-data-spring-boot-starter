## Why

当前项目仅支持日志脱敏(通过 `toString()` 方法实现),而 API 返回值始终返回完整数据。但在某些场景下,需要同时支持日志和 API 返回值脱敏,例如:
- 对外接口需要保护用户隐私数据
- 管理后台 API 也不应返回完整敏感信息
- 需要灵活控制哪些字段在 API 响应中脱敏

当前架构虽然已经预留了 Jackson 脱敏支持(从 README 中可以看到),但实际代码中已经被删除(根据 git status,`SensitiveModule` 和 `SensitiveSerializer` 已删除)。需要重新引入并简化设计。

## What Changes

### 新增功能
- **注解增强**: 为 `@Sensitive` 注解添加 `forApi` 属性,控制是否对 API 返回值进行脱敏
- **Jackson 序列化器**: 实现自定义的 Jackson `JsonSerializer`,支持根据 `@Sensitive.forApi` 属性进行脱敏
- **零配置设计**: 使用 `@JacksonAnnotationsInside` 元注解,无需 Spring Boot 自动配置即可工作

### 行为变化
- **向后兼容**: 默认情况下 `forApi = false`,不影响现有 API 返回行为
- **可选启用**: 当 `forApi = true` 时,API JSON 序列化会自动脱敏该字段

### 删除功能
- 无(已删除的 Jackson 相关代码将被重新实现)

## Capabilities

### New Capabilities
- **api-desensitization**: 支持 API 返回值的敏感数据脱敏功能

### Modified Capabilities
- 无(现有日志脱敏能力保持不变,仅为新增 API 脱敏能力)

## Impact

### 受影响的代码
- `Sensitive.java`: 添加 `@JacksonAnnotationsInside` 和 `@JsonSerialize` 元注解,添加 `forApi` 属性
- 新增: `SensitiveJsonSerializer.java` - Jackson 序列化器(实现 `ContextualSerializer`)
- **不需要**: `SensitiveJsonModule.java` - 使用元注解替代
- **不需要**: `SensitiveLogAutoConfiguration.java` - 零配置设计

### 受影响的 API
- `@Sensitive` 注解新增可选属性,向后兼容
- 所有使用 `@Sensitive` 注解的字段可选择启用 API 脱敏

### 依赖项
- 无新增外部依赖(使用已有的 Jackson)
- **不依赖** Spring Boot 自动配置,框架无关

### 配置项
- 可选: 通过配置文件全局启用/禁用 API 脱敏(简单开关,不设计复杂配置)
