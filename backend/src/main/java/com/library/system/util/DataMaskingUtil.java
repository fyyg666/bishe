package com.library.system.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

/**
 * 数据脱敏工具类
 * 提供手机号、邮箱、身份证等敏感信息的脱敏功能
 *
 * @author Security Team
 * @version 2.0.0
 */
public class DataMaskingUtil {

    private DataMaskingUtil() {
        // 私有构造函数防止实例化
    }

    /**
     * 手机号脱敏
     * 示例: 13812345678 -> 138****5678
     *
     * @param phone 原始手机号
     * @return 脱敏后的手机号
     */
    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        int length = phone.length();
        return phone.substring(0, 3) + "****" + phone.substring(length - 4);
    }

    /**
     * 邮箱脱敏
     * 示例: test@example.com -> t***@example.com
     *
     * @param email 原始邮箱
     * @return 脱敏后的邮箱
     */
    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        String[] parts = email.split("@");
        if (parts.length != 2 || parts[0].isEmpty()) {
            return email;
        }

        String username = parts[0];
        String domain = parts[1];

        // 用户名只有1位，直接保留
        if (username.length() == 1) {
            return username + "***@" + domain;
        }

        // 用户名保留首尾字符，中间用*替代
        return username.charAt(0) + "***" + username.charAt(username.length() - 1) + "@" + domain;
    }

    /**
     * 身份证脱敏
     * 示例: 310101199001011234 -> 310***********1234
     *
     * @param idCard 原始身份证号
     * @return 脱敏后的身份证号
     */
    public static String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() < 10) {
            return idCard;
        }
        int length = idCard.length();
        return idCard.substring(0, 3) + "***********" + idCard.substring(length - 4);
    }

    /**
     * 密码脱敏
     * 示例: anypassword -> ********
     *
     * @param password 原始密码
     * @return 脱敏后的密码
     */
    public static String maskPassword(String password) {
        if (password == null) {
            return password;
        }
        return "******";
    }

    /**
     * 姓名脱敏
     * 示例: 张三 -> 张*
     * 示例: 欧阳娜娜 -> 欧*娜
     *
     * @param name 原始姓名
     * @return 脱敏后的姓名
     */
    public static String maskName(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        int length = name.length();
        if (length == 1) {
            return name;
        }
        if (length == 2) {
            return name.charAt(0) + "*";
        }
        // 长度大于2时，保留首尾
        return name.charAt(0) + "*" + name.charAt(length - 1);
    }

    /**
     * 银行卡号脱敏
     * 示例: 6222021234567890123 -> 6222**********0123
     *
     * @param bankCard 原始银行卡号
     * @return 脱敏后的银行卡号
     */
    public static String maskBankCard(String bankCard) {
        if (bankCard == null || bankCard.length() < 10) {
            return bankCard;
        }
        int length = bankCard.length();
        return bankCard.substring(0, 4) + "**********" + bankCard.substring(length - 4);
    }

    /**
     * 地址脱敏
     * 只显示省份和城市
     * 示例: 上海市浦东新区张江路123号 -> 上海市浦东新区
     *
     * @param address 原始地址
     * @return 脱敏后的地址
     */
    public static String maskAddress(String address) {
        if (address == null || address.isEmpty()) {
            return address;
        }
        // 查找常见地址分隔符
        String[] separators = {"区", "县", "市"};
        for (String sep : separators) {
            int index = address.indexOf(sep);
            if (index != -1 && index + 1 < address.length()) {
                // 保留区/县/市之后的一位
                int keepIndex = Math.min(index + 2, address.length());
                return address.substring(0, keepIndex) + "***";
            }
        }
        // 如果没有找到分隔符，只保留前6个字符
        if (address.length() > 6) {
            return address.substring(0, 6) + "***";
        }
        return address;
    }

    // ==================== Jackson序列化器 ====================

    /**
     * 手机号序列化器
     * 用于Jackson在JSON序列化时自动脱敏
     */
    public static class PhoneSerializer extends JsonSerializer<String> {
        @Override
        public void serialize(String value, JsonGenerator gen, SerializerProvider serializers)
                throws IOException {
            gen.writeString(maskPhone(value));
        }
    }

    /**
     * 邮箱序列化器
     * 用于Jackson在JSON序列化时自动脱敏
     */
    public static class EmailSerializer extends JsonSerializer<String> {
        @Override
        public void serialize(String value, JsonGenerator gen, SerializerProvider serializers)
                throws IOException {
            gen.writeString(maskEmail(value));
        }
    }

    /**
     * 身份证序列化器
     * 用于Jackson在JSON序列化时自动脱敏
     */
    public static class IdCardSerializer extends JsonSerializer<String> {
        @Override
        public void serialize(String value, JsonGenerator gen, SerializerProvider serializers)
                throws IOException {
            gen.writeString(maskIdCard(value));
        }
    }

    /**
     * 密码序列化器
     * 用于Jackson在JSON序列化时自动脱敏
     */
    public static class PasswordSerializer extends JsonSerializer<String> {
        @Override
        public void serialize(String value, JsonGenerator gen, SerializerProvider serializers)
                throws IOException {
            gen.writeString(maskPassword(value));
        }
    }

    /**
     * 姓名序列化器
     * 用于Jackson在JSON序列化时自动脱敏
     */
    public static class NameSerializer extends JsonSerializer<String> {
        @Override
        public void serialize(String value, JsonGenerator gen, SerializerProvider serializers)
                throws IOException {
            gen.writeString(maskName(value));
        }
    }

    /**
     * 银行卡序列化器
     * 用于Jackson在JSON序列化时自动脱敏
     */
    public static class BankCardSerializer extends JsonSerializer<String> {
        @Override
        public void serialize(String value, JsonGenerator gen, SerializerProvider serializers)
                throws IOException {
            gen.writeString(maskBankCard(value));
        }
    }
}
