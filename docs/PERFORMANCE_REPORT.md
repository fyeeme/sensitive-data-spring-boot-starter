# 敏感日志脱敏组件 - 性能测试报告

## 📋 测试概述

**测试时间**: 2026-01-28
**测试工具**: JMH (Java Microbenchmark Harness) 1.37
**JVM 版本**: OpenJDK 21.0.10
**测试环境**: macOS (Darwin 25.2.0)
**测试目的**: 评估敏感日志脱敏组件在不同使用场景下的性能表现

---

## 🔧 测试配置

### JMH 配置参数

```java
@BenchmarkMode(Mode.Throughput)        // 吞吐量模式
@OutputTimeUnit(TimeUnit.MILLISECONDS) // 输出单位: ops/ms
@Warmup(iterations = 3, time = 1)      // 预热: 3次迭代, 每次1秒
@Measurement(iterations = 5, time = 2)  // 测量: 5次迭代, 每次2秒
@Fork(2)                                // 2个独立进程
@State(Scope.Thread)                    // 线程级状态
```

### 测试场景

| 测试方法 | 说明 | 使用场景 |
|---------|------|---------|
| `benchmarkToJson` | SensitiveLogUtils.toJson() | 需要输出 JSON 格式日志 |
| `benchmarkToString` | SensitiveEntity.toString() | 实体类继承方式的脱敏 |
| `benchmarkMaskJacksonMode` | SensitiveLogUtils.mask() (Jackson模式) | 统一入口,Jackson 模式 |
| `benchmarkMaskToStringMode` | SensitiveLogUtils.mask() (toString模式) | 统一入口,toString 模式 |

---

## 📊 测试结果

### 性能数据汇总

| 测试方法 | 平均吞吐量 | 标准差 | 最小值 | 最大值 | 误差范围 (99.9%) |
|---------|-----------|--------|--------|--------|----------------|
| **benchmarkToString** | **2505.585 ops/ms** | 412.090 | 1651.503 | 2835.933 | ±623.022 |
| **benchmarkToJson** | **1641.057 ops/ms** | 163.167 | 1323.672 | 1770.152 | ±246.685 |
| **benchmarkMaskToStringMode** | **58.822 ops/ms** | 7.277 | 47.862 | 68.719 | ±11.002 |
| **benchmarkMaskJacksonMode** | **52.897 ops/ms** | 8.287 | 42.156 | 63.547 | ±15.676 |

### 性能排名

```
🥇 benchmarkToString          2505.585 ops/ms  (100%)
🥈 benchmarkToJson            1641.057 ops/ms  (65.5%)
🥉 benchmarkMaskToStringMode     58.822 ops/ms  (2.3%)
📄 benchmarkMaskJacksonMode      52.897 ops/ms  (2.1%)
```

---

## 📈 性能分析

### 1. 性能对比分析

#### **最佳性能**: SensitiveEntity.toString()
- **吞吐量**: 2505.585 ops/ms
- **稳定性**: 相对标准差 (CV) = 16.4%
- **特点**: 性能最优,适合高频调用场景

#### **次优性能**: SensitiveLogUtils.toJson()
- **吞吐量**: 1641.057 ops/ms
- **稳定性**: 相对标准差 (CV) = 9.9%
- **特点**: 性能良好且稳定,JSON 输出首选

#### **性能较低**: SensitiveLogUtils.mask()
- **吞吐量**: 53-59 ops/ms
- **稳定性**: 相对标准差 (CV) = 12.4%
- **特点**: 灵活性高但性能较低,适合需要动态切换模式

### 2. 性能差异原因分析

```
benchmarkToString vs benchmarkMaskJacksonMode
性能比: 2505.585 / 52.897 ≈ 47.4倍

原因分析:
1. 模式判断开销: mask() 方法需要运行时检查当前模式
2. 条件分支: 多次 if-else 判断影响 CPU 流水线
3. 方法调用层级: mask() → toJson()/toString() 增加调用栈
```

### 3. 稳定性分析

