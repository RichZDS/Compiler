package com.zds.parser;

import java.util.List;
import com.zds.lexer.Token;
import com.zds.lexer.consts.TokenType;
// 错误处理（ErrorHandling）
public class ErrorHandling {
    public void error(List<String> errors, Token token, String message) {
        errors.add(String.format("(%d:%d) 语法错误：%s，遇到 %s('%s')",
                token.line, token.col, message, token.type, token.lexeme));
    }

    public void synchronize(Recognizer r) {
        r.advance();
        while (!r.isAtEnd()) {
            if (r.previous().type == TokenType.SEMI) return;
            if (r.peek().type == TokenType.IF ||
                r.peek().type == TokenType.FOR ||
                r.peek().type == TokenType.WHILE ||
                r.peek().type == TokenType.INT ||
                r.peek().type == TokenType.DOUBLE ||
                r.peek().type == TokenType.STRING ||
                r.peek().type == TokenType.LBRACE) {
                return;
            }
            r.advance();
        }
    }

    public void synchronizeInFor(Recognizer r) {
        while (!r.isAtEnd() && r.peek().type != TokenType.SEMI && r.peek().type != TokenType.RPAREN) {
            r.advance();
        }
    }
}
