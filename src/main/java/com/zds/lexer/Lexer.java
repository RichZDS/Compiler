package com.zds.lexer;

import java.util.List;

/**
 * 词法分析器 (Lexer)
 * 门面类 (Facade)：对外提供统一的词法分析接口，屏蔽内部扫描细节。
 *
 * 输入：源代码字符串 (String)
 * 输出：词法单元序列 (List<Token>)
 */
public class Lexer {

    /**
     * 对源代码进行词法分析
     * @param source 源代码字符串
     * @return 词法单元列表
     */
    public static List<Token> scan(String source) {
        ScannerCore scanner = new ScannerCore(source);
        return scanner.scanTokens();
    }

    /**
     * 获取最后一次分析的错误信息（如果有）
     * 注意：通常建议通过上层 Service 统一收集错误，此方法主要用于单独测试。
     */
    public static List<String> scanErrors(String source) {
        ScannerCore scanner = new ScannerCore(source);
        scanner.scanTokens();
        return scanner.getErrors();
    }

    // ==========================================
    // Data Structures (Public Interface)
    // ==========================================

    /**
     * 词法单元（Token）
     * 编译器前端的基本数据单元，表示源代码中的一个单词或符号。
     */
    public static class Token {
        /**
         * Token 类型标识
         */
        public static class Type {
            private final String name;
            public Type(String name) { this.name = name; }
            @Override public String toString() { return name; }
        }

        public final Type type;       // 种别码
        public final String lexeme;   // 单词原文
        public final Object literal;  // 字面量值（如数字、字符串内容）
        public final int line;        // 行号
        public final int col;         // 列号

        public Token(Type type, String lexeme, Object literal, int line, int col) {
            this.type = type;
            this.lexeme = lexeme;
            this.literal = literal;
            this.line = line;
            this.col = col;
        }

        @Override
        public String toString() {
            String attr = (literal != null) ? String.valueOf(literal) : lexeme;
            return String.format("<%s, %s>  @(%d:%d)", type, attr, line, col);
        }
    }

    /**
     * Token 类型常量定义
     */
    public static class TokenType {
        // 关键字
        public static final Token.Type IF = new Token.Type("IF");
        public static final Token.Type ELSE = new Token.Type("ELSE");
        public static final Token.Type FOR = new Token.Type("FOR");
        public static final Token.Type WHILE = new Token.Type("WHILE");
        public static final Token.Type INT = new Token.Type("INT");
        public static final Token.Type DOUBLE = new Token.Type("DOUBLE");
        public static final Token.Type STRING = new Token.Type("STRING");

        // 标识符 & 字面量
        public static final Token.Type IDENT = new Token.Type("IDENT");
        public static final Token.Type INT_LIT = new Token.Type("INT_LIT");
        public static final Token.Type DOUBLE_LIT = new Token.Type("DOUBLE_LIT");
        public static final Token.Type STRING_LIT = new Token.Type("STRING_LIT");

        // 运算符
        public static final Token.Type PLUS = new Token.Type("PLUS");
        public static final Token.Type MINUS = new Token.Type("MINUS");
        public static final Token.Type MUL = new Token.Type("MUL");
        public static final Token.Type DIV = new Token.Type("DIV");
        public static final Token.Type ASSIGN = new Token.Type("ASSIGN");
        public static final Token.Type GT = new Token.Type("GT");
        public static final Token.Type GE = new Token.Type("GE");
        public static final Token.Type LT = new Token.Type("LT");
        public static final Token.Type LE = new Token.Type("LE");
        public static final Token.Type EQ = new Token.Type("EQ");
        public static final Token.Type NE = new Token.Type("NE");

        // 分隔符
        public static final Token.Type LPAREN = new Token.Type("LPAREN");
        public static final Token.Type RPAREN = new Token.Type("RPAREN");
        public static final Token.Type LBRACE = new Token.Type("LBRACE");
        public static final Token.Type RBRACE = new Token.Type("RBRACE");
        public static final Token.Type SEMI = new Token.Type("SEMI");
        public static final Token.Type COMMA = new Token.Type("COMMA");

        // 特殊标记
        public static final Token.Type EOF = new Token.Type("EOF");
        public static final Token.Type ERROR = new Token.Type("ERROR");
    }
}
