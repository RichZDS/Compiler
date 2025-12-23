package com.zds.lexer.consts;

import com.zds.lexer.Token;

/**
 * Token 种别码（PPT 常叫：种别码 / 类别码）
 */
public class TokenType {
    // 定义类型别名，简化引用
    private static final class Type extends Token.Type {
        public Type(String name) {
            super(name);
        }
    }
    
    // 关键字
    public static final Type IF = new Type("IF");     // if
    public static final Type ELSE = new Type("ELSE");   // else
    public static final Type FOR = new Type("FOR");    // for
    public static final Type WHILE = new Type("WHILE");  // while
    public static final Type INT = new Type("INT");    // int
    public static final Type DOUBLE = new Type("DOUBLE"); // double
    public static final Type STRING = new Type("STRING"); // string

    // 标识符 & 字面量
    public static final Type IDENT = new Type("IDENT");      // 标识符
    public static final Type INT_LIT = new Type("INT_LIT");    // 整数字面量
    public static final Type DOUBLE_LIT = new Type("DOUBLE_LIT"); // 双精度浮点数字面量
    public static final Type STRING_LIT = new Type("STRING_LIT"); // 字符串字面量

    // 运算符
    public static final Type PLUS = new Type("PLUS");  // +
    public static final Type MINUS = new Type("MINUS"); // -
    public static final Type MUL = new Type("MUL");   // *
    public static final Type DIV = new Type("DIV");   // /
    public static final Type ASSIGN = new Type("ASSIGN"); // =
    public static final Type GT = new Type("GT");     // >
    public static final Type GE = new Type("GE");     // >=
    public static final Type LT = new Type("LT");     // <
    public static final Type LE = new Type("LE");     // <=
    public static final Type EQ = new Type("EQ");     // ==
    public static final Type NE = new Type("NE");     // !=

    // 分隔符
    public static final Type LPAREN = new Type("LPAREN");  // (
    public static final Type RPAREN = new Type("RPAREN");  // )
    public static final Type LBRACE = new Type("LBRACE");  // {
    public static final Type RBRACE = new Type("RBRACE");  // }
    public static final Type SEMI = new Type("SEMI");    // ;
    public static final Type COMMA = new Type("COMMA");   // ,

    // 文件结束
    public static final Type EOF = new Type("EOF");   // 文件结束符

    // 错误（不会阻止继续扫描，但会记录 errors）
    public static final Type ERROR = new Type("ERROR");  // 错误标记
}