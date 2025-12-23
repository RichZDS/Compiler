package com.zds.lexer.util;

/**
 * 词法分析器工具类
 */
public class LexerUtils {
    /**
     * 判断字符是否为数字
     */
    public static boolean isDigit(char c) { return c >= '0' && c <= '9'; }

    /**
     * 判断字符是否为字母或下划线
     */
    public static boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_';
    }

    /**
     * 判断字符是否为字母、数字或下划线
     */
    public static boolean isAlphaNumeric(char c) { return isAlpha(c) || isDigit(c); }
}