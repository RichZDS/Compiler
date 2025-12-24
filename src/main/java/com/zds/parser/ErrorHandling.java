package com.zds.parser;

import java.util.List;
import com.zds.lexer.Lexer;

/**
 * 错误处理类 - 负责处理语法分析过程中的错误
 */
class ErrorHandling {
    /**
     * 记录语法错误
     */
    public void error(List<String> errors, Lexer.Token token, String message) {
        errors.add(String.format("(%d:%d) 语法错误：%s，遇到 %s('%s')",
                token.line, token.col, message, token.type, token.lexeme));
    }

    /**
     * 在遇到错误时同步到下一个合适的位置
     */
    public void synchronize(Recognizer r) {
        r.advance();
        // 同步到分号或语句开始的标记
        while (!r.isAtEnd()) {
            if (r.previous().type == Lexer.TokenType.SEMI) return;
            if (r.peek().type == Lexer.TokenType.IF ||
                r.peek().type == Lexer.TokenType.FOR ||
                r.peek().type == Lexer.TokenType.WHILE ||
                r.peek().type == Lexer.TokenType.INT ||
                r.peek().type == Lexer.TokenType.DOUBLE ||
                r.peek().type == Lexer.TokenType.STRING ||
                r.peek().type == Lexer.TokenType.LBRACE) {
                return;
            }
            r.advance();
        }
    }

    /**
     * 在for循环内部同步到合适的位置
     */
    public void synchronizeInFor(Recognizer r) {
        while (!r.isAtEnd() && r.peek().type != Lexer.TokenType.SEMI && r.peek().type != Lexer.TokenType.RPAREN) {
            r.advance();
        }
    }
}
