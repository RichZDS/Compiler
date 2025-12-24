package com.zds.lexer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 词法分析器内部实现（Core Scanner）
 * 包级私有类，负责将源代码扫描为 Token 流
 */
class ScannerCore {
    private final String source;
    private final List<Lexer.Token> tokens = new ArrayList<>();
    private final List<String> errors = new ArrayList<>();

    private int start = 0;
    private int current = 0;
    private int line = 1;
    private int col = 1;

    private static final Map<String, Lexer.Token.Type> KEYWORDS = new HashMap<>();
    static {
        KEYWORDS.put("if", Lexer.TokenType.IF);
        KEYWORDS.put("else", Lexer.TokenType.ELSE);
        KEYWORDS.put("for", Lexer.TokenType.FOR);
        KEYWORDS.put("while", Lexer.TokenType.WHILE);
        KEYWORDS.put("int", Lexer.TokenType.INT);
        KEYWORDS.put("double", Lexer.TokenType.DOUBLE);
        KEYWORDS.put("string", Lexer.TokenType.STRING);
    }

    public ScannerCore(String source) {
        this.source = source == null ? "" : source;
    }

    public List<String> getErrors() {
        return errors;
    }

    public List<Lexer.Token> scanTokens() {
        while (!isAtEnd()) {
            start = current;
            scanToken();
        }
        tokens.add(new Lexer.Token(Lexer.TokenType.EOF, "", null, line, col));
        return tokens;
    }

    private void scanToken() {
        char c = advance();
        switch (c) {
            case ' ':
            case '\r':
            case '\t':
                return;
            case '\n':
                line++;
                col = 1;
                return;
            case '(': addToken(Lexer.TokenType.LPAREN); break;
            case ')': addToken(Lexer.TokenType.RPAREN); break;
            case '{': addToken(Lexer.TokenType.LBRACE); break;
            case '}': addToken(Lexer.TokenType.RBRACE); break;
            case ';': addToken(Lexer.TokenType.SEMI); break;
            case ',': addToken(Lexer.TokenType.COMMA); break;
            case '+': addToken(Lexer.TokenType.PLUS); break;
            case '-': addToken(Lexer.TokenType.MINUS); break;
            case '*': addToken(Lexer.TokenType.MUL); break;
            case '/':
                if (match('/')) {
                    while (peek() != '\n' && !isAtEnd()) advance();
                    return;
                }
                if (match('*')) {
                    blockComment();
                    return;
                }
                addToken(Lexer.TokenType.DIV);
                break;
            case '=': addToken(match('=') ? Lexer.TokenType.EQ : Lexer.TokenType.ASSIGN); break;
            case '>': addToken(match('=') ? Lexer.TokenType.GE : Lexer.TokenType.GT); break;
            case '<': addToken(match('=') ? Lexer.TokenType.LE : Lexer.TokenType.LT); break;
            case '!':
                if (match('=')) addToken(Lexer.TokenType.NE);
                else error("非法字符 '!'：仅支持 '!='");
                break;
            case '"': stringLiteral(); break;
            default:
                if (isDigit(c)) {
                    numberLiteral();
                    return;
                }
                if (isAlpha(c)) {
                    identifierOrKeyword();
                    return;
                }
                error("无法识别的字符：'" + c + "'");
                break;
        }
    }

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

    private void identifierOrKeyword() {
        while (isAlphaNumeric(peek())) advance();
        String text = source.substring(start, current);
        Lexer.Token.Type type = KEYWORDS.getOrDefault(text, Lexer.TokenType.IDENT);
        addToken(type);
    }

    private void numberLiteral() {
        while (isDigit(peek())) advance();
        boolean isDouble = false;
        if (peek() == '.' && isDigit(peekNext())) {
            isDouble = true;
            advance();
            while (isDigit(peek())) advance();
        }
        String text = source.substring(start, current);
        try {
            if (isDouble) addToken(Lexer.TokenType.DOUBLE_LIT, Double.parseDouble(text));
            else addToken(Lexer.TokenType.INT_LIT, Integer.parseInt(text));
        } catch (NumberFormatException ex) {
            error("数字格式错误：" + text);
        }
    }

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
        advance();
        String lexeme = source.substring(start, current);
        tokens.add(new Lexer.Token(Lexer.TokenType.STRING_LIT, lexeme, sb.toString(), beginLine, beginCol));
    }

    private boolean isAtEnd() { return current >= source.length(); }

    private char advance() {
        char c = source.charAt(current++);
        col++;
        return c;
    }

    private char peek() {
        if (isAtEnd()) return '\0';
        return source.charAt(current);
    }

    private char peekNext() {
        if (current + 1 >= source.length()) return '\0';
        return source.charAt(current + 1);
    }

    private boolean match(char expected) {
        if (isAtEnd()) return false;
        if (source.charAt(current) != expected) return false;
        current++;
        col++;
        return true;
    }

    private void addToken(Lexer.Token.Type type) { addToken(type, null); }

    private void addToken(Lexer.Token.Type type, Object literal) {
        String text = source.substring(start, current);
        int tokenCol = col - (current - start);
        tokens.add(new Lexer.Token(type, text, literal, line, tokenCol));
    }

    private void error(String msg) {
        int errCol = col - 1;
        errors.add(String.format("(%d:%d) 词法错误：%s", line, errCol, msg));
        String text = source.substring(start, Math.min(current, source.length()));
        tokens.add(new Lexer.Token(Lexer.TokenType.ERROR, text, null, line, errCol));
    }

    private static boolean isDigit(char c) { return c >= '0' && c <= '9'; }

    private static boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_';
    }

    private static boolean isAlphaNumeric(char c) { return isAlpha(c) || isDigit(c); }
}
