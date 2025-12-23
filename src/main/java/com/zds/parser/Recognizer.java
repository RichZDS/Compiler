package com.zds.parser;

import java.util.ArrayList;
import java.util.List;

import com.zds.lexer.Token;
import com.zds.lexer.consts.TokenType;

// 语法分析器（Recognizer）
public class Recognizer {
    private final List<Token> tokens;
    private int current = 0;
    private final List<String> errors;
    private final ErrorHandling err;
    private final AST.Factory ast;

     
    public Recognizer(List<Token> tokens, List<String> errors, ErrorHandling err, AST.Factory ast) {
        this.tokens = (tokens == null) ? List.of(new Token(TokenType.EOF, "", null, 1, 1)) : tokens;
        this.errors = errors;
        this.err = err;
        this.ast = ast;
    }

    public AST.Program parseProgram() {
        List<AST.Stmt> stmts = new ArrayList<>();
        while (!isAtEnd()) {
            if (peek().type == TokenType.ERROR) { advance(); continue; }
            AST.Stmt s = statement();
            if (s != null) stmts.add(s);
            else err.synchronize(this);
        }
        return ast.newProgram(stmts);
    }

    private AST.Stmt statement() {
        if (match(TokenType.LBRACE)) return block();
        if (match(TokenType.IF)) return ifStatement();
        if (match(TokenType.WHILE)) return whileStatement();
        if (match(TokenType.FOR)) return forStatement();

        if (match(TokenType.INT, TokenType.DOUBLE, TokenType.STRING)) {
            return declaration(previous());
        }

        if (check(TokenType.IDENT) && checkNext(TokenType.ASSIGN)) {
            AST.Stmt a = assignmentStatement();
            consume(TokenType.SEMI, "缺少 ';'（赋值语句必须以分号结尾）");
            return a;
        }

        AST.Expr e = expression();
        consume(TokenType.SEMI, "缺少 ';'（表达式语句必须以分号结尾）");
        return ast.newExprStmt(e);
    }

    private AST.Block block() {
        List<AST.Stmt> stmts = new ArrayList<>();
        while (!check(TokenType.RBRACE) && !isAtEnd()) {
            AST.Stmt s = statement();
            if (s != null) stmts.add(s);
            else err.synchronize(this);
        }
        consume(TokenType.RBRACE, "缺少 '}'（块语句未闭合）");
        return ast.newBlock(stmts);
    }

    private AST.Stmt declaration(Token typeToken) {
        String typeName = typeToken.lexeme;
        Token name = consume(TokenType.IDENT, "声明语句缺少标识符（变量名）");

        AST.Expr init = null;
        if (match(TokenType.ASSIGN)) init = expression();

        consume(TokenType.SEMI, "缺少 ';'（声明语句必须以分号结尾）");
        return ast.newVarDecl(typeName, name.lexeme, init);
    }

    private AST.Stmt assignmentStatement() {
        Token name = consume(TokenType.IDENT, "赋值语句缺少标识符（变量名）");
        consume(TokenType.ASSIGN, "赋值语句缺少 '='");
        AST.Expr value = expression();
        return ast.newAssign(name.lexeme, value);
    }

    private AST.Stmt ifStatement() {
        consume(TokenType.LPAREN, "if 条件缺少 '('");
        AST.Expr cond = condition();
        consume(TokenType.RPAREN, "if 条件缺少 ')'");

        AST.Stmt thenBranch = statement();
        AST.Stmt elseBranch = null;
        if (match(TokenType.ELSE)) elseBranch = statement();
        return ast.newIfStmt(cond, thenBranch, elseBranch);
    }

    private AST.Stmt whileStatement() {
        consume(TokenType.LPAREN, "while 条件缺少 '('");
        AST.Expr cond = condition();
        consume(TokenType.RPAREN, "while 条件缺少 ')'");
        AST.Stmt body = statement();
        return ast.newWhileStmt(cond, body);
    }

