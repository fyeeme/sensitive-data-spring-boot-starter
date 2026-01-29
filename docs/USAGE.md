# 敏感日志脱敏 Starter 使用文档

## 一、核心概念

基于 `toString()` 实现的日志脱敏方案，**支持日志和 API 返回值脱敏**。

### 1.1 日志脱敏 (默认行为)

```java
// 日志打印 - 自动脱敏
log.info("用户: {}", user);
// 输出: UserDTO(phone=138****5678)

// API 返回 - 完整数据（forApi = false,默认）
@GetMapping("/user")
public UserDTO getUser() {
    return user;  // JSON: {"phone":"13812345678"}
}
```

### 1.2 🆕 API 返回值脱敏 (新功能)

```java
// 启用 API 脱敏 - 设置 forApi = true
@Sensitive(type = SensitiveType.PHONE, forApi = true)
private String phone;

// 日志打印 - 脱敏
log.info("用户: {}", user);
// 输出: UserDTO(phone=138****5678)

// API 返回 - 也脱敏！
@GetMapping("/user")
public UserDTO getUser() {
    return user;  // JSON: {"phone":"138****5678"}
}
```

**关键区别:**
- `forApi = false`: 仅日志脱敏，API 返回完整数据 (默认)
- `forApi = true`: 日志和 API 都脱敏

---

## 二、快速开始

### 1. 标记敏感字段

```java
import com.example.sensitive.annotation.Sensitive;
import com.example.sensitive.enums.SensitiveType;

public class UserDTO {
    private Long id;

    @Sensitive(type = SensitiveType.PHONE)      // 手机号: 138****5678
    private String phone;

    @Sensitive(type = SensitiveType.ID_CARD)     // 身份证: 110101********1234
    private String idCard;

    @Sensitive(type = SensitiveType.EMAIL)       // 邮箱: t***@example.com
    private String email;

    @Sensitive(type = SensitiveType.BANK_CARD)   // 银行卡: 6222***********0123
    private String bankCard;

    @Sensitive(type = SensitiveType.NAME)        // 姓名: 张*丰
    private String realName;
}
```

### 2. 启用脱敏（三种方式）

---

## 三、使用方式

### 方式 1️⃣：继承 SensitiveEntity（最简单 ⭐推荐）

```java
import com.example.sensitive.support.SensitiveEntity;

@Getter
@Setter
public class UserDTO extends SensitiveEntity {
    @Sensitive(type = SensitiveType.PHONE)
    private String phone;
}

// 直接打印日志，自动脱敏
log.info("用户: {}", user);
```

**优点**：零代码，继承即可
**缺点**：占用继承位置

---

### 方式 2️⃣：实现 SensitiveSupport 接口

```java
import com.example.sensitive.support.SensitiveSupport;

@Getter
@Setter
public class UserDTO extends BaseEntity implements SensitiveSupport {
    @Sensitive(type = SensitiveType.PHONE)
    private String phone;

    @Override
    public String toString() {
        return toSensitiveString();  // 调用接口默认方法
    }
}
```

**优点**：不占用继承位置
**缺点**：需要手动覆写 `toString()`

---

### 方式 3⃣：结合 Builder 模式

```java
@Getter
@Setter
@Builder
public class UserDTO extends SensitiveEntity {
    @Sensitive(type = SensitiveType.PHONE)
    private String phone;
}

// 使用
UserDTO user = UserDTO.builder()
    .id(1L)
    .phone("13812345678")
    .build();

log.info("用户: {}", user);  // 自动脱敏
```

---

### 🆕 方式 4⃣：API 返回值脱敏 (新功能)

```java
@Getter
@Setter
public class UserDTO extends SensitiveEntity {

    @Sensitive(type = SensitiveType.PHONE, forApi = true)  // ⭐ 启用 API 脱敏
    private String phone;

    @Sensitive(type = SensitiveType.ID_CARD)  // 默认 forApi = false
    private String idCard;
}

// 日志打印 - 都脱敏
log.info("用户: {}", user);
// 输出: UserDTO(phone=138****5678, idCard=110101********1234)

// API 返回 - phone 脱敏, idCard 完整
@GetMapping("/user")
public UserDTO getUser() {
    return user;
    // JSON: {"phone":"138****5678","idCard":"110101199001011234"}
}
```

**优点**：
- ✅ 零配置：无需任何配置类
- ✅ 灵活控制：字段级别的细粒度控制
- ✅ 框架无关：不依赖 Spring Boot 自动配置

**缺点**：
- 需要显式标记 `forApi = true`

**适用场景**：
- 对外接口需要保护用户隐私
- 管理后台 API 不应返回完整敏感信息
- 需要灵活控制哪些字段在 API 响应中脱敏

---

## 四、高级用法

### 1. 选择性脱敏

