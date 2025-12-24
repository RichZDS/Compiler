package com.zds.parser;

import java.util.ArrayList;
import java.util.List;

import com.zds.lexer.Lexer;

/**
 * 语法分析器（Recognizer）
 * 使用递归下降（Recursive Descent）算法，将词法单元序列转换为抽象语法树（AST）。
 */
class Recognizer {
    /**
     * 词法单元列表
     */
    private final List<Lexer.Token> tokens;
    /**
     * 当前词法单元索引
     */
    private int current = 0;
    /**
     * 错误收集列表
     */
    private final List<String> errors;
    /**
     * 错误处理对象
     */
    private final ErrorHandling err;
    /**
     * AST工厂对象
     */
    private final Parser.Factory ast;

     
    /**
     * 构造语法分析器
     * @param tokens 词法单元列表
     * @param errors 错误收集列表
     * @param err 错误处理对象
     * @param ast AST工厂对象
     */
    public Recognizer(List<Lexer.Token> tokens, List<String> errors, ErrorHandling err, Parser.Factory ast) {
        this.tokens = (tokens == null) ? List.of(new Lexer.Token(Lexer.TokenType.EOF, "", null, 1, 1)) : tokens;
        this.errors = errors;
        this.err = err;
        this.ast = ast;
    }

    /**
     * 解析整个程序
     * @return 解析得到的程序节点
     */
    public Parser.Program parseProgram() {
        List<Parser.Stmt> stmts = new ArrayList<>();
        // 遍历所有词法单元，解析语句
        while (!isAtEnd()) {
            if (peek().type == Lexer.TokenType.ERROR) { advance(); continue; }
            Parser.Stmt s = statement();
            if (s != null) stmts.add(s);
            else err.synchronize(this);
        }
        return ast.newProgram(stmts);
    }

    /**
     * 解析语句
     * @return 解析得到的语句节点
     */
    private Parser.Stmt statement() {
        // 根据词法单元类型选择相应的解析方法
        if (match(Lexer.TokenType.LBRACE)) return block();
        if (match(Lexer.TokenType.IF)) return ifStatement();
        if (match(Lexer.TokenType.WHILE)) return whileStatement();
        if (match(Lexer.TokenType.FOR)) return forStatement();

        if (match(Lexer.TokenType.INT, Lexer.TokenType.DOUBLE, Lexer.TokenType.STRING)) {
            return declaration(previous());
        }

        if (check(Lexer.TokenType.IDENT) && checkNext(Lexer.TokenType.ASSIGN)) {
            Parser.Stmt a = assignmentStatement();
            consume(Lexer.TokenType.SEMI, "缺少 ';'（赋值语句必须以分号结尾）");
            return a;
        }

        Parser.Expr e = expression();
        consume(Lexer.TokenType.SEMI, "缺少 ';'（表达式语句必须以分号结尾）");
        return ast.newExprStmt(e);
    }

    /**
     * 解析块语句
     * @return 解析得到的块语句节点
     */
    private Parser.Block block() {
        List<Parser.Stmt> stmts = new ArrayList<>();
        // 解析大括号内的语句列表
        while (!check(Lexer.TokenType.RBRACE) && !isAtEnd()) {
            Parser.Stmt s = statement();
            if (s != null) stmts.add(s);
            else err.synchronize(this);
        }
        consume(Lexer.TokenType.RBRACE, "缺少 '}'（块语句未闭合）");
        return ast.newBlock(stmts);
    }

    /**
     * 解析变量声明语句
     * @param typeToken 类型词法单元
     * @return 解析得到的变量声明语句节点
     */
    private Parser.Stmt declaration(Lexer.Token typeToken) {
        String typeName = typeToken.lexeme;
        Lexer.Token name = consume(Lexer.TokenType.IDENT, "声明语句缺少标识符（变量名）");

        Parser.Expr init = null;
        if (match(Lexer.TokenType.ASSIGN)) init = expression();

        consume(Lexer.TokenType.SEMI, "缺少 ';'（声明语句必须以分号结尾）");
        return ast.newVarDecl(typeName, name.lexeme, init);
    }

    /**
     * 解析赋值语句
     * @return 解析得到的赋值语句节点
     */
    private Parser.Stmt assignmentStatement() {
        Lexer.Token name = consume(Lexer.TokenType.IDENT, "赋值语句缺少标识符（变量名）");
        consume(Lexer.TokenType.ASSIGN, "赋值语句缺少 '='");
        Parser.Expr value = expression();
        return ast.newAssign(name.lexeme, value);
    }

    /**
     * 解析if语句
     * @return 解析得到的if语句节点
     */
    private Parser.Stmt ifStatement() {
        consume(Lexer.TokenType.LPAREN, "if 条件缺少 '('");
        Parser.Expr cond = condition();
        consume(Lexer.TokenType.RPAREN, "if 条件缺少 ')'");

        Parser.Stmt thenBranch = statement();
        Parser.Stmt elseBranch = null;
        if (match(Lexer.TokenType.ELSE)) elseBranch = statement();
        return ast.newIfStmt(cond, thenBranch, elseBranch);
    }

    /**
     * 解析while循环语句
     * @return 解析得到的while循环语句节点
     */
    private Parser.Stmt whileStatement() {
        consume(Lexer.TokenType.LPAREN, "while 条件缺少 '('");
        Parser.Expr cond = condition();
        consume(Lexer.TokenType.RPAREN, "while 条件缺少 ')'");
        Parser.Stmt body = statement();
        return ast.newWhileStmt(cond, body);
    }

