# Sensitive Log Spring Boot Starter

高性能日志脱敏组件，基于 Jackson 序列化实现，零日志框架侵入。

## 特性

- ✅ **零侵入**：日志框架无需任何改动
- ✅ **高性能**：无正则、有缓存、O(1) 策略查找
- ✅ **易扩展**：支持自定义脱敏策略
- ✅ **灵活控制**：支持线程级脱敏开关
- ✅ **多种方案**：Jackson / toString / 混合模式可配置切换
- ✅ **API 无影响**：toString 方案不影响接口返回值

## 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>sensitive-log-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 2. 选择使用方案

#### 方案一：继承 SensitiveEntity（推荐 ⭐⭐⭐⭐⭐）

**最简单，直接打印对象即可，不影响 API 返回！**

```java
@Data
public class UserDTO extends SensitiveEntity {
    
    private Long id;
    
    @Sensitive(type = SensitiveType.PHONE)
    private String phone;
    
    @Sensitive(type = SensitiveType.ID_CARD)
    private String idCard;
}

// 日志打印（自动脱敏）
log.info("用户: {}", user);
// 输出: 用户: UserDTO(id=1, phone=138****5678, idCard=110101********1234)

// API 返回（不脱敏，返回完整数据）
@GetMapping("/user")
public UserDTO getUser() {
    return user;  // ✅ JSON 返回完整数据
}
```

#### 方案二：实现接口（已有父类时使用）

```java
@Data
public class UserDTO extends BaseEntity implements SensitiveToStringSupport {
    
    @Sensitive(type = SensitiveType.PHONE)
    private String phone;
    
    @Override
    public String toString() {
        return toSensitiveString();  // 调用接口默认方法
    }
}
```

#### 方案三：配合 Lombok 使用

```java
@Data
@ToString(onlyExplicitlyIncluded = true)  // 禁用 Lombok 自动 toString
public class UserDTO {
    
    @ToString.Include
    private Long id;
    
    @Sensitive(type = SensitiveType.PHONE)
    private String phone;
    
    @Override
    public String toString() {
        return SensitiveToStringBuilder.build(this);
    }
}
```

#### 方案四：Jackson 序列化（需要 JSON 格式时）

```java
@Data
public class UserDTO {
    
    @Sensitive(type = SensitiveType.PHONE)
    private String phone;
}

// 使用 SensitiveLogUtils
log.info("用户: {}", SensitiveLogUtils.toJson(user));
// 输出: 用户: {"id":1,"phone":"138****5678"}
```

---

## 为什么 toString() 不影响 API 返回？

```
┌─────────────────────────────────────────────────────────────────┐
│                    Spring MVC 接口返回                           │
│  @ResponseBody → Jackson ObjectMapper.writeValueAsString()      │
│  ✅ 调用 getter 方法获取值                                        │
│  ❌ 不调用 toString()                                            │
│  结果：返回完整数据                                               │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                        日志打印                                   │
│  log.info("user: {}", user)                                     │
│  ✅ SLF4J 调用 user.toString()                                   │
│  结果：返回脱敏数据                                               │
└─────────────────────────────────────────────────────────────────┘
```

---

## 配置项

```yaml
sensitive:
  log:
    # 是否启用脱敏（默认 true）
    enabled: true
    
    # 脱敏模式（默认 BOTH）
    # - JACKSON: 使用 Jackson 序列化脱敏
    # - TO_STRING: 使用 toString() 脱敏
    # - BOTH: 同时支持两种方式
    mode: BOTH
    
    # 默认掩码字符（默认 *）
    mask-char: '*'
    
    # toString 配置
    to-string-config:
      # 是否启用反射缓存（默认 true）
      enable-cache: true
      # 缓存最大容量（默认 1000）
      cache-max-size: 1000
```

---

## 支持的脱敏类型

| 类型 | 说明 | 示例 |
|------|------|------|
| `PHONE` | 手机号 | `138****5678` |
| `ID_CARD` | 身份证 | `110101********1234` |
| `BANK_CARD` | 银行卡 | `6222****0123` |
| `EMAIL` | 邮箱 | `t***@example.com` |
| `NAME` | 姓名 | `张*丰` |
| `ADDRESS` | 地址 | `北京市朝阳区***` |
| `PASSWORD` | 密码 | `******` |
| `CAR_NUMBER` | 车牌号 | `京A****8` |
| `IP_ADDRESS` | IP地址 | `192.168.*.*` |
| `CUSTOM` | 自定义 | 根据配置 |
| `DEFAULT` | 默认 | `a*****z` |

