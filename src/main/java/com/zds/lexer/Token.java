package com.zds.lexer;

/**
 * 单词符号（Token）= 二元式 <种别码, 属性值>
 */
public class Token {
    public final Token.Type type;   // 种别码
    public final String lexeme;    // 单词原文（属性的一种）
    public final Object literal;   // 字面量值（属性的一种）
    public final int line;         // 行号（从 1 开始）
    public final int col;          // 列号（从 1 开始）

    public Token(Token.Type type, String lexeme, Object literal, int line, int col) {
        this.type = type;
        this.lexeme = lexeme;
        this.literal = literal;
        this.line = line;
        this.col = col;
    }

    @Override
    public String toString() {
        // 二元式风格：<种别码, 属性值>
        String attr = (literal != null) ? String.valueOf(literal) : lexeme;
        return String.format("<%s, %s>  @(%d:%d)", type, attr, line, col);
    }

    /**
     * Token类型类
     */
    public static class Type {
        private final String name;
        public Type(String name) {
            this.name = name;
        }
        @Override
        public String toString() {
            return name;
        }
    }
}