    private AST.Stmt forStatement() {
        consume(TokenType.LPAREN, "for 缺少 '('");

        AST.Stmt init = null;
        if (match(TokenType.SEMI)) {
        } else if (match(TokenType.INT, TokenType.DOUBLE, TokenType.STRING)) {
            Token typeToken = previous();
            String typeName = typeToken.lexeme;
            Token name = consume(TokenType.IDENT, "for-init 声明缺少变量名");
            AST.Expr initExpr = null;
            if (match(TokenType.ASSIGN)) initExpr = expression();
            init = ast.newVarDecl(typeName, name.lexeme, initExpr);
            consume(TokenType.SEMI, "for-init 缺少 ';'");
        } else if (check(TokenType.IDENT) && checkNext(TokenType.ASSIGN)) {
            init = assignmentStatement();
            consume(TokenType.SEMI, "for-init 缺少 ';'");
        } else {
            err.error(errors, peek(), "for-init 只能是：声明/赋值/空（如 for(;...;...)）");
            err.synchronizeInFor(this);
            consume(TokenType.SEMI, "for-init 缺少 ';'");
        }

        AST.Expr cond = null;
        if (!check(TokenType.SEMI)) cond = condition();
        consume(TokenType.SEMI, "for-cond 缺少 ';'");

        AST.Stmt step = null;
        if (!check(TokenType.RPAREN)) {
            if (check(TokenType.IDENT) && checkNext(TokenType.ASSIGN)) {
                step = assignmentStatement();
            } else {
                err.error(errors, peek(), "for-step 目前仅支持赋值形式：i = i + 1");
                err.synchronizeInFor(this);
            }
        }

        consume(TokenType.RPAREN, "for 缺少 ')'");
        AST.Stmt body = statement();
        return ast.newForStmt(init, cond, step, body);
    }

    private AST.Expr condition() {
        AST.Expr left = expression();
        if (match(TokenType.GT, TokenType.GE, TokenType.LT, TokenType.LE, TokenType.EQ, TokenType.NE)) {
            String op = previous().lexeme;
            AST.Expr right = expression();
            return ast.newBinary(op, left, right);
        }
        return left;
    }

    private AST.Expr expression() { return additive(); }

    private AST.Expr additive() {
        AST.Expr expr = multiplicative();
        while (match(TokenType.PLUS, TokenType.MINUS)) {
            String op = previous().lexeme;
            AST.Expr right = multiplicative();
            expr = ast.newBinary(op, expr, right);
        }
        return expr;
    }

    private AST.Expr multiplicative() {
        AST.Expr expr = unary();
        while (match(TokenType.MUL, TokenType.DIV)) {
            String op = previous().lexeme;
            AST.Expr right = unary();
            expr = ast.newBinary(op, expr, right);
        }
        return expr;
    }

    private AST.Expr unary() {
        if (match(TokenType.PLUS, TokenType.MINUS)) {
            String op = previous().lexeme;
            return ast.newUnary(op, unary());
        }
        return primary();
    }

    private AST.Expr primary() {
        if (match(TokenType.INT_LIT)) return ast.newLiteral(previous().literal);
        if (match(TokenType.DOUBLE_LIT)) return ast.newLiteral(previous().literal);
        if (match(TokenType.STRING_LIT)) return ast.newLiteral(previous().literal);

        if (match(TokenType.IDENT)) return ast.newVar(previous().lexeme);

        if (match(TokenType.LPAREN)) {
            AST.Expr e = expression();
            consume(TokenType.RPAREN, "缺少 ')'（括号未闭合）");
            return e;
        }

        err.error(errors, peek(), "无法解析的表达式（期望：常量/标识符/(表达式)）");
        advance();
        return ast.newLiteral(null);
    }

    public boolean match(Token.Type... types) {
        for (Token.Type t : types) { if (check(t)) { advance(); return true; } }
        return false;
    }
    public boolean check(Token.Type type) { return !isAtEnd() && peek().type == type; }
    public boolean checkNext(Token.Type type) {
        if (current + 1 >= tokens.size()) return false;
        return tokens.get(current + 1).type == type;
    }
    public Token advance() { if (!isAtEnd()) current++; return previous(); }
    public boolean isAtEnd() {
        if (current >= tokens.size()) return true;
        return peek().type == TokenType.EOF;
    }
    public Token peek() { return tokens.get(Math.min(current, tokens.size() - 1)); }
    public Token previous() { return tokens.get(Math.max(current - 1, 0)); }
    public Token consume(Token.Type type, String message) {
        if (check(type)) return advance();
        err.error(errors, peek(), message);
        return peek();
    }
}
