package com.zds.IR;

import com.zds.Semantic.Semantic;
import com.zds.parser.Parser;

import java.util.ArrayList;
import java.util.List;

/**
 * IR 生成器核心实现 (Core Generator)
 * 包级私有类，负责将 AST 转换为四元式列表
 */
class Generator {
    private final Semantic.Result sem;
    private final List<IR.Quad> out = new ArrayList<>();
    private final List<String> errors = new ArrayList<>();

    private int tempId = 0;
    private int labelId = 0;

    Generator(Semantic.Result sem) {
        this.sem = sem;
    }

    public List<String> getErrors() {
        return errors;
    }

    List<IR.Quad> run(Parser.Program program) {
        if (program != null) {
            for (Parser.Stmt s : program.statements) {
                genStmt(s);
            }
        }
        return out;
    }

    // -------- stmt --------
    private void genStmt(Parser.Stmt stmt) {
        if (stmt == null) return;

        if (stmt instanceof Parser.Block) {
            Parser.Block b = (Parser.Block) stmt;
            for (Parser.Stmt s : b.statements) genStmt(s);
            return;
        }

        if (stmt instanceof Parser.VarDecl) {
            Parser.VarDecl d = (Parser.VarDecl) stmt;
            if (d.init != null) {
                String rhs = genExpr(d.init);
                emit(":=", rhs, "_", d.name);
            }
            return;
        }

        if (stmt instanceof Parser.Assign) {
            Parser.Assign a = (Parser.Assign) stmt;
            String rhs = genExpr(a.value);
            emit(":=", rhs, "_", a.name);
            return;
        }

        if (stmt instanceof Parser.ExprStmt) {
            Parser.ExprStmt e = (Parser.ExprStmt) stmt;
            genExpr(e.expr); // 计算但丢弃结果
            return;
        }

        if (stmt instanceof Parser.IfStmt) {
            Parser.IfStmt i = (Parser.IfStmt) stmt;
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

        if (stmt instanceof Parser.WhileStmt) {
            Parser.WhileStmt w = (Parser.WhileStmt) stmt;
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

        if (stmt instanceof Parser.ForStmt) {
            Parser.ForStmt f = (Parser.ForStmt) stmt;

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
    private String genExpr(Parser.Expr expr) {
        if (expr == null) return "0";

        if (expr instanceof Parser.Literal) {
            Object v = ((Parser.Literal) expr).value;
            if (v == null) return "0";
            if (v instanceof String) return "\"" + v + "\"";
            return String.valueOf(v);
        }

        if (expr instanceof Parser.Var) {
            return ((Parser.Var) expr).name;
        }

        if (expr instanceof Parser.Unary) {
            Parser.Unary u = (Parser.Unary) expr;
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

        if (expr instanceof Parser.Binary) {
            Parser.Binary b = (Parser.Binary) expr;
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
    private void emitCondJump(Parser.Expr cond, String trueLabel, String falseLabel) {
        // 期望：cond 是比较 Binary（< <= > >= == !=）
        if (cond instanceof Parser.Binary) {
            Parser.Binary b = (Parser.Binary) cond;
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
        out.add(new IR.Quad(op, a1, a2, res));
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
