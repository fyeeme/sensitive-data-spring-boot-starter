# API 脱敏功能规格

## ADDED Requirements

### Requirement: 通过注解控制 API 返回值脱敏
`@Sensitive` 注解必须支持 `forApi` 属性,用于控制该字段在 API JSON 序列化时是否进行脱敏处理。

**注解定义**:
```java
public @interface Sensitive {
    // ... 现有属性 ...

    /**
     * 是否对 API 返回值进行脱敏
     * 默认 false,表示 API 返回完整数据,仅日志脱敏
     */
    boolean forApi() default false;
}
```

#### Scenario: 默认不启用 API 脱敏(forApi = false)
- **GIVEN** 一个 DTO 字段标记了 `@Sensitive(type = SensitiveType.PHONE, forApi = false)` 或省略 `forApi` 属性
- **WHEN** 通过 Spring MVC 接口返回该 DTO 对象
- **THEN** JSON 响应中该字段返回完整的手机号,如 `{"phone": "13812345678"}`
- **AND** 日志打印该对象时仍然脱敏,如 `UserDTO(phone=138****5678)`

#### Scenario: 启用 API 脱敏(forApi = true)
- **GIVEN** 一个 DTO 字段标记了 `@Sensitive(type = SensitiveType.PHONE, forApi = true)`
- **WHEN** 通过 Spring MVC 接口返回该 DTO 对象
- **THEN** JSON 响应中该字段返回脱敏后的手机号,如 `{"phone": "138****5678"}`
- **AND** 日志打印该对象时也脱敏

#### Scenario: 混合配置(部分字段启用,部分不启用)
- **GIVEN** 一个 DTO 有多个敏感字段:
  - `phone` 字段标记 `@Sensitive(type = SensitiveType.PHONE, forApi = true)`
  - `idCard` 字段标记 `@Sensitive(type = SensitiveType.ID_CARD, forApi = false)`
- **WHEN** 通过 Spring MVC 接口返回该 DTO 对象
- **THEN** JSON 响应中 `phone` 字段脱敏,`idCard` 字段完整显示
- **AND** 日志打印时两个字段都脱敏

---

### Requirement: 自动注册 Jackson 序列化模块
项目启动时必须自动注册 Jackson 模块,无需手动配置。

**技术实现**:
- 创建 `SensitiveJsonModule` 继承 `SimpleModule`
- 创建 `SensitiveJsonSerializer` 实现 `ContextualSerializer` 接口
- 通过 Spring Boot 自动配置类自动注册到 `ObjectMapper`

#### Scenario: 添加依赖后自动生效
- **GIVEN** 项目添加了 `sensitive-log-spring-boot-starter` 依赖
- **WHEN** Spring Boot 应用启动
- **THEN** `SensitiveJsonModule` 自动注册到 Jackson `ObjectMapper`
- **AND** 标记了 `@Sensitive(forApi = true)` 的字段在 API 返回时自动脱敏

#### Scenario: 支持全局禁用
- **GIVEN** 配置文件中设置了 `sensitive.log.api-enabled=false`
- **WHEN** Spring Boot 应用启动
- **THEN** `SensitiveJsonModule` 不会注册
- **AND** 所有 `@Sensitive(forApi = true)` 的字段在 API 返回时仍然显示完整数据
- **AND** 日志脱敏功能不受影响,继续正常工作

---

### Requirement: 复用现有脱敏策略
API 脱敏必须复用现有的 `MaskStrategyFactory` 和脱敏策略实现,不重复编写脱敏逻辑。

**实现要求**:
- `SensitiveJsonSerializer` 调用 `MaskStrategyFactory.mask()` 方法
- 支持所有 `SensitiveType` 枚举类型(PHONE, ID_CARD, EMAIL 等)
- 支持自定义 `prefixLength`, `suffixLength`, `maskChar` 参数

