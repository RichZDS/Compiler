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
    private final Semantic.Result sem; // 语义分析结果
    private final List<IR.Quad> out = new ArrayList<>(); // 存储生成的四元式
    private final List<String> errors = new ArrayList<>(); // 存储错误信息

    private int tempId = 0; // 临时变量ID计数器
    private int labelId = 0; // 标签ID计数器

    Generator(Semantic.Result sem) {
        this.sem = sem;
    }

    public List<String> getErrors() {
        return errors;
    }

    /**
     * 运行IR生成器，将AST转换为四元式列表
     * @param program 程序AST根节点
     * @return 生成的四元式列表
     */
    List<IR.Quad> run(Parser.Program program) {
        if (program != null) {
            for (Parser.Stmt s : program.statements) {
                genStmt(s);
            }
        }
        return out;
    }

    // -------- stmt --------
    /**
     * 生成语句的IR代码
     * @param stmt 待处理的语句
     */
    private void genStmt(Parser.Stmt stmt) {
        if (stmt == null) return;

        // 处理块语句
        if (stmt instanceof Parser.Block) {
            Parser.Block b = (Parser.Block) stmt;
            for (Parser.Stmt s : b.statements) genStmt(s);
            return;
        }

        // 处理变量声明
        if (stmt instanceof Parser.VarDecl) {
            Parser.VarDecl d = (Parser.VarDecl) stmt;
            if (d.init != null) {
                String rhs = genExpr(d.init); // 生成初始化表达式
                emit(":=", rhs, "_", d.name); // 生成赋值四元式
            }
            return;
        }

        // 处理赋值语句
        if (stmt instanceof Parser.Assign) {
            Parser.Assign a = (Parser.Assign) stmt;
            String rhs = genExpr(a.value); // 生成右值表达式
            emit(":=", rhs, "_", a.name); // 生成赋值四元式
            return;
        }

        // 处理表达式语句
        if (stmt instanceof Parser.ExprStmt) {
            Parser.ExprStmt e = (Parser.ExprStmt) stmt;
            genExpr(e.expr); // 计算但丢弃结果
            return;
        }

        // 处理if语句
        if (stmt instanceof Parser.IfStmt) {
            Parser.IfStmt i = (Parser.IfStmt) stmt;
            String L_then = newLabel(); // then分支标签
            String L_else = newLabel(); // else分支标签
            String L_end  = newLabel(); // 结束标签

            emitCondJump(i.condition, L_then, L_else); // 根据条件跳转

            emit("label", L_then, "_", "_"); // then标签
            genStmt(i.thenBranch); // 生成then分支代码
            emit("j", "_", "_", L_end); // 跳转到结束

            emit("label", L_else, "_", "_"); // else标签
            if (i.elseBranch != null) genStmt(i.elseBranch); // 生成else分支代码

            emit("label", L_end, "_", "_"); // 结束标签
            return;
        }

        // 处理while循环
        if (stmt instanceof Parser.WhileStmt) {
            Parser.WhileStmt w = (Parser.WhileStmt) stmt;
            String L_begin = newLabel(); // 循环开始标签
            String L_body  = newLabel(); // 循环体标签
            String L_end   = newLabel(); // 循环结束标签

            emit("label", L_begin, "_", "_"); // 开始标签
            emitCondJump(w.condition, L_body, L_end); // 根据条件判断是否进入循环体或结束

            emit("label", L_body, "_", "_"); // 循环体标签
            genStmt(w.body); // 生成循环体代码
            emit("j", "_", "_", L_begin); // 跳转到循环开始

            emit("label", L_end, "_", "_"); // 结束标签
            return;
        }

        // 处理for循环
        if (stmt instanceof Parser.ForStmt) {
            Parser.ForStmt f = (Parser.ForStmt) stmt;

            // 处理初始化语句
            if (f.init != null) genStmt(f.init);

            String L_begin = newLabel(); // 循环开始标签
            String L_body  = newLabel(); // 循环体标签
            String L_end   = newLabel(); // 循环结束标签

            emit("label", L_begin, "_", "_"); // 开始标签

            if (f.cond != null) {
                emitCondJump(f.cond, L_body, L_end); // 根据条件判断是否进入循环体或结束
            } else {
                // cond 为空：视为 true
                emit("j", "_", "_", L_body);
            }

            emit("label", L_body, "_", "_"); // 循环体标签
            genStmt(f.body); // 生成循环体代码

            // 处理步进语句
            if (f.step != null) genStmt(f.step);

            emit("j", "_", "_", L_begin); // 跳转到循环开始
            emit("label", L_end, "_", "_"); // 结束标签
            return;
        }

        errors.add("IR错误: 未知语句类型 " + stmt.getClass().getSimpleName());
    }

    // -------- expr --------
    /**
     * 生成表达式的IR代码
     * @param expr 待处理的表达式
     * @return 表达式结果的存储位置
     */
    private String genExpr(Parser.Expr expr) {
        if (expr == null) return "0";

        // 处理字面量
        if (expr instanceof Parser.Literal) {
            Object v = ((Parser.Literal) expr).value;
            if (v == null) return "0";
            if (v instanceof String) return "\"" + v + "\"";
            return String.valueOf(v);
        }

        // 处理变量
        if (expr instanceof Parser.Var) {
            return ((Parser.Var) expr).name;
        }

        // 处理一元运算
        if (expr instanceof Parser.Unary) {
            Parser.Unary u = (Parser.Unary) expr;
            String x = genExpr(u.expr);

            if (u.op.equals("+")) {
                return x;
            }
            if (u.op.equals("-")) {
                String t = newTemp(); // 创建临时变量
                emit("neg", x, "_", t); // 生成取负四元式
                return t;
            }

            errors.add("IR错误: 未知一元运算符 " + u.op);
            return x;
        }

        // 处理二元运算
        if (expr instanceof Parser.Binary) {
            Parser.Binary b = (Parser.Binary) expr;
            String a = genExpr(b.left); // 左操作数
            String c = genExpr(b.right); // 右操作数

            // 算术 / 比较 都先按"产生一个临时量"处理（比较通常用于条件跳转时会走 emitCondJump）
            String t = newTemp(); // 创建临时变量
            emit(b.op, a, c, t); // 生成运算四元式
            return t;
        }

        errors.add("IR错误: 未知表达式类型 " + expr.getClass().getSimpleName());
        return "0";
    }

    // -------- cond jump（控制流关键）--------
    /**
     * 生成条件跳转代码
     * @param cond 条件表达式
     * @param trueLabel 条件为真时跳转的目标标签
     * @param falseLabel 条件为假时跳转的目标标签
     */
    private void emitCondJump(Parser.Expr cond, String trueLabel, String falseLabel) {
        // 期望：cond 是比较 Binary（< <= > >= == !=）
        if (cond instanceof Parser.Binary) {
            Parser.Binary b = (Parser.Binary) cond;
            if (isRelOp(b.op)) {
                String left = genExpr(b.left);
                String right = genExpr(b.right);
                emit("j" + b.op, left, right, trueLabel); // 生成条件跳转
                emit("j", "_", "_", falseLabel); // 生成无条件跳转到false分支
                return;
            }
        }

        // fallback：cond 不是比较表达式，就用 "cond != 0" 作为真
        String place = genExpr(cond);
        emit("j!=", place, "0", trueLabel);
        emit("j", "_", "_", falseLabel);
    }

    /**
     * 判断是否为关系运算符
     * @param op 运算符
     * @return 是否为关系运算符
     */
    private boolean isRelOp(String op) {
        return op.equals(">") || op.equals(">=") || op.equals("<") || op.equals("<=")
                || op.equals("==") || op.equals("!=");
    }

    // -------- utils --------
    /**
     * 生成四元式并添加到输出列表
     * @param op 操作符
     * @param a1 操作数1
     * @param a2 操作数2
     * @param res 结果
     */
    private void emit(String op, String a1, String a2, String res) {
        out.add(new IR.Quad(op, a1, a2, res));
    }

    /**
     * 创建新的临时变量
     * @return 临时变量名
     */
    private String newTemp() {
        tempId++;
        return "t" + tempId;
    }

    /**
     * 创建新的标签
     * @return 标签名
     */
    private String newLabel() {
        labelId++;
        return "L" + labelId;
    }
}
