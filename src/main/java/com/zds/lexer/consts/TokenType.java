package com.zds.lexer.consts;

import com.zds.lexer.Token;

/**
 * Token 种别码（PPT 常叫：种别码 / 类别码）
 */
public class TokenType {
    // 关键字
    public static final com.zds.lexer.Token.Type IF = new com.zds.lexer.Token.Type("IF");     // if
    public static final com.zds.lexer.Token.Type ELSE = new com.zds.lexer.Token.Type("ELSE");   // else
    public static final com.zds.lexer.Token.Type FOR = new com.zds.lexer.Token.Type("FOR");    // for
    public static final com.zds.lexer.Token.Type WHILE = new com.zds.lexer.Token.Type("WHILE");  // while
    public static final com.zds.lexer.Token.Type INT = new com.zds.lexer.Token.Type("INT");    // int
    public static final com.zds.lexer.Token.Type DOUBLE = new com.zds.lexer.Token.Type("DOUBLE"); // double
    public static final com.zds.lexer.Token.Type STRING = new com.zds.lexer.Token.Type("STRING"); // string

    // 标识符 & 字面量
    public static final com.zds.lexer.Token.Type IDENT = new com.zds.lexer.Token.Type("IDENT");      // 标识符
    public static final com.zds.lexer.Token.Type INT_LIT = new com.zds.lexer.Token.Type("INT_LIT");    // 整数字面量
    public static final com.zds.lexer.Token.Type DOUBLE_LIT = new com.zds.lexer.Token.Type("DOUBLE_LIT"); // 双精度浮点数字面量
    public static final com.zds.lexer.Token.Type STRING_LIT = new com.zds.lexer.Token.Type("STRING_LIT"); // 字符串字面量

    // 运算符
    public static final com.zds.lexer.Token.Type PLUS = new com.zds.lexer.Token.Type("PLUS");  // +
    public static final com.zds.lexer.Token.Type MINUS = new com.zds.lexer.Token.Type("MINUS"); // -
    public static final com.zds.lexer.Token.Type MUL = new com zds.lexer.Token.Type("MUL");   // *
    public static final com.zds.lexer.Token.Type DIV = new com.zds.lexer.Token.Type("DIV");   // /
    public static final com.zds.lexer.Token.Type ASSIGN = new com.zds.lexer.Token.Type("ASSIGN"); // =
    public static final com.zds.lexer.Token.Type GT = new com.zds.lexer.Token.Type("GT");     // >
    public static final com.zds.lexer.Token.Type GE = new com.zds.lexer.Token.Type("GE");     // >=
    public static final com.zds.lexer.Token.Type LT = new com.zds.lexer.Token.Type("LT");     // <
    public static final com.zds.lexer.Token.Type LE = new com.zds.lexer.Token.Type("LE");     // <=
    public static final com.zds.lexer.Token.Type EQ = new com.zds.lexer.Token.Type("EQ");     // ==
    public static final com.zds.lexer.Token.Type NE = new com.zds.lexer.Token.Type("NE");     // !=

    // 分隔符
    public static final com.zds.lexer.Token.Type LPAREN = new com.zds.lexer.Token.Type("LPAREN");  // (
    public static final com.zds.lexer.Token.Type RPAREN = new com.zds.lexer.Token.Type("RPAREN");  // )
    public static final com.zds.lexer.Token.Type LBRACE = new com.zds.lexer.Token.Type("LBRACE");  // {
    public static final com.zds.lexer.Token.Type RBRACE = new com.zds.lexer.Token.Type("RBRACE");  // }
    public static final com.zds.lexer.Token.Type SEMI = new com.zds.lexer.Token.Type("SEMI");    // ;
    public static final com.zds.lexer.Token.Type COMMA = new com.zds.lexer.Token.Type("COMMA");   // ,

    // 文件结束
    public static final com.zds.lexer.Token.Type EOF = new com.zds.lexer.Token.Type("EOF");   // 文件结束符

    // 错误（不会阻止继续扫描，但会记录 errors）
    public static final com.zds.lexer.Token.Type ERROR = new com.zds.lexer.Token.Type("ERROR");  // 错误标记
}