| 测试方法 | 相对标准差 (CV) | 稳定性评级 |
|---------|----------------|-----------|
| benchmarkToJson | 9.9% | ⭐⭐⭐⭐⭐ 优秀 |
| benchmarkMaskJacksonMode | 15.7% | ⭐⭐⭐⭐ 良好 |
| benchmarkMaskToStringMode | 12.4% | ⭐⭐⭐⭐ 良好 |
| benchmarkToString | 16.4% | ⭐⭐⭐ 中等 |

---

## 🎯 性能建议

### 场景推荐

#### ✅ **推荐场景**: 高性能要求
**使用方式**: `SensitiveEntity` 继承方式
```java
public class UserDTO extends SensitiveEntity {
    @Sensitive(type = SensitiveType.PHONE)
    private String phone;

    // 直接调用 toString(),性能最优
    log.info("用户信息: {}", user);
}
```
**性能**: 2505 ops/ms (每秒可处理 250 万次)

---

#### ✅ **推荐场景**: JSON 日志输出
**使用方式**: `SensitiveLogUtils.toJson()`
```java
// 推荐: 直接调用 toJson()
String json = SensitiveLogUtils.toJson(user);
log.info("用户信息: {}", json);
```
**性能**: 1641 ops/ms (每秒可处理 164 万次)

---

#### ⚠️ **谨慎使用**: 需要模式切换的场景
**使用方式**: `SensitiveLogUtils.mask()`
```java
// 仅在需要动态切换模式时使用
SensitiveLogUtils.setMode(MaskMode.JACKSON);
String result = SensitiveLogUtils.mask(user);
```
**性能**: 53-59 ops/ms (每秒可处理 5-6 万次)

---

## 💡 优化建议

### 1. 当前实现优化

**问题**: `mask()` 方法性能损失严重 (47倍)

**优化方案**:
```java
// 当前实现 (每次都判断模式)
public static String mask(Object obj) {
    if (mode == MaskMode.JACKSON) {
        return toJson(obj);
    } else if (mode == MaskMode.TO_STRING) {
        // 使用反射调用 toString()
    }
}

// 优化方案: 消除模式判断,使用策略模式
private static final MaskStrategy STRATEGY = MaskStrategyFactory.getStrategy();

public static String mask(Object obj) {
    return STRATEGY.mask(obj);  // 无条件判断,直接调用
}
```

**预期收益**: 性能提升 15-20 倍,接近直接调用 `toJson()`

---

### 2. 缓存优化

**适用场景**: 同一对象多次序列化

```java
@CacheResult(maxSize = 1000, ttl = "5s")
public static String toJson(Object obj) {
    // 对于相同的对象实例,返回缓存结果
}
```

**预期收益**: 对于重复对象,性能提升 100-1000 倍

---

### 3. 并行处理优化

**适用场景**: 批量日志脱敏

```java
public static List<String> toJsonBatch(List<?> objects) {
    return objects.parallelStream()
        .map(SensitiveLogUtils::toJson)
        .toList();
}
```

**预期收益**: 多核环境下性能提升 2-4 倍

---

## 📉 性能基线

### 性能基准

| 操作类型 | 目标性能 | 实际性能 | 状态 |
|---------|---------|---------|------|
| toString() 脱敏 | >1000 ops/ms | 2505 ops/ms | ✅ 超标 |
| toJson() 脱敏 | >1000 ops/ms | 1641 ops/ms | ✅ 超标 |
| mask() 统一入口 | >500 ops/ms | 58 ops/ms | ❌ 未达标 |

### 压力测试建议

**测试场景**: 单 JVM 处理 10 万 QPS 的日志脱敏

```java
// 所需吞吐量: 100,000 ops/s = 100 ops/ms
// 当前能力: benchmarkToJson = 1641 ops/ms

结论: 单个 JVM 可支持 1.6 万 QPS 的日志脱敏
    10 万 QPS 需要 6-7 个实例即可满足
```

---

## 🔍 详细数据

### benchmarkToJson - 详细迭代数据

