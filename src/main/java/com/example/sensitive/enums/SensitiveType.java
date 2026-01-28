package com.example.sensitive.enums;

/**
 * 敏感数据类型枚举
 * 
 * @author example
 */
public enum SensitiveType {
    
    /**
     * 手机号: 138****1234
     */
    PHONE,
    
    /**
     * 身份证号: 110101********1234
     */
    ID_CARD,
    
    /**
     * 银行卡号: 6222****1234
     */
    BANK_CARD,
    
    /**
     * 邮箱: t***@gmail.com
     */
    EMAIL,
    
    /**
     * 姓名: 张*
     */
    NAME,
    
    /**
     * 地址: 北京市朝阳区***
     */
    ADDRESS,
    
    /**
     * 密码: ******
     */
    PASSWORD,
    
    /**
     * 车牌号: 京A****8
     */
    CAR_NUMBER,
    
    /**
     * IP地址: 192.168.*.*
     */
    IP_ADDRESS,
    
    /**
     * 自定义（需配合 pattern 使用）
     */
    CUSTOM,
    
    /**
     * 默认: 首尾保留，中间*
     */
    DEFAULT
}
