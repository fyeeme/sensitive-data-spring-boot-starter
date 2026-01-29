package com.example.sensitive.integration;

import com.example.sensitive.annotation.Sensitive;
import com.example.sensitive.enums.SensitiveType;
import com.example.sensitive.support.SensitiveEntity;

/**
 * 集成测试用的 DTO 类
 * <p>
 * 包含混合配置:
 * <ul>
 *   <li>phone: forApi = true (API 脱敏)</li>
 *   <li>idCard: forApi = false (API 不脱敏)</li>
 *   <li>email: 省略 forApi (默认 false,API 不脱敏)</li>
 *   <li>name: forApi = true (API 脱敏)</li>
 *   <li>bankCard: forApi = true (API 脱敏)</li>
 * </ul>
 */
public class TestDto extends SensitiveEntity {

    private Long id;

    @Sensitive(type = SensitiveType.PHONE, forApi = true)
    private String phone;

    @Sensitive(type = SensitiveType.ID_CARD, forApi = false)
    private String idCard;

    @Sensitive(type = SensitiveType.EMAIL) // 默认 forApi = false
    private String email;

    @Sensitive(type = SensitiveType.NAME, forApi = true)
    private String name;

    @Sensitive(type = SensitiveType.BANK_CARD, forApi = true)
    private String bankCard;

    public TestDto() {
    }

    public TestDto(Long id, String phone, String idCard, String email, String name, String bankCard) {
        this.id = id;
        this.phone = phone;
        this.idCard = idCard;
        this.email = email;
        this.name = name;
        this.bankCard = bankCard;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getIdCard() {
        return idCard;
    }

    public void setIdCard(String idCard) {
        this.idCard = idCard;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBankCard() {
        return bankCard;
    }

    public void setBankCard(String bankCard) {
        this.bankCard = bankCard;
    }
}