    /**
     * 解析for循环语句
     * @return 解析得到的for循环语句节点
     */
    private Parser.Stmt forStatement() {
        consume(Lexer.TokenType.LPAREN, "for 缺少 '('");

        Parser.Stmt init = null;
        // 解析for循环的初始化部分
        if (match(Lexer.TokenType.SEMI)) {
        } else if (match(Lexer.TokenType.INT, Lexer.TokenType.DOUBLE, Lexer.TokenType.STRING)) {
            Lexer.Token typeToken = previous();
            String typeName = typeToken.lexeme;
            Lexer.Token name = consume(Lexer.TokenType.IDENT, "for-init 声明缺少变量名");
            Parser.Expr initExpr = null;
            if (match(Lexer.TokenType.ASSIGN)) initExpr = expression();
            init = ast.newVarDecl(typeName, name.lexeme, initExpr);
            consume(Lexer.TokenType.SEMI, "for-init 缺少 ';'");
        } else if (check(Lexer.TokenType.IDENT) && checkNext(Lexer.TokenType.ASSIGN)) {
            init = assignmentStatement();
            consume(Lexer.TokenType.SEMI, "for-init 缺少 ';'");
        } else {
            err.error(errors, peek(), "for-init 只能是：声明/赋值/空（如 for(;...;...)）");
            err.synchronizeInFor(this);
            consume(Lexer.TokenType.SEMI, "for-init 缺少 ';'");
        }

        Parser.Expr cond = null;
        // 解析for循环的条件部分
        if (!check(Lexer.TokenType.SEMI)) cond = condition();
        consume(Lexer.TokenType.SEMI, "for-cond 缺少 ';'");

        Parser.Stmt step = null;
        // 解析for循环的步进部分
        if (!check(Lexer.TokenType.RPAREN)) {
            if (check(Lexer.TokenType.IDENT) && checkNext(Lexer.TokenType.ASSIGN)) {
                step = assignmentStatement();
            } else {
                err.error(errors, peek(), "for-step 目前仅支持赋值形式：i = i + 1");
                err.synchronizeInFor(this);
            }
        }

        consume(Lexer.TokenType.RPAREN, "for 缺少 ')'");
        Parser.Stmt body = statement();
        return ast.newForStmt(init, cond, step, body);
    }

    /**
     * 解析条件表达式
     */
    private Parser.Expr condition() {
        Parser.Expr left = expression();
        if (match(Lexer.TokenType.GT, Lexer.TokenType.GE, Lexer.TokenType.LT, Lexer.TokenType.LE, Lexer.TokenType.EQ, Lexer.TokenType.NE)) {
            String op = previous().lexeme;
            Parser.Expr right = expression();
            return ast.newBinary(op, left, right);
        }
        return left;
    }

    private Parser.Expr expression() { return additive(); }

    private Parser.Expr additive() {
        Parser.Expr expr = multiplicative();
        while (match(Lexer.TokenType.PLUS, Lexer.TokenType.MINUS)) {
            String op = previous().lexeme;
            Parser.Expr right = multiplicative();
            expr = ast.newBinary(op, expr, right);
        }
        return expr;
    }

    private Parser.Expr multiplicative() {
        Parser.Expr expr = unary();
        while (match(Lexer.TokenType.MUL, Lexer.TokenType.DIV)) {
            String op = previous().lexeme;
            Parser.Expr right = unary();
            expr = ast.newBinary(op, expr, right);
        }
        return expr;
    }

    private Parser.Expr unary() {
        if (match(Lexer.TokenType.PLUS, Lexer.TokenType.MINUS)) {
            String op = previous().lexeme;
            return ast.newUnary(op, unary());
        }
        return primary();
    }

    private Parser.Expr primary() {
        if (match(Lexer.TokenType.INT_LIT)) return ast.newLiteral(previous().literal);
        if (match(Lexer.TokenType.DOUBLE_LIT)) return ast.newLiteral(previous().literal);
        if (match(Lexer.TokenType.STRING_LIT)) return ast.newLiteral(previous().literal);

        if (match(Lexer.TokenType.IDENT)) return ast.newVar(previous().lexeme);

        if (match(Lexer.TokenType.LPAREN)) {
            Parser.Expr e = expression();
            consume(Lexer.TokenType.RPAREN, "缺少 ')'（括号未闭合）");
            return e;
        }

        err.error(errors, peek(), "无法解析的表达式（期望：常量/标识符/(表达式)）");
        advance();
        return ast.newLiteral(null);
    }

    public boolean match(Lexer.Token.Type... types) {
        for (Lexer.Token.Type t : types) { if (check(t)) { advance(); return true; } }
        return false;
    }
    public boolean check(Lexer.Token.Type type) { return !isAtEnd() && peek().type == type; }
    public boolean checkNext(Lexer.Token.Type type) {
        if (current + 1 >= tokens.size()) return false;
        return tokens.get(current + 1).type == type;
    }
    public Lexer.Token advance() { if (!isAtEnd()) current++; return previous(); }
    public boolean isAtEnd() {
        if (current >= tokens.size()) return true;
        return peek().type == Lexer.TokenType.EOF;
    }
    public Lexer.Token peek() { return tokens.get(Math.min(current, tokens.size() - 1)); }
    public Lexer.Token previous() { return tokens.get(Math.max(current - 1, 0)); }
    public Lexer.Token consume(Lexer.Token.Type type, String message) {
        if (check(type)) return advance();
        err.error(errors, peek(), message);
        return peek();
    }
}