---

## 高级用法

### 自定义脱敏规则

```java
// 自定义前后保留长度
@Sensitive(type = SensitiveType.CUSTOM, prefixLength = 2, suffixLength = 3)
private String customField;
// "1234567890" -> "12*****890"

// 自定义掩码字符
@Sensitive(type = SensitiveType.PHONE, maskChar = '#')
private String phone;
// "13812345678" -> "138####5678"
```

### 临时禁用脱敏

```java
// 方式1：try-with-resources
try (var scope = SensitiveLogUtils.disableMask()) {
    log.debug("完整数据: {}", SensitiveLogUtils.toJson(user));
}

// 方式2：直接调用
String rawJson = SensitiveLogUtils.toRawJson(user);
```

### 运行时切换模式

```java
// 切换到纯 toString 模式
SensitiveLogUtils.setMode(MaskMode.TO_STRING);

// 切换到纯 Jackson 模式
SensitiveLogUtils.setMode(MaskMode.JACKSON);
```

### 选择性输出字段

```java
// 只输出指定字段
String result = SensitiveToStringBuilder.buildWith(user, "id", "phone");

// 排除指定字段
String result = SensitiveToStringBuilder.buildWithout(user, "password");
```

---

## 方案对比

| 方案 | 代码量 | 灵活性 | API影响 | 推荐场景 |
|------|--------|--------|---------|----------|
| 继承 SensitiveEntity | ⭐ 最少 | 中 | ❌ 无 | 新项目首选 |
| 实现 SensitiveToStringSupport | ⭐⭐ 少 | 高 | ❌ 无 | 已有父类 |
| Lombok 配合 | ⭐⭐⭐ 中 | 最高 | ❌ 无 | 需要细粒度控制 |
| SensitiveLogUtils.toJson() | ⭐⭐ 少 | 高 | ❌ 无 | 需要 JSON 格式 |

---

## 架构图

```
┌─────────────────────────────────────────────────────────────────┐
│                         业务代码                                 │
│                                                                 │
│  方案1: log.info("user: {}", user);        // toString 脱敏     │
│  方案2: log.info("user: {}", SensitiveLogUtils.toJson(user));  │
└─────────────────────────────────────────────────────────────────┘
                          │
          ┌───────────────┴───────────────┐
          ▼                               ▼
┌─────────────────────┐       ┌─────────────────────────┐
│  SensitiveEntity    │       │   SensitiveLogUtils     │
│  toString()         │       │   toJson()              │
└─────────────────────┘       └─────────────────────────┘
          │                               │
          ▼                               ▼
┌─────────────────────┐       ┌─────────────────────────┐
│ SensitiveToString   │       │   SensitiveSerializer   │
│ Builder             │       │   (Jackson)             │
└─────────────────────┘       └─────────────────────────┘
          │                               │
          └───────────────┬───────────────┘
                          ▼
               ┌─────────────────────┐
               │ MaskStrategyFactory │
               │  EnumMap O(1) 查找   │
               └─────────────────────┘
                          │
                          ▼
               ┌─────────────────────┐
               │   Log4j2/Logback    │
               │   直接输出已脱敏内容  │
               └─────────────────────┘
```

---

## 性能数据

测试环境：JDK 17, MacBook Pro M1

| 操作 | 吞吐量 |
|------|--------|
| SensitiveEntity.toString() | ~8,000 ops/ms |
| SensitiveLogUtils.toJson() | ~5,000 ops/ms |
| 单字段脱敏 | ~15,000 ops/ms |

---

## 注意事项

1. **toString 方案不影响 API 返回**：Spring MVC 使用 Jackson 序列化，调用 getter 而非 toString()

2. **线程安全**：所有组件都是线程安全的

3. **空值处理**：null 值直接输出 `null`，不会报错

4. **嵌套对象**：Jackson 方案支持嵌套对象递归脱敏

5. **Lombok 兼容**：可配合 `@Data`、`@Builder` 等注解使用

## License

MIT License
