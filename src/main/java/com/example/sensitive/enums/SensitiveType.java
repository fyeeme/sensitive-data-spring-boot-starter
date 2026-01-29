package com.example.sensitive.enums;

/**
 * 敏感数据脱敏类型枚举。
 * <p>
 * 定义了常见的敏感数据类型及其对应的脱敏策略。
 * </p>
 *
 * @author example
 */
public enum SensitiveType {

    /** 手机号码，脱敏示例：138****1234 */
    PHONE,

    /** 身份证号码，脱敏示例：110101********1234 */
    ID_CARD,

    /** 银行卡号，脱敏示例：6222****1234 */
    BANK_CARD,

    /** 电子邮箱，脱敏示例：t***@gmail.com */
    EMAIL,

    /** 姓名，脱敏示例：张* */
    NAME,

    /** 地址，脱敏示例：北京市朝阳区*** */
    ADDRESS,

    /** IP地址，脱敏示例：192.168.*.* */
    IP_ADDRESS,

    /** 通用文本，保留首尾，中间用*代替 */
    TEXT,

    /** 自定义脱敏，需配合 pattern 参数使用 */
    CUSTOM
}
