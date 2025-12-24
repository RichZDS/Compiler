package com.zds.IR;

import com.zds.Semantic.Semantic;
import com.zds.Semantic.Semantic.Result;
import com.zds.parser.AST;

import java.util.*;

/**
 * 中间代码 IR（Intermediate Representation）
 * 四元式（Quadruple / Three-address code）
 *
 * 输入：AST.Program (+ Semantic.Result)
 * 输出：List<Quad>
 *
 * Quad: (op, arg1, arg2, result)
 * 例：
 *   (:=, 3, _, a)
 *   (+, a, b, t1)
 *   (j<, a, 10, L1)
 *   (label, L1, _, _)
 */
public class IR {

    public static class Quad {
        public final String op;
        public final String arg1;
        public final String arg2;
        public final String result;

        public Quad(String op, String arg1, String arg2, String result) {
            this.op = op;
            this.arg1 = arg1;
            this.arg2 = arg2;
            this.result = result;
        }

        @Override
        public String toString() {
            return "(" + op + ", " + arg1 + ", " + arg2 + ", " + result + ")";
        }
    }

    // Facade
    public static List<Quad> generate(AST.Program program, Semantic.Result sem, List<String> outErrors) {
        Generator g = new Generator(sem);
        List<Quad> quads = g.run(program);
        if (outErrors != null) outErrors.addAll(g.errors);
        return quads;
    }

    public static void print(List<Quad> quads) {
        for (int i = 0; i < quads.size(); i++) {
            System.out.printf("%d: %s%n", i, quads.get(i));
        }
    }

    // Worker
    private static class Generator {
        private final Semantic.Result sem;
        private final List<Quad> out = new ArrayList<>();
        private final List<String> errors = new ArrayList<>();

        private int tempId = 0;
        private int labelId = 0;

        Generator(Semantic.Result sem) {
            this.sem = sem;
        }

        List<Quad> run(AST.Program program) {
            if (program != null) {
                for (AST.Stmt s : program.statements) {
                    genStmt(s);
                }
            }
            return out;
        }

        // -------- stmt --------
        private void genStmt(AST.Stmt stmt) {
            if (stmt == null) return;

            if (stmt instanceof AST.Block) {
                AST.Block b = (AST.Block) stmt;
                for (AST.Stmt s : b.statements) genStmt(s);
                return;
            }

            if (stmt instanceof AST.VarDecl) {
                AST.VarDecl d = (AST.VarDecl) stmt;
                if (d.init != null) {
                    String rhs = genExpr(d.init);
                    emit(":=", rhs, "_", d.name);
                }
                return;
            }

            if (stmt instanceof AST.Assign) {
                AST.Assign a = (AST.Assign) stmt;
                String rhs = genExpr(a.value);
                emit(":=", rhs, "_", a.name);
                return;
            }

            if (stmt instanceof AST.ExprStmt) {
                AST.ExprStmt e = (AST.ExprStmt) stmt;
                genExpr(e.expr); // 计算但丢弃结果
                return;
            }

            if (stmt instanceof AST.IfStmt) {
                AST.IfStmt i = (AST.IfStmt) stmt;
                String L_then = newLabel();
                String L_else = newLabel();
                String L_end  = newLabel();

                emitCondJump(i.condition, L_then, L_else);

                emit("label", L_then, "_", "_");
                genStmt(i.thenBranch);
                emit("j", "_", "_", L_end);

                emit("label", L_else, "_", "_");
                if (i.elseBranch != null) genStmt(i.elseBranch);

                emit("label", L_end, "_", "_");
                return;
            }

            if (stmt instanceof AST.WhileStmt) {
                AST.WhileStmt w = (AST.WhileStmt) stmt;
                String L_begin = newLabel();
                String L_body  = newLabel();
                String L_end   = newLabel();

                emit("label", L_begin, "_", "_");
                emitCondJump(w.condition, L_body, L_end);

                emit("label", L_body, "_", "_");
                genStmt(w.body);
                emit("j", "_", "_", L_begin);

                emit("label", L_end, "_", "_");
                return;
            }

            if (stmt instanceof AST.ForStmt) {
                AST.ForStmt f = (AST.ForStmt) stmt;

                // init
                if (f.init != null) genStmt(f.init);

                String L_begin = newLabel();
                String L_body  = newLabel();
                String L_end   = newLabel();

                emit("label", L_begin, "_", "_");

                if (f.cond != null) {
                    emitCondJump(f.cond, L_body, L_end);
                } else {
                    // cond 为空：视为 true
                    emit("j", "_", "_", L_body);
                }

                emit("label", L_body, "_", "_");
                genStmt(f.body);

                // step
                if (f.step != null) genStmt(f.step);

                emit("j", "_", "_", L_begin);
                emit("label", L_end, "_", "_");
                return;
            }

            errors.add("IR错误: 未知语句类型 " + stmt.getClass().getSimpleName());
        }

        // -------- expr --------
        private String genExpr(AST.Expr expr) {
            if (expr == null) return "0";

            if (expr instanceof AST.Literal) {
                Object v = ((AST.Literal) expr).value;
                if (v == null) return "0";
                if (v instanceof String) return "\"" + v + "\"";
                return String.valueOf(v);
            }

            if (expr instanceof AST.Var) {
                return ((AST.Var) expr).name;
            }

            if (expr instanceof AST.Unary) {
                AST.Unary u = (AST.Unary) expr;
                String x = genExpr(u.expr);

                if (u.op.equals("+")) {
                    return x;
                }
                if (u.op.equals("-")) {
                    String t = newTemp();
                    emit("neg", x, "_", t);
                    return t;
                }

                errors.add("IR错误: 未知一元运算符 " + u.op);
                return x;
            }

            if (expr instanceof AST.Binary) {
                AST.Binary b = (AST.Binary) expr;
                String a = genExpr(b.left);
                String c = genExpr(b.right);

                // 算术 / 比较 都先按“产生一个临时量”处理（比较通常用于条件跳转时会走 emitCondJump）
                String t = newTemp();
                emit(b.op, a, c, t);
                return t;
            }

            errors.add("IR错误: 未知表达式类型 " + expr.getClass().getSimpleName());
            return "0";
        }

        // -------- cond jump（控制流关键）--------
        private void emitCondJump(AST.Expr cond, String trueLabel, String falseLabel) {
            // 期望：cond 是比较 Binary（< <= > >= == !=）
            if (cond instanceof AST.Binary) {
                AST.Binary b = (AST.Binary) cond;
                if (isRelOp(b.op)) {
                    String left = genExpr(b.left);
                    String right = genExpr(b.right);
                    emit("j" + b.op, left, right, trueLabel);
                    emit("j", "_", "_", falseLabel);
                    return;
                }
            }

            // fallback：cond 不是比较表达式，就用 “cond != 0” 作为真
            String place = genExpr(cond);
            emit("j!=", place, "0", trueLabel);
            emit("j", "_", "_", falseLabel);
        }

        private boolean isRelOp(String op) {
            return op.equals(">") || op.equals(">=") || op.equals("<") || op.equals("<=")
                    || op.equals("==") || op.equals("!=");
        }

        // -------- utils --------
        private void emit(String op, String a1, String a2, String res) {
            out.add(new Quad(op, a1, a2, res));
        }

        private String newTemp() {
            tempId++;
            return "t" + tempId;
        }

        private String newLabel() {
            labelId++;
            return "L" + labelId;
        }
    }
}