```java
// 只包含指定字段
String result = user.toSensitiveStringWith("id", "phone");
// 输出: UserDTO(id=1, phone=138****5678)

// 排除指定字段
String result = user.toSensitiveStringWithout("password");
// 输出: UserDTO(id=1, phone=138****5678, ...)
```

### 2. 临时禁用脱敏

```java
try (var scope = SensitiveContext.disable()) {
    log.debug("完整用户信息: {}", user);  // 不脱敏
}
```

---

## 五、支持的脱敏类型

| 类型 | 枚举 | 示例 |
|------|------|------|
| 手机号 | `PHONE` | `138****5678` |
| 身份证 | `ID_CARD` | `110101********1234` |
| 银行卡 | `BANK_CARD` | `6222***********0123` |
| 邮箱 | `EMAIL` | `t***@example.com` |
| 姓名 | `NAME` | `张*丰` |
| 地址 | `ADDRESS` | `北京市朝阳区***` |
| IP地址 | `IP_ADDRESS` | `192.168.*.*` |
| 自定义 | `CUSTOM` | 自定义前后保留长度 |

---

## 六、选择建议

| 场景 | 推荐方式 |
|------|---------|
| 实体类无父类 | **方式1：继承 SensitiveEntity** ⭐ |
| 已有父类 | 方式2：实现 SensitiveSupport |
| 需要自定义格式 | 方式3：@Exclude + @Include |
| 完全控制 | 方式4：禁用 Lombok + 手动覆写 |
| 使用 Builder | 方式5：结合 Builder 模式 |

---

## 七、原理说明

### 核心原理

```
日志打印: log.info("{}", user)
    ↓
调用 user.toString()
    ↓
SensitiveEntity/SensitiveSupport 提供 toString()
    ↓
SensitiveToStringBuilder 反射扫描字段
    ↓
发现 @Sensitive 注解
    ↓
调用 MaskStrategyFactory 执行脱敏
    ↓
返回脱敏后的字符串
```

### 为什么不影响 API 返回？

```
API 返回: @GetMapping("/user")
    ↓
Spring MVC 使用 Jackson 序列化
    ↓
调用 user.getPhone() (不是 toString())
    ↓
返回完整数据
```

**关键**：
- 日志使用 `toString()` → 脱敏
- API 使用 getter → 完整数据
- 两者互不影响

---

## 八、完整示例

### 示例 1：继承方式

```java
@Getter
@Setter
public class UserDTO extends SensitiveEntity {
    private Long id;
    private String username;

    @Sensitive(type = SensitiveType.PHONE)
    private String phone;

    @Sensitive(type = SensitiveType.ID_CARD)
    private String idCard;

    @Sensitive(type = SensitiveType.NAME)
    private String realName;
}

// 使用
UserDTO user = new UserDTO();
user.setId(1L);
user.setPhone("13812345678");
user.setIdCard("110101199001011234");
user.setRealName("张三丰");

log.info("用户信息: {}", user);
// 输出: UserDTO(id=1, username=null, phone=138****5678, idCard=110101********1234, realName=张*丰)
```

### 示例 2：接口方式

```java
@Getter
@Setter
public class OrderDTO extends BaseEntity implements SensitiveSupport {
    private Long orderId;

    @Sensitive(type = SensitiveType.PHONE)
    private String customerPhone;

    @Sensitive(type = SensitiveType.BANK_CARD)
    private String bankCard;

    @Override
    public String toString() {
        return toSensitiveString();
    }
}

// 使用
OrderDTO order = new OrderDTO();
order.setOrderId(1001L);
order.setCustomerPhone("13987654321");
order.setBankCard("6222021234567890123");

log.info("订单信息: {}", order);
// 输出: OrderDTO(orderId=1001, customerPhone=139****4321, bankCard=6222***********0123)
```

---

## 九、常见问题

### Q1: 为什么 API 返回的数据没有脱敏？

**A**: 这是设计使然。本方案只为**日志打印**提供脱敏，不影响业务 API 返回。如果需要 API 返回也脱敏，需要使用 Jackson 序列化方案。

### Q2: 可以自定义脱敏规则吗？

**A**: 可以。使用 `CUSTOM` 类型：

```java
@Sensitive(type = SensitiveType.CUSTOM, prefixLength = 2, suffixLength = 3)
private String value;
// 1234567890 → 12*****890
```

### Q3: 如何临时禁用脱敏？

**A**: 使用 `SensitiveContext.disable()`：

```java
try (var scope = SensitiveContext.disable()) {
    log.info("完整信息: {}", user);  // 不脱敏
}
```

### Q4: Lombok 的 `@Getter
@Setter` 会冲突吗？

**A**: 不会。`@Getter
@Setter` 生成的 `toString()` 会被 `SensitiveEntity` 覆盖（方式1），或者需要手动禁用（方式2、4）。

---

**文档版本**: 1.0.0
**最后更新**: 2026-01-28