#### Scenario: 支持所有内置脱敏类型
- **GIVEN** DTO 中有多个不同类型的敏感字段:
  ```java
  @Sensitive(type = SensitiveType.PHONE, forApi = true)
  private String phone;

  @Sensitive(type = SensitiveType.ID_CARD, forApi = true)
  private String idCard;

  @Sensitive(type = SensitiveType.EMAIL, forApi = true)
  private String email;
  ```
- **WHEN** 通过 API 返回该 DTO
- **THEN** 各字段按照对应类型脱敏:
  - `phone` → `138****5678`
  - `idCard` → `110101********1234`
  - `email` → `t***@example.com`

#### Scenario: 支持自定义脱敏参数
- **GIVEN** DTO 字段标记了自定义脱敏参数:
  ```java
  @Sensitive(
      type = SensitiveType.CUSTOM,
      prefixLength = 2,
      suffixLength = 3,
      maskChar = '#',
      forApi = true
  )
  private String customField = "1234567890";
  ```
- **WHEN** 通过 API 返回该 DTO
- **THEN** JSON 中该字段显示为 `"12###890"`

---

### Requirement: 不影响现有日志脱敏功能
API 脱敏功能的引入不得影响现有的基于 `toString()` 的日志脱敏功能。

#### Scenario: toString 方法继续正常工作
- **GIVEN** 一个 DTO 继承了 `SensitiveEntity` 或实现了 `SensitiveSupport`
- **AND** 该 DTO 的字段标记了 `@Sensitive(forApi = true)`
- **WHEN** 使用日志打印该对象: `log.info("user: {}", user)`
- **THEN** 日志输出中该字段脱敏显示
- **AND** API 返回值也脱敏显示(如果 `forApi = true`)

#### Scenario: 日志脱敏和 API 脱敏独立控制
- **GIVEN** 一个 DTO 字段标记了 `@Sensitive(forApi = false)` 或省略该属性
- **WHEN** 分别通过日志和 API 返回该对象
- **THEN** 日志输出中该字段脱敏
- **AND** API JSON 返回完整数据

---

### Requirement: 向后兼容性
新功能必须保持向后兼容,不影响现有代码行为。

**兼容性要求**:
- `forApi` 属性默认值为 `false`
- 不添加该属性的现有代码行为保持不变
- 可以通过配置完全禁用 API 脱敏功能

#### Scenario: 现有代码无需修改
- **GIVEN** 现有项目使用了该 Starter,但没有设置 `forApi` 属性
- **WHEN** 升级到新版本
- **THEN** 所有 API 返回值行为保持不变,仍然返回完整数据
- **AND** 日志脱敏功能保持不变

#### Scenario: 可选全局禁用
- **GIVEN** 项目升级到新版本
- **AND** 配置了 `sensitive.log.api-enabled=false`
- **WHEN** 所有 API 接口返回 DTO 对象
- **THEN** 即使标记了 `@Sensitive(forApi = true)` 也返回完整数据
- **AND** 日志脱敏功能不受影响

---

### Requirement: 性能要求
API 脱敏功能的性能开销必须最小化。

**性能指标**:
- Jackson 序列化器配置会被缓存,不是每次序列化都重新解析注解
- 只处理标记了 `@Sensitive` 的字段,不影响其他字段的序列化性能
- API 序列化性能损耗应小于 5%

#### Scenario: 序列化器配置缓存
- **GIVEN** 一个 DTO 类有多个字段标记了 `@Sensitive(forApi = true)`
- **WHEN** 多次序列化该 DTO 的不同实例
- **THEN** 字段注解只在第一次序列化时解析一次
- **AND** 后续序列化直接使用缓存配置

#### Scenario: 不影响其他字段
- **GIVEN** 一个 DTO 有 20 个字段,其中只有 2 个标记了 `@Sensitive(forApi = true)`
- **WHEN** 序列化该 DTO
- **THEN** 只有那 2 个字段经过自定义序列化器处理
- **AND** 其他 18 个字段使用 Jackson 默认序列化,无性能损耗
