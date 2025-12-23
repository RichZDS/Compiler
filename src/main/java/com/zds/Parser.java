package com.zds;

import java.util.ArrayList;
import java.util.List;

/*
 *@auther 郑笃实
 *@version 1.0
 */

/**
 * 语法分析器（Syntax Analyzer）
 *
 * ✅ PPT 里的说法：语法分析（自顶向下）——LL(1) 预测分析
 * 这里用“递归下降 Recursive Descent”来实现（本质就是 LL(1) 的程序化写法）。
 *
 * 当前阶段目标：
 * - 把 Main + Lexer + Parser 串起来，Parser 先做“语法正确性检查”
 * - 并且预留 AST 结构，后续语义分析/四元式生成可以直接接。
 *
 * 支持的语言子集（你们目前定的）：
 * - 类型：int/double/string
 * - 语句：声明、赋值、块{...}、if/else、while、for
 * - 表达式：+ - * /，括号
 * - 条件：比较运算 > < >= <= == !=
 */
public class Parser {

    // ============== AST（预留） ==============
    public static class Program {
        public final List<Stmt> statements;
        public Program(List<Stmt> statements) { this.statements = statements; }
    }

    public interface Stmt {}

    public static class Block implements Stmt {
        public final List<Stmt> statements;
        public Block(List<Stmt> statements) { this.statements = statements; }
    }

    public static class VarDecl implements Stmt {
        public final String typeName; // "int"/"double"/"string"
        public final String name;
        public final Expr init; // 允许 null
        public VarDecl(String typeName, String name, Expr init) {
            this.typeName = typeName; this.name = name; this.init = init;
        }
    }

    public static class Assign implements Stmt {
        public final String name;
        public final Expr value;
        public Assign(String name, Expr value) { this.name = name; this.value = value; }
    }

    public static class IfStmt implements Stmt {
        public final Expr condition;
        public final Stmt thenBranch;
        public final Stmt elseBranch; // 可为 null
        public IfStmt(Expr condition, Stmt thenBranch, Stmt elseBranch) {
            this.condition = condition; this.thenBranch = thenBranch; this.elseBranch = elseBranch;
        }
    }

    public static class WhileStmt implements Stmt {
        public final Expr condition;
        public final Stmt body;
        public WhileStmt(Expr condition, Stmt body) { this.condition = condition; this.body = body; }
    }

    public static class ForStmt implements Stmt {
        public final Stmt init;     // VarDecl / Assign / null
        public final Expr cond;     // 可为 null
        public final Stmt step;     // Assign / null
        public final Stmt body;
        public ForStmt(Stmt init, Expr cond, Stmt step, Stmt body) {
            this.init = init; this.cond = cond; this.step = step; this.body = body;
        }
    }

    public static class ExprStmt implements Stmt {
        public final Expr expr;
        public ExprStmt(Expr expr) { this.expr = expr; }
    }

    public interface Expr {}
    public static class Binary implements Expr {
        public final String op; public final Expr left, right;
        public Binary(String op, Expr left, Expr right) { this.op = op; this.left = left; this.right = right; }
    }
    public static class Unary implements Expr {
        public final String op; public final Expr expr;
        public Unary(String op, Expr expr) { this.op = op; this.expr = expr; }
    }
    public static class Literal implements Expr {
        public final Object value;
        public Literal(Object value) { this.value = value; }
    }
    public static class Var implements Expr {
        public final String name;
        public Var(String name) { this.name = name; }
    }

    // ============== Parser 本体 ==============
    private final List<Lexer.Token> tokens;
    private int current = 0;

    private final List<String> errors = new ArrayList<>();
    private Program program;

    public Parser(List<Lexer.Token> tokens) {
        this.tokens = (tokens == null) ? List.of() : tokens;
    }

    public List<String> getErrors() { return errors; }
    public Program getProgram() { return program; }

    public Program parse() {
        List<Stmt> stmts = new ArrayList<>();
        while (!isAtEnd()) {
            if (peek().type == Lexer.TokenType.ERROR) { advance(); continue; }
            Stmt s = statement();
            if (s != null) stmts.add(s);
            else synchronize();
        }
        program = new Program(stmts);
        return program;
    }

