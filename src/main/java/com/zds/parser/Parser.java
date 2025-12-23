package com.zds.parser;

import com.zds.lexer.Token;

/**
 * 语法分析器（Syntax Analyzer）
 *
 * ✅ PPT 里的说法：语法分析（自顶向下）——LL(1) 预测分析
 * 这里用"递归下降 Recursive Descent"来实现（本质就是 LL(1) 的程序化写法）。
 *
 * 当前阶段目标：
 * - 把 Main + Lexer + Parser 串起来，Parser 先做"语法正确性检查"
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
        public final java.util.List<Stmt> statements;
        public Program(java.util.List<Stmt> statements) { this.statements = statements; }
    }

    public interface Stmt {}

    public static class Block implements Stmt {
        public final java.util.List<Stmt> statements;
        public Block(java.util.List<Stmt> statements) { this.statements = statements; }
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
    private final java.util.List<Token> tokens;
    private int current = 0;

    private final java.util.List<String> errors = new java.util.ArrayList<>();
    private Program program;

    public Parser(java.util.List<Token> tokens) {
        this.tokens = (tokens == null) ? java.util.List.of() : tokens;
    }

    public java.util.List<String> getErrors() { return errors; }
    public Program getProgram() { return program; }

    public Program parse() {
        java.util.List<Stmt> stmts = new java.util.ArrayList<>();
        while (!isAtEnd()) {
            if (peek().type == com.zds.lexer.consts.TokenType.ERROR) { advance(); continue; }
            Stmt s = statement();
            if (s != null) stmts.add(s);
            else synchronize();
        }
        program = new Program(stmts);
        return program;
    }

    // -------- Statements --------
    /**
     * 解析语句
     * 语句类型包括：块语句、if语句、while语句、for语句、声明语句、赋值语句、表达式语句
     */
    private Stmt statement() {
        if (match(com.zds.lexer.consts.TokenType.LBRACE)) return block();
        if (match(com.zds.lexer.consts.TokenType.IF)) return ifStatement();
        if (match(com.zds.lexer.consts.TokenType.WHILE)) return whileStatement();
        if (match(com.zds.lexer.consts.TokenType.FOR)) return forStatement();

        if (match(com.zds.lexer.consts.TokenType.INT, com.zds.lexer.consts.TokenType.DOUBLE, com.zds.lexer.consts.TokenType.STRING)) {
            return declaration(previous());
        }

        if (check(com.zds.lexer.consts.TokenType.IDENT) && checkNext(com.zds.lexer.consts.TokenType.ASSIGN)) {
            Stmt a = assignmentStatement();
            consume(com.zds.lexer.consts.TokenType.SEMI, "缺少 ';'（赋值语句必须以分号结尾）");
            return a;
        }

        Expr e = expression();
        consume(com.zds.lexer.consts.TokenType.SEMI, "缺少 ';'（表达式语句必须以分号结尾）");
        return new ExprStmt(e);
    }

    /**
     * 解析块语句
     * 块语句由花括号包围的语句序列组成
     */
    private Block block() {
        java.util.List<Stmt> stmts = new java.util.ArrayList<>();
        while (!check(com.zds.lexer.consts.TokenType.RBRACE) && !isAtEnd()) {
            Stmt s = statement();
            if (s != null) stmts.add(s);
            else synchronize();
        }
        consume(com.zds.lexer.consts.TokenType.RBRACE, "缺少 '}'（块语句未闭合）");
        return new Block(stmts);
    }

    /**
     * 解析声明语句
     * 如：int a = 10;
     */
    private Stmt declaration(Token typeToken) {
        String typeName = typeToken.lexeme;
        Token name = consume(com.zds.lexer.consts.TokenType.IDENT, "声明语句缺少标识符（变量名）");

        Expr init = null;
        if (match(com.zds.lexer.consts.TokenType.ASSIGN)) init = expression();

        consume(com.zds.lexer.consts.TokenType.SEMI, "缺少 ';'（声明语句必须以分号结尾）");
        return new VarDecl(typeName, name.lexeme, init);
    }

    /**
     * 解析赋值语句
     * 如：a = 10;
     */
    private Stmt assignmentStatement() {
        Token name = consume(com.zds.lexer.consts.TokenType.IDENT, "赋值语句缺少标识符（变量名）");
        consume(com.zds.lexer.consts.TokenType.ASSIGN, "赋值语句缺少 '='");
        Expr value = expression();
        return new Assign(name.lexeme, value);
    }

    /**
     * 解析if语句
     * 支持if-else结构
     */
    private Stmt ifStatement() {
        consume(com.zds.lexer.consts.TokenType.LPAREN, "if 条件缺少 '('");
        Expr cond = condition();
        consume(com.zds.lexer.consts.TokenType.RPAREN, "if 条件缺少 ')'");

        Stmt thenBranch = statement();
        Stmt elseBranch = null;
        if (match(com.zds.lexer.consts.TokenType.ELSE)) elseBranch = statement();
        return new IfStmt(cond, thenBranch, elseBranch);
    }

    /**
     * 解析while循环语句
     */
    private Stmt whileStatement() {
        consume(com.zds.lexer.consts.TokenType.LPAREN, "while 条件缺少 '('");
        Expr cond = condition();
        consume(com.zds.lexer.consts.TokenType.RPAREN, "while 条件缺少 ')'");
        Stmt body = statement();
        return new WhileStmt(cond, body);
    }

    /**
     * 解析for循环语句
     * 支持完整的for循环结构：for(init; condition; step)
     */
    private Stmt forStatement() {
        consume(com.zds.lexer.consts.TokenType.LPAREN, "for 缺少 '('");

        Stmt init = null;
        if (match(com.zds.lexer.consts.TokenType.SEMI)) {
            // empty init
        } else if (match(com.zds.lexer.consts.TokenType.INT, com.zds.lexer.consts.TokenType.DOUBLE, com.zds.lexer.consts.TokenType.STRING)) {
            Token typeToken = previous();
            String typeName = typeToken.lexeme;
            Token name = consume(com.zds.lexer.consts.TokenType.IDENT, "for-init 声明缺少变量名");
            Expr initExpr = null;
            if (match(com.zds.lexer.consts.TokenType.ASSIGN)) initExpr = expression();
            init = new VarDecl(typeName, name.lexeme, initExpr);
            consume(com.zds.lexer.consts.TokenType.SEMI, "for-init 缺少 ';'");
        } else if (check(com.zds.lexer.consts.TokenType.IDENT) && checkNext(com.zds.lexer.consts.TokenType.ASSIGN)) {
            init = assignmentStatement();
            consume(com.zds.lexer.consts.TokenType.SEMI, "for-init 缺少 ';'");
        } else {
            error(peek(), "for-init 只能是：声明/赋值/空（如 for(;...;...)）");
            synchronizeInFor();
        }

        Expr cond = null;
        if (!check(com.zds.lexer.consts.TokenType.SEMI)) cond = condition();
        consume(com.zds.lexer.consts.TokenType.SEMI, "for-cond 缺少 ';'");

        Stmt step = null;
        if (!check(com.zds.lexer.consts.TokenType.RPAREN)) {
            if (check(com.zds.lexer.consts.TokenType.IDENT) && checkNext(com.zds.lexer.consts.TokenType.ASSIGN)) step = assignmentStatement();
            else {
                error(peek(), "for-step 目前仅支持赋值形式：i = i + 1");
                synchronizeInFor();
            }
        }
        consume(com.zds.lexer.consts.TokenType.RPAREN, "for 缺少 ')'");

        Stmt body = statement();
        return new ForStmt(init, cond, step, body);
    }

    // -------- Condition --------
    /**
     * 解析条件表达式
     * 支持比较运算符：> >= < <= == !=
     */
    private Expr condition() {
        Expr left = expression();
        if (match(com.zds.lexer.consts.TokenType.GT, com.zds.lexer.consts.TokenType.GE, com.zds.lexer.consts.TokenType.LT, com.zds.lexer.consts.TokenType.LE,
                com.zds.lexer.consts.TokenType.EQ, com.zds.lexer.consts.TokenType.NE)) {
            String op = previous().lexeme;
            Expr right = expression();
            return new Binary(op, left, right);
        }
        return left;
    }

    // -------- Expression (乘除 > 加减) --------
    /**
     * 解析表达式（顶层）
     * 当前简单地调用加法表达式
     */
    private Expr expression() { return additive(); }

    /**
     * 解析加法表达式（加法和减法）
     * 优先级低于乘除法
     */
    private Expr additive() {
        Expr expr = multiplicative();
        while (match(com.zds.lexer.consts.TokenType.PLUS, com.zds.lexer.consts.TokenType.MINUS)) {
            String op = previous().lexeme;
            Expr right = multiplicative();
            expr = new Binary(op, expr, right);
        }
        return expr;
    }

    /**
     * 解析乘法表达式（乘法和除法）
     * 优先级高于加减法
     */
    private Expr multiplicative() {
        Expr expr = unary();
        while (match(com.zds.lexer.consts.TokenType.MUL, com.zds.lexer.consts.TokenType.DIV)) {
            String op = previous().lexeme;
            Expr right = unary();
            expr = new Binary(op, expr, right);
        }
        return expr;
    }

    /**
     * 解析一元表达式
     * 支持正负号：+ -
     */
    private Expr unary() {
        if (match(com.zds.lexer.consts.TokenType.PLUS, com.zds.lexer.consts.TokenType.MINUS)) {
            String op = previous().lexeme;
            return new Unary(op, unary());
        }
        return primary();
    }

    /**
     * 解析基本表达式（最低优先级）
     * 包括字面量、标识符、括号表达式
     */
    private Expr primary() {
        if (match(com.zds.lexer.consts.TokenType.INT_LIT)) return new Literal(previous().literal);
        if (match(com.zds.lexer.consts.TokenType.DOUBLE_LIT)) return new Literal(previous().literal);
        if (match(com.zds.lexer.consts.TokenType.STRING_LIT)) return new Literal(previous().literal);

        if (match(com.zds.lexer.consts.TokenType.IDENT)) return new Var(previous().lexeme);

        if (match(com.zds.lexer.consts.TokenType.LPAREN)) {
            Expr e = expression();
            consume(com.zds.lexer.consts.TokenType.RPAREN, "缺少 ')'（括号未闭合）");
            return e;
        }

        error(peek(), "无法解析的表达式（期望：常量/标识符/(表达式)）");
        return new Literal(null);
    }

    // -------- Helpers --------
    /**
     * 匹配当前Token是否为指定类型之一
     * 如果匹配成功则消耗该Token并返回true，否则返回false
     */
    private boolean match(Token.Type... types) {
        for (Token.Type t : types) if (check(t)) { advance(); return true; }
        return false;
    }

    /**
     * 检查当前Token是否为指定类型
     * 不消耗Token
     */
    private boolean check(Token.Type type) { return !isAtEnd() && peek().type == type; }

    /**
     * 检查下一个Token是否为指定类型
     * 不消耗Token
     */
    private boolean checkNext(Token.Type type) {
        if (current + 1 >= tokens.size()) return false;
        return tokens.get(current + 1).type == type;
    }

    /**
     * 消耗当前Token，移动到下一个Token
     * 返回被消耗的Token
     */
    private Token advance() { if (!isAtEnd()) current++; return previous(); }

    /**
     * 检查是否已到达Token序列末尾
     */
    private boolean isAtEnd() { return peek().type == com.zds.lexer.consts.TokenType.EOF; }

    /**
     * 查看当前Token但不消耗它
     */
    private Token peek() { return tokens.get(current); }

    /**
     * 获取前一个Token
     */
    private Token previous() { return tokens.get(current - 1); }

    /**
     * 消耗指定类型的Token
     * 如果当前Token不匹配指定类型，则报告错误
     */
    private Token consume(Token.Type type, String message) {
        if (check(type)) return advance();
        error(peek(), message);
        return peek();
    }

    /**
     * 记录语法错误
     */
    private void error(Token token, String message) {
        errors.add(String.format("(%d:%d) 语法错误：%s，遇到 %s('%s')",
                token.line, token.col, message, token.type, token.lexeme));
    }

    /** panic-mode 错误恢复：跳到 ';' 或下一条语句起始 */
    private void synchronize() {
        advance();
        while (!isAtEnd()) {
            if (previous().type == com.zds.lexer.consts.TokenType.SEMI) return;
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
        while (!isAtEnd() && peek().type != com.zds.lexer.consts.TokenType.SEMI && peek().type != com.zds.lexer.consts.TokenType.RPAREN) {
            advance();
        }
    }
}