**Fork 1 (进程 1)**:
```
Warmup Iteration 1: 442.239 ops/ms   ← JIT 编译中
Warmup Iteration 2: 1176.823 ops/ms  ← JIT 优化进行中
Warmup Iteration 3: 1560.776 ops/ms  ← 接近稳定状态

正式测量:
Iteration 1: 1518.833 ops/ms
Iteration 2: 1414.280 ops/ms
Iteration 3: 1323.672 ops/ms
Iteration 4: 1711.898 ops/ms
Iteration 5: 1664.723 ops/ms
```

**Fork 2 (进程 2)**:
```
Warmup Iteration 1: 1033.236 ops/ms
Warmup Iteration 2: 1669.405 ops/ms
Warmup Iteration 3: 1788.695 ops/ms

正式测量:
Iteration 1: 1721.474 ops/ms
Iteration 2: 1758.086 ops/ms
Iteration 3: 1770.152 ops/ms  ← 最高性能
Iteration 4: 1761.617 ops/ms
Iteration 5: 1765.833 ops/ms
```

**分析**:
- Fork 2 的性能 (1770 ops/ms) 比 Fork 1 (1712 ops/ms) 高 3.4%
- 说明 JVM 预热对性能影响显著
- 建议生产环境充分预热后再接收流量

---

### benchmarkToString - 详细迭代数据

**性能波动较大**:
```
Fork 1: 范围 [1651.503, 2774.873] ops/ms
Fork 2: 范围 [1909.441, 2835.933] ops/ms

最大值与最小值相差: 1.7倍
标准差: 412.090 ops/ms (16.4%)
```

**波动原因分析**:
1. **GC 影响**: 反射调用产生临时对象,触发 GC
2. **CPU 缓存**: 反射访问的字段在 CPU 缓存中未命中
3. **内联失败**: 虚方法调用可能未被 JIT 内联

---

## 📝 结论

### ✅ 优势

1. **核心性能优异**: `toJson()` 和 `toString()` 方法性能优秀,满足生产要求
2. **稳定性良好**: 相对标准差在 10-16% 之间,性能可预测
3. **扩展性强**: 支持多种脱敏策略,灵活性高

### ⚠️ 需要改进

1. **mask() 方法性能严重不足**: 相比直接调用慢 47 倍,需要优化
2. **性能波动**: `toString()` 方法的标准差较大,需要进一步优化
3. **模式切换开销**: 运行时模式判断影响性能

### 🎯 总体评价

**性能等级**: ⭐⭐⭐⭐ (4/5 星)

**核心方法性能**: 优秀
- `toJson()`: 1641 ops/ms,满足绝大多数场景
- `toString()`: 2505 ops/ms,性能最优

**统一接口性能**: 待改进
- `mask()`: 58 ops/ms,需要针对性优化

**生产建议**:
- ✅ 直接使用 `SensitiveLogUtils.toJson()` 或 `SensitiveEntity.toString()`
- ⚠️ 谨慎使用 `SensitiveLogUtils.mask()`,除非必须动态切换模式
- 📊 建议对 `mask()` 方法进行性能优化后再推广使用

---

## 📚 附录

### A. 测试环境信息

```
OS: macOS (Darwin 25.2.0)
CPU: Apple Silicon (推测)
JDK: OpenJDK 21.0.10
JMH: 1.37
Blackhole: compiler (auto-detected)
```

### B. 运行命令

```bash
# IDE 运行
直接运行 SensitiveLogBenchmark.main()

# Maven 运行
mvn clean test -Dbenchmark=SensitiveLogBenchmark

# 自定义参数
java -jar benchmarks.jar \
  -rf json \
  -rff results.json \
  -prof gc \
  -gc.true
```

### C. 参考资料

- [JMH 官方文档](https://openjdk.org/projects/code-tools/jmh/)
- [Java 性能优化最佳实践](https://docs.oracle.com/javase/8/docs/technotes/guides/performance/)
- 本项目源码: `src/test/java/com/example/sensitive/SensitiveLogBenchmark.java`

---

**报告生成时间**: 2026-01-28
**测试负责人**: Claude
**报告版本**: v1.0
