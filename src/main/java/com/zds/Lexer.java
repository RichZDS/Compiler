package com.zds;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.zds.util.LexerUtils;

/*
 *@auther 郑笃实
 *@version 1.0
 */

/**
 * 词法分析器（Lexical Analyzer）
 *
 * ✅ 用 PPT 常见说法：把“字符流”切分成“单词符号/记号 Token”，即二元式：<种别码, 属性值>
 * - 种别码：TokenType（比如 IF、IDENT、INT_LIT、PLUS ...）
 * - 属性值：literal(字面量值) 或 lexeme(单词原文)
 *
 * 目前支持：
 * - 关键字：if else for while int double string
 * - 标识符：abc、a1、_tmp
 * - 字面量：整数 123、小数 3.14、字符串 "hello"
 * - 运算符：+ - * / 以及 =  > < >= <= == !=
 * - 分隔符：( ) { } ; ,
 * - 注释：// 行注释、/* 块注释 * /
 */
public class Lexer {

    /** Token 种别码（PPT 常叫：种别码 / 类别码） */
    public enum TokenType {
        // 关键字
        IF,     // if
        ELSE,   // else
        FOR,    // for
        WHILE,  // while
        INT,    // int
        DOUBLE, // double
        STRING, // string

        // 标识符 & 字面量
        IDENT,      // 标识符
        INT_LIT,    // 整数字面量
        DOUBLE_LIT, // 双精度浮点数字面量
        STRING_LIT, // 字符串字面量

        // 运算符
        PLUS,  // +
        MINUS, // -
        MUL,   // *
        DIV,   // /
        ASSIGN, // =
        GT,     // >
        GE,     // >=
        LT,     // <
        LE,     // <=
        EQ,     // ==
        NE,     // !=

        // 分隔符
        LPAREN,  // (
        RPAREN,  // )
        LBRACE,  // {
        RBRACE,  // }
        SEMI,    // ;
        COMMA,   // ,

        // 文件结束
        EOF,   // 文件结束符

        // 错误（不会阻止继续扫描，但会记录 errors）
        ERROR  // 错误标记
    }

    /** 单词符号（Token）= 二元式 <种别码, 属性值> */
    public static class Token {
        public final TokenType type;   // 种别码
        public final String lexeme;    // 单词原文（属性的一种）
        public final Object literal;   // 字面量值（属性的一种）
        public final int line;         // 行号（从 1 开始）
        public final int col;          // 列号（从 1 开始）

        public Token(TokenType type, String lexeme, Object literal, int line, int col) {
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
    }

    private final String source;
    private final List<Token> tokens = new ArrayList<>();
    private final List<String> errors = new ArrayList<>();

    private int start = 0;
    private int current = 0;
    private int line = 1;
    private int col = 1;

    private static final Map<String, TokenType> KEYWORDS = new HashMap<>();
    static {
        KEYWORDS.put("if", TokenType.IF);
        KEYWORDS.put("else", TokenType.ELSE);
        KEYWORDS.put("for", TokenType.FOR);
        KEYWORDS.put("while", TokenType.WHILE);
        KEYWORDS.put("int", TokenType.INT);
        KEYWORDS.put("double", TokenType.DOUBLE);
        KEYWORDS.put("string", TokenType.STRING);
    }

    public Lexer(String source) {
        this.source = source == null ? "" : source;
    }

    /**
     * 获取词法分析过程中发现的错误列表
     */
    public List<String> getErrors() {
        return errors;
    }

    /**
     * 扫描整个源程序，输出 Token 序列
     */
    public List<Token> scanTokens() {
        while (!isAtEnd()) {
            start = current;
            scanToken();
        }
        tokens.add(new Token(TokenType.EOF, "", null, line, col));
        return tokens;
    }

    /**
     * 扫描单个Token
     */
    private void scanToken() {
        char c = advance();
        switch (c) {
            // 空白
            case ' ':
            case '\r':
            case '\t':
                return;
            case '\n':
                line++;
                col = 1;
                return;

            // 分隔符
            case '(': addToken(TokenType.LPAREN); break;
            case ')': addToken(TokenType.RPAREN); break;
            case '{': addToken(TokenType.LBRACE); break;
            case '}': addToken(TokenType.RBRACE); break;
            case ';': addToken(TokenType.SEMI); break;
            case ',': addToken(TokenType.COMMA); break;

            // 运算符
            case '+': addToken(TokenType.PLUS); break;
            case '-': addToken(TokenType.MINUS); break;
            case '*': addToken(TokenType.MUL); break;

            case '/':
                if (match('/')) { // 行注释
                    while (peek() != '\n' && !isAtEnd()) advance();
                    return;
                }
                if (match('*')) { // 块注释
                    blockComment();
                    return;
                }
                addToken(TokenType.DIV);
                break;

            case '=': addToken(match('=') ? TokenType.EQ : TokenType.ASSIGN); break;
            case '>': addToken(match('=') ? TokenType.GE : TokenType.GT); break;
            case '<': addToken(match('=') ? TokenType.LE : TokenType.LT); break;

            case '!':
                if (match('=')) addToken(TokenType.NE);
                else error("非法字符 '!'：仅支持 '!='");
                break;

            case '"': stringLiteral(); break;

            default:
                if (LexerUtils.isDigit(c)) {
                    numberLiteral();
                    return;
                }
                if (LexerUtils.isAlpha(c)) {
                    identifierOrKeyword();
                    return;
                }
                error("无法识别的字符：'" + c + "'");
                break;
        }
    }

