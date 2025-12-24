package com.zds.parser;

import com.zds.lexer.Lexer;
import java.util.ArrayList;
import java.util.List;

/**
 * 语法分析器 (Parser)
 * 包含 AST 节点定义与解析入口
 */
public class Parser {

    /**
     * 对词法单元列表进行语法分析
     * @param tokens 词法单元列表
     * @param outErrors 用于收集错误的列表
     * @return 分析得到的程序节点
     */
    public static Program analyze(List<Lexer.Token> tokens, List<String> outErrors) {
        List<String> errors = (outErrors != null) ? outErrors : new ArrayList<>();
        ErrorHandling err = new ErrorHandling();
        Factory ast = new Factory();
        Recognizer recognizer = new Recognizer(tokens, errors, err, ast);
        return recognizer.parseProgram();
    }

    // ==========================================
    // AST Nodes (Refactored from AST.java)
    // ==========================================

    /**
     * 程序节点 - 表示整个程序的根节点
     */
    public static class Program {
        public final List<Stmt> statements;
        public Program(List<Stmt> statements) { this.statements = statements; }
    }

    /**
     * 语句接口 - 所有语句类型的基接口
     */
    public interface Stmt {}

    /**
     * 块语句节点 - 表示由大括号包围的一组语句
     */
    public static class Block implements Stmt {
        public final List<Stmt> statements;
        public Block(List<Stmt> statements) { this.statements = statements; }
    }

    /**
     * 变量声明节点 - 表示变量的声明和初始化
     */
    public static class VarDecl implements Stmt {
        public final String typeName;
        public final String name;
        public final Expr init;
        public VarDecl(String typeName, String name, Expr init) {
            this.typeName = typeName;
            this.name = name;
            this.init = init;
        }
    }

    /**
     * 赋值语句节点 - 表示变量的赋值操作
     */
    public static class Assign implements Stmt {
        public final String name;
        public final Expr value;
        public Assign(String name, Expr value) {
            this.name = name;
            this.value = value;
        }
    }

    /**
     * If语句节点 - 表示条件语句
     */
    public static class IfStmt implements Stmt {
        public final Expr condition;
        public final Stmt thenBranch;
        public final Stmt elseBranch;
        public IfStmt(Expr condition, Stmt thenBranch, Stmt elseBranch) {
            this.condition = condition;
            this.thenBranch = thenBranch;
            this.elseBranch = elseBranch;
        }
    }

    /**
     * While循环语句节点 - 表示while循环
     */
    public static class WhileStmt implements Stmt {
        public final Expr condition;
        public final Stmt body;
        public WhileStmt(Expr condition, Stmt body) {
            this.condition = condition;
            this.body = body;
        }
    }

    /**
     * For循环语句节点 - 表示for循环
     */
    public static class ForStmt implements Stmt {
        public final Stmt init;
        public final Expr cond;
        public final Stmt step;
        public final Stmt body;
        public ForStmt(Stmt init, Expr cond, Stmt step, Stmt body) {
            this.init = init;
            this.cond = cond;
            this.step = step;
            this.body = body;
        }
    }

    /**
     * 表达式语句节点 - 表示以分号结尾的表达式
     */
    public static class ExprStmt implements Stmt {
        public final Expr expr;
        public ExprStmt(Expr expr) { this.expr = expr; }
    }

    /**
     * 表达式接口 - 所有表达式类型的基接口
     */
    public interface Expr {}

    /**
     * 二元表达式节点 - 表示二元运算（如加减乘除、比较运算等）
     */
    public static class Binary implements Expr {
        public final String op;
        public final Expr left, right;
        public Binary(String op, Expr left, Expr right) {
            this.op = op;
            this.left = left;
            this.right = right;
        }
    }

    /**
     * 一元表达式节点 - 表示一元运算（如正负号）
     */
    public static class Unary implements Expr {
        public final String op;
        public final Expr expr;
        public Unary(String op, Expr expr) {
            this.op = op;
            this.expr = expr;
        }
    }

    /**
     * 字面量表达式节点 - 表示常量值（如数字、字符串等）
     */
    public static class Literal implements Expr {
        public final Object value;
        public Literal(Object value) { this.value = value; }
    }

    /**
     * 变量表达式节点 - 表示变量引用
     */
    public static class Var implements Expr {
        public final String name;
        public Var(String name) { this.name = name; }
    }

    /**
     * AST节点工厂类 - 提供创建各种AST节点的便捷方法
     */
    public static class Factory {
        public Program newProgram(List<Stmt> statements) { return new Program(statements); }
        public Block newBlock(List<Stmt> statements) { return new Block(statements); }
        public VarDecl newVarDecl(String typeName, String name, Expr init) { return new VarDecl(typeName, name, init); }
        public Assign newAssign(String name, Expr value) { return new Assign(name, value); }
        public IfStmt newIfStmt(Expr cond, Stmt thenBranch, Stmt elseBranch) { return new IfStmt(cond, thenBranch, elseBranch); }
        public WhileStmt newWhileStmt(Expr cond, Stmt body) { return new WhileStmt(cond, body); }
        public ForStmt newForStmt(Stmt init, Expr cond, Stmt step, Stmt body) { return new ForStmt(init, cond, step, body); }
        public ExprStmt newExprStmt(Expr expr) { return new ExprStmt(expr); }
        public Binary newBinary(String op, Expr left, Expr right) { return new Binary(op, left, right); }
        public Unary newUnary(String op, Expr expr) { return new Unary(op, expr); }
        public Literal newLiteral(Object value) { return new Literal(value); }
        public Var newVar(String name) { return new Var(name); }
    }

    /**
     * AST打印器类 - 提供将AST转换为字符串表示的功能
     */
    public static class Printer {
        /**
         * 将程序节点转换为字符串表示
         */
        public static String print(Program program) {
            StringBuilder sb = new StringBuilder();
            sb.append("Program\n");
            for (Stmt s : program.statements) {
                printStmt(sb, s, 1);
            }
            return sb.toString();
        }

        private static void printStmt(StringBuilder sb, Stmt s, int indent) {
            if (s == null) { indent(sb, indent).append("(null stmt)\n"); return; }

            if (s instanceof Block) {
                Block b = (Block) s;
                indent(sb, indent).append("Block\n");
                for (Stmt child : b.statements) printStmt(sb, child, indent + 1);
                return;
            }

            if (s instanceof VarDecl) {
                VarDecl d = (VarDecl) s;
                indent(sb, indent).append("VarDecl type=").append(d.typeName)
                        .append(" name=").append(d.name).append("\n");
                if (d.init != null) {
                    indent(sb, indent + 1).append("init:\n");
                    printExpr(sb, d.init, indent + 2);
                }
                return;
            }

            if (s instanceof Assign) {
                Assign a = (Assign) s;
                indent(sb, indent).append("Assign name=").append(a.name).append("\n");
                indent(sb, indent + 1).append("value:\n");
                printExpr(sb, a.value, indent + 2);
                return;
            }

            if (s instanceof IfStmt) {
                IfStmt i = (IfStmt) s;
                indent(sb, indent).append("IfStmt\n");
                indent(sb, indent + 1).append("cond:\n");
                printExpr(sb, i.condition, indent + 2);
                indent(sb, indent + 1).append("then:\n");
                printStmt(sb, i.thenBranch, indent + 2);
                if (i.elseBranch != null) {
                    indent(sb, indent + 1).append("else:\n");
                    printStmt(sb, i.elseBranch, indent + 2);
                }
                return;
            }

            if (s instanceof WhileStmt) {
                WhileStmt w = (WhileStmt) s;
                indent(sb, indent).append("WhileStmt\n");
                indent(sb, indent + 1).append("cond:\n");
                printExpr(sb, w.condition, indent + 2);
                indent(sb, indent + 1).append("body:\n");
                printStmt(sb, w.body, indent + 2);
                return;
            }

            if (s instanceof ForStmt) {
                ForStmt f = (ForStmt) s;
                indent(sb, indent).append("ForStmt\n");

                indent(sb, indent + 1).append("init:\n");
                if (f.init != null) printStmt(sb, f.init, indent + 2);
                else indent(sb, indent + 2).append("(empty)\n");

                indent(sb, indent + 1).append("cond:\n");
                if (f.cond != null) printExpr(sb, f.cond, indent + 2);
                else indent(sb, indent + 2).append("(empty)\n");

                indent(sb, indent + 1).append("step:\n");
                if (f.step != null) printStmt(sb, f.step, indent + 2);
                else indent(sb, indent + 2).append("(empty)\n");

                indent(sb, indent + 1).append("body:\n");
                printStmt(sb, f.body, indent + 2);
                return;
            }

            if (s instanceof ExprStmt) {
                ExprStmt e = (ExprStmt) s;
                indent(sb, indent).append("ExprStmt\n");
                printExpr(sb, e.expr, indent + 1);
                return;
            }

            indent(sb, indent).append("UnknownStmt: ").append(s.getClass().getSimpleName()).append("\n");
        }

        private static void printExpr(StringBuilder sb, Expr e, int indent) {
            if (e == null) { indent(sb, indent).append("(null expr)\n"); return; }

            if (e instanceof Binary) {
                Binary b = (Binary) e;
                indent(sb, indent).append("Binary op=").append(b.op).append("\n");
                indent(sb, indent + 1).append("left:\n");
                printExpr(sb, b.left, indent + 2);
                indent(sb, indent + 1).append("right:\n");
                printExpr(sb, b.right, indent + 2);
                return;
            }

            if (e instanceof Unary) {
                Unary u = (Unary) e;
                indent(sb, indent).append("Unary op=").append(u.op).append("\n");
                printExpr(sb, u.expr, indent + 1);
                return;
            }

            if (e instanceof Literal) {
                Literal l = (Literal) e;
                indent(sb, indent).append("Literal value=").append(formatLiteral(l.value)).append("\n");
                return;
            }

            if (e instanceof Var) {
                Var v = (Var) e;
                indent(sb, indent).append("Var name=").append(v.name).append("\n");
                return;
            }

            indent(sb, indent).append("UnknownExpr: ").append(e.getClass().getSimpleName()).append("\n");
        }

        private static StringBuilder indent(StringBuilder sb, int n) {
            for (int i = 0; i < n; i++) sb.append("  ");
            return sb;
        }

        private static String formatLiteral(Object v) {
            if (v == null) return "null";
            if (v instanceof String) {
                String s = (String) v;
                return "\"" + s.replace("\n", "\\n").replace("\t", "\\t") + "\"";
            }
            return String.valueOf(v);
        }
    }
}