    // -------- Statements --------
    private Stmt statement() {
        if (match(Lexer.TokenType.LBRACE)) return block();
        if (match(Lexer.TokenType.IF)) return ifStatement();
        if (match(Lexer.TokenType.WHILE)) return whileStatement();
        if (match(Lexer.TokenType.FOR)) return forStatement();

        if (match(Lexer.TokenType.INT, Lexer.TokenType.DOUBLE, Lexer.TokenType.STRING)) {
            return declaration(previous());
        }

        if (check(Lexer.TokenType.IDENT) && checkNext(Lexer.TokenType.ASSIGN)) {
            Stmt a = assignmentStatement();
            consume(Lexer.TokenType.SEMI, "缺少 ';'（赋值语句必须以分号结尾）");
            return a;
        }

        Expr e = expression();
        consume(Lexer.TokenType.SEMI, "缺少 ';'（表达式语句必须以分号结尾）");
        return new ExprStmt(e);
    }

    private Block block() {
        List<Stmt> stmts = new ArrayList<>();
        while (!check(Lexer.TokenType.RBRACE) && !isAtEnd()) {
            Stmt s = statement();
            if (s != null) stmts.add(s);
            else synchronize();
        }
        consume(Lexer.TokenType.RBRACE, "缺少 '}'（块语句未闭合）");
        return new Block(stmts);
    }

    private Stmt declaration(Lexer.Token typeToken) {
        String typeName = typeToken.lexeme;
        Lexer.Token name = consume(Lexer.TokenType.IDENT, "声明语句缺少标识符（变量名）");

        Expr init = null;
        if (match(Lexer.TokenType.ASSIGN)) init = expression();

        consume(Lexer.TokenType.SEMI, "缺少 ';'（声明语句必须以分号结尾）");
        return new VarDecl(typeName, name.lexeme, init);
    }

    private Stmt assignmentStatement() {
        Lexer.Token name = consume(Lexer.TokenType.IDENT, "赋值语句缺少标识符（变量名）");
        consume(Lexer.TokenType.ASSIGN, "赋值语句缺少 '='");
        Expr value = expression();
        return new Assign(name.lexeme, value);
    }

    private Stmt ifStatement() {
        consume(Lexer.TokenType.LPAREN, "if 条件缺少 '('");
        Expr cond = condition();
        consume(Lexer.TokenType.RPAREN, "if 条件缺少 ')'");

        Stmt thenBranch = statement();
        Stmt elseBranch = null;
        if (match(Lexer.TokenType.ELSE)) elseBranch = statement();
        return new IfStmt(cond, thenBranch, elseBranch);
    }

    private Stmt whileStatement() {
        consume(Lexer.TokenType.LPAREN, "while 条件缺少 '('");
        Expr cond = condition();
        consume(Lexer.TokenType.RPAREN, "while 条件缺少 ')'");
        Stmt body = statement();
        return new WhileStmt(cond, body);
    }

    private Stmt forStatement() {
        consume(Lexer.TokenType.LPAREN, "for 缺少 '('");

        Stmt init = null;
        if (match(Lexer.TokenType.SEMI)) {
            // empty init
        } else if (match(Lexer.TokenType.INT, Lexer.TokenType.DOUBLE, Lexer.TokenType.STRING)) {
            Lexer.Token typeToken = previous();
            String typeName = typeToken.lexeme;
            Lexer.Token name = consume(Lexer.TokenType.IDENT, "for-init 声明缺少变量名");
            Expr initExpr = null;
            if (match(Lexer.TokenType.ASSIGN)) initExpr = expression();
            init = new VarDecl(typeName, name.lexeme, initExpr);
            consume(Lexer.TokenType.SEMI, "for-init 缺少 ';'");
        } else if (check(Lexer.TokenType.IDENT) && checkNext(Lexer.TokenType.ASSIGN)) {
            init = assignmentStatement();
            consume(Lexer.TokenType.SEMI, "for-init 缺少 ';'");
        } else {
            error(peek(), "for-init 只能是：声明/赋值/空（如 for(;...;...)）");
            synchronizeInFor();
        }

        Expr cond = null;
        if (!check(Lexer.TokenType.SEMI)) cond = condition();
        consume(Lexer.TokenType.SEMI, "for-cond 缺少 ';'");

        Stmt step = null;
        if (!check(Lexer.TokenType.RPAREN)) {
            if (check(Lexer.TokenType.IDENT) && checkNext(Lexer.TokenType.ASSIGN)) step = assignmentStatement();
            else {
                error(peek(), "for-step 目前仅支持赋值形式：i = i + 1");
                synchronizeInFor();
            }
        }
        consume(Lexer.TokenType.RPAREN, "for 缺少 ')'");

        Stmt body = statement();
        return new ForStmt(init, cond, step, body);
    }

    // -------- Condition --------
    private Expr condition() {
        Expr left = expression();
        if (match(Lexer.TokenType.GT, Lexer.TokenType.GE, Lexer.TokenType.LT, Lexer.TokenType.LE,
                Lexer.TokenType.EQ, Lexer.TokenType.NE)) {
            String op = previous().lexeme;
            Expr right = expression();
            return new Binary(op, left, right);
        }
        return left;
    }

    // -------- Expression (乘除 > 加减) --------
    private Expr expression() { return additive(); }

    private Expr additive() {
        Expr expr = multiplicative();
        while (match(Lexer.TokenType.PLUS, Lexer.TokenType.MINUS)) {
            String op = previous().lexeme;
            Expr right = multiplicative();
            expr = new Binary(op, expr, right);
        }
        return expr;
    }

    private Expr multiplicative() {
        Expr expr = unary();
        while (match(Lexer.TokenType.MUL, Lexer.TokenType.DIV)) {
            String op = previous().lexeme;
            Expr right = unary();
            expr = new Binary(op, expr, right);
        }
        return expr;
    }

    private Expr unary() {
        if (match(Lexer.TokenType.PLUS, Lexer.TokenType.MINUS)) {
            String op = previous().lexeme;
            return new Unary(op, unary());
        }
        return primary();
    }

    private Expr primary() {
        if (match(Lexer.TokenType.INT_LIT)) return new Literal(previous().literal);
        if (match(Lexer.TokenType.DOUBLE_LIT)) return new Literal(previous().literal);
        if (match(Lexer.TokenType.STRING_LIT)) return new Literal(previous().literal);

        if (match(Lexer.TokenType.IDENT)) return new Var(previous().lexeme);

        if (match(Lexer.TokenType.LPAREN)) {
            Expr e = expression();
            consume(Lexer.TokenType.RPAREN, "缺少 ')'（括号未闭合）");
            return e;
        }

        error(peek(), "无法解析的表达式（期望：常量/标识符/(表达式)）");
        return new Literal(null);
    }

    // -------- Helpers --------
    private boolean match(Lexer.TokenType... types) {
        for (Lexer.TokenType t : types) if (check(t)) { advance(); return true; }
        return false;
    }

    private boolean check(Lexer.TokenType type) { return !isAtEnd() && peek().type == type; }

    private boolean checkNext(Lexer.TokenType type) {
        if (current + 1 >= tokens.size()) return false;
        return tokens.get(current + 1).type == type;
    }

    private Lexer.Token advance() { if (!isAtEnd()) current++; return previous(); }

    private boolean isAtEnd() { return peek().type == Lexer.TokenType.EOF; }

    private Lexer.Token peek() { return tokens.get(current); }

    private Lexer.Token previous() { return tokens.get(current - 1); }

    private Lexer.Token consume(Lexer.TokenType type, String message) {
        if (check(type)) return advance();
        error(peek(), message);
        return peek();
    }

    private void error(Lexer.Token token, String message) {
        errors.add(String.format("(%d:%d) 语法错误：%s，遇到 %s('%s')",
                token.line, token.col, message, token.type, token.lexeme));
    }

    /** panic-mode 错误恢复：跳到 ';' 或下一条语句起始 */
    private void synchronize() {
        advance();
        while (!isAtEnd()) {
            if (previous().type == Lexer.TokenType.SEMI) return;
            switch (peek().type) {
                case IF:
                case FOR:
                case WHILE:
                case INT:
                case DOUBLE:
                case STRING:
                case LBRACE:
                    return;
                default:
                    break;
            }
            advance();
        }
    }

    /** for(...) 内部同步：尽量跳到 ';' 或 ')' */
    private void synchronizeInFor() {
        while (!isAtEnd() && peek().type != Lexer.TokenType.SEMI && peek().type != Lexer.TokenType.RPAREN) {
            advance();
        }
    }
}