    /**
     * 处理块注释
     */
    private void blockComment() {
        while (!isAtEnd()) {
            if (peek() == '\n') {
                advance();
                line++;
                col = 1;
                continue;
            }
            if (peek() == '*' && peekNext() == '/') {
                advance();
                advance();
                return;
            }
            advance();
        }
        error("块注释未闭合（缺少 */）");
    }

    /**
     * 识别标识符或关键字
     */
    private void identifierOrKeyword() {
        while (LexerUtils.isAlphaNumeric(peek())) advance();
        String text = source.substring(start, current);
        TokenType type = KEYWORDS.getOrDefault(text, TokenType.IDENT);
        addToken(type);
    }

    /**
     * 识别数字字面量（整数或浮点数）
     */
    private void numberLiteral() {
        while (LexerUtils.isDigit(peek())) advance();

        boolean isDouble = false;
        if (peek() == '.' && LexerUtils.isDigit(peekNext())) {
            isDouble = true;
            advance();
            while (LexerUtils.isDigit(peek())) advance();
        }

        String text = source.substring(start, current);
        try {
            if (isDouble) addToken(TokenType.DOUBLE_LIT, Double.parseDouble(text));
            else addToken(TokenType.INT_LIT, Integer.parseInt(text));
        } catch (NumberFormatException ex) {
            error("数字格式错误：" + text);
        }
    }

    /**
     * 识别字符串字面量
     */
    private void stringLiteral() {
        int beginLine = line;
        int beginCol = col - 1;

        StringBuilder sb = new StringBuilder();
        while (!isAtEnd() && peek() != '"') {
            char ch = advance();
            if (ch == '\n') {
                errors.add(String.format("(%d:%d) 词法错误：字符串常量不允许换行", beginLine, beginCol));
                return;
            }

            if (ch == '\\') {
                if (isAtEnd()) break;
                char esc = advance();
                switch (esc) {
                    case '"': sb.append('"'); break;
                    case 'n': sb.append('\n'); break;
                    case 't': sb.append('\t'); break;
                    case '\\': sb.append('\\'); break;
                    default: sb.append(esc); break;
                }
            } else {
                sb.append(ch);
            }
        }

        if (isAtEnd()) {
            errors.add(String.format("(%d:%d) 词法错误：字符串未闭合", beginLine, beginCol));
            return;
        }

        advance(); // closing quote
        String lexeme = source.substring(start, current);
        tokens.add(new Token(TokenType.STRING_LIT, lexeme, sb.toString(), beginLine, beginCol));
    }


    /**
     * 判断是否到达源码末尾
     */
    private boolean isAtEnd() { return current >= source.length(); }

    /**
     * 读取下一个字符，并移动当前位置指针
     */
    private char advance() {
        char c = source.charAt(current++);
        col++;
        return c;
    }

    /**
     * 查看当前字符但不移动指针
     */
    private char peek() {
        if (isAtEnd()) return '\0';
        return source.charAt(current);
    }

    /**
     * 查看下一个字符但不移动指针
     */
    private char peekNext() {
        if (current + 1 >= source.length()) return '\0';
        return source.charAt(current + 1);
    }

    /**
     * 匹配当前字符是否为期望的字符
     */
    private boolean match(char expected) {
        if (isAtEnd()) return false;
        if (source.charAt(current) != expected) return false;
        current++;
        col++;
        return true;
    }

    /**
     * 添加Token（无属性值）
     */
    private void addToken(TokenType type) { addToken(type, null); }

    /**
     * 添加Token（带属性值）
     */
    private void addToken(TokenType type, Object literal) {
        String text = source.substring(start, current);
        int tokenCol = col - (current - start);
        tokens.add(new Token(type, text, literal, line, tokenCol));
    }

    /**
     * 记录错误信息
     */
    private void error(String msg) {
        int errCol = col - 1;
        errors.add(String.format("(%d:%d) 词法错误：%s", line, errCol, msg));
        String text = source.substring(start, Math.min(current, source.length()));
        tokens.add(new Token(TokenType.ERROR, text, null, line, errCol));
    }

    /**
     * 判断字符是否为数字
     */
    private static boolean isDigit(char c) { return LexerUtils.isDigit(c); }

    /**
     * 判断字符是否为字母或下划线
     */
    private static boolean isAlpha(char c) { return LexerUtils.isAlpha(c); }

    /**
     * 判断字符是否为字母、数字或下划线
     */
    private static boolean isAlphaNumeric(char c) { return LexerUtils.isAlphaNumeric(c); }
}
