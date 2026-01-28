package com.example.sensitive;

import com.example.sensitive.annotation.Sensitive;
import com.example.sensitive.config.SensitiveLogProperties.MaskMode;
import com.example.sensitive.enums.SensitiveType;
import com.example.sensitive.support.SensitiveEntity;
import com.example.sensitive.util.SensitiveLogUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * 敏感日志脱敏 JMH 基准测试
 *
 * 运行方式:
 * 1. IDE: 直接运行 main 方法
 * 2. Maven: mvn clean test -Dbenchmark=SensitiveLogBenchmark
 * 3. JAR: 打包后运行 java -jar benchmarks.jar
 *
 * @author Claude
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Fork(2)
@State(Scope.Thread)
public class SensitiveLogBenchmark {

    private UserDTO userDTO;
    private UserEntityDTO userEntity;

    @Setup
    public void setup() {
        // 初始化 ObjectMapper
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

        // 初始化工具类
        SensitiveLogUtils.init(mapper, MaskMode.BOTH);

        // 准备测试数据
        userDTO = new UserDTO();
        userDTO.setId(1L);
        userDTO.setUsername("testuser");
        userDTO.setPhone("13812345678");
        userDTO.setIdCard("110101199001011234");
        userDTO.setEmail("test@example.com");
        userDTO.setBankCard("6222021234567890123");
        userDTO.setRealName("张三丰");
        userDTO.setPassword("secret123");
        userDTO.setCreateTime(LocalDateTime.now());

        userEntity = new UserEntityDTO();
        userEntity.setId(1L);
        userEntity.setPhone("13812345678");
        userEntity.setIdCard("110101199001011234");
        userEntity.setRealName("张三丰");
    }

    /**
     * 基准测试: SensitiveLogUtils.toJson()
     * 测试通过 Jackson 序列化进行脱敏的性能
     */
    @Benchmark
    public void benchmarkToJson(Blackhole bh) {
        String result = SensitiveLogUtils.toJson(userDTO);
        bh.consume(result);
    }

    /**
     * 基准测试: SensitiveEntity.toString()
     * 测试通过反射进行 toString 脱敏的性能
     */
    @Benchmark
    public void benchmarkToString(Blackhole bh) {
        String result = userEntity.toString();
        bh.consume(result);
    }

    /**
     * 基准测试: SensitiveLogUtils.mask() - Jackson 模式
     */
    @Benchmark
    public void benchmarkMaskJacksonMode(Blackhole bh) {
        SensitiveLogUtils.setMode(MaskMode.JACKSON);
        String result = SensitiveLogUtils.mask(userDTO);
        bh.consume(result);
    }

    /**
     * 基准测试: SensitiveLogUtils.mask() - toString 模式
     */
    @Benchmark
    public void benchmarkMaskToStringMode(Blackhole bh) {
        SensitiveLogUtils.setMode(MaskMode.TO_STRING);
        String result = SensitiveLogUtils.mask(userDTO);
        bh.consume(result);
    }

    /**
     * 主方法: 用于直接运行基准测试
     */
    public static void main(String[] args) throws Exception {
        org.openjdk.jmh.Main.main(args);
    }

    // ==================== 测试用 DTO ====================

    /**
     * 测试用 DTO（普通类）
     */
    public static class UserDTO {

        private Long id;
        private String username;

        @Sensitive(type = SensitiveType.PHONE)
        private String phone;

        @Sensitive(type = SensitiveType.ID_CARD)
        private String idCard;

        @Sensitive(type = SensitiveType.EMAIL)
        private String email;

        @Sensitive(type = SensitiveType.BANK_CARD)
        private String bankCard;

        @Sensitive(type = SensitiveType.NAME)
        private String realName;

        @Sensitive(type = SensitiveType.PASSWORD)
        private String password;

        private LocalDateTime createTime;

        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getIdCard() { return idCard; }
        public void setIdCard(String idCard) { this.idCard = idCard; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getBankCard() { return bankCard; }
        public void setBankCard(String bankCard) { this.bankCard = bankCard; }
        public String getRealName() { return realName; }
        public void setRealName(String realName) { this.realName = realName; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public LocalDateTime getCreateTime() { return createTime; }
        public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    }

    /**
     * 测试用 DTO（继承 SensitiveEntity）
     */
    public static class UserEntityDTO extends SensitiveEntity {

        private Long id;

        @Sensitive(type = SensitiveType.PHONE)
        private String phone;

        @Sensitive(type = SensitiveType.ID_CARD)
        private String idCard;

        @Sensitive(type = SensitiveType.NAME)
        private String realName;

        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getIdCard() { return idCard; }
        public void setIdCard(String idCard) { this.idCard = idCard; }
        public String getRealName() { return realName; }
        public void setRealName(String realName) { this.realName = realName; }
    }
}
