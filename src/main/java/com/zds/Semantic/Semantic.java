package com.zds.Semantic;

import com.zds.parser.Parser;

import java.util.*;

/**
 * 语义分析（Semantic Analysis / 静态语义）
 *
 * 输入：Parser.Program
 * 输出：Semantic.Result（符号表 + Expr类型 + errors）
 *
 * 本阶段做最小闭环：
 * 1) 符号表（Symbol Table / Scope）
 * 2) 类型检查（Type Checking）
 * 3) 条件表达式检查（if/while/for-cond 必须可作为 bool）
 */
public class Semantic {

    // ========= 类型系统（Type System）=========
    public enum Type {
        INT, DOUBLE, STRING, BOOL, VOID, ERROR;

        public boolean isNumeric() {
            return this == INT || this == DOUBLE;
        }
    }

    // ========= 符号 / 符号表（Symbol / Symbol Table）=========
    public static class Symbol {
        public final String name;
        public final Type type;
        public final int depth;

        public Symbol(String name, Type type, int depth) {
            this.name = name;
            this.type = type;
            this.depth = depth;
        }
    }

    public static class Scope {
        private final Scope parent;
        private final Map<String, Symbol> table = new LinkedHashMap<>();
        private final int depth;

        public Scope(Scope parent, int depth) {
            this.parent = parent;
            this.depth = depth;
        }

        public Scope parent() { return parent; }
        public int depth() { return depth; }

        public boolean define(String name, Type type) {
            if (table.containsKey(name)) return false;
            table.put(name, new Symbol(name, type, depth));
            return true;
        }

        public Symbol resolve(String name) {
            for (Scope s = this; s != null; s = s.parent) {
                Symbol sym = s.table.get(name);
                if (sym != null) return sym;
            }
            return null;
        }

        public Collection<Symbol> symbolsHere() {
            return table.values();
        }
    }

    // ========= 语义分析输出（Result）=========
    public static class Result {
        public final Scope global;
        public final List<String> errors;
        // 用 IdentityHashMap：按对象引用记录“某个 Expr 的类型”
        private final IdentityHashMap<Parser.Expr, Type> exprTypes;

        public Result(Scope global, List<String> errors, IdentityHashMap<Parser.Expr, Type> exprTypes) {
            this.global = global;
            this.errors = errors;
            this.exprTypes = exprTypes;
        }

        public Type getType(Parser.Expr e) {
            Type t = exprTypes.get(e);
            return (t == null) ? Type.ERROR : t;
        }

        public String dumpSymbolTable() {
            StringBuilder sb = new StringBuilder();
            dumpScope(sb, global, 0);
            return sb.toString();
        }

        private void dumpScope(StringBuilder sb, Scope scope, int indent) {
            if (scope == null) return;
            String pad = "  ".repeat(Math.max(0, indent));
            sb.append(pad).append("Scope(depth=").append(scope.depth()).append(")\n");
            for (Symbol sym : scope.symbolsHere()) {
                sb.append(pad).append("  - ").append(sym.name).append(" : ").append(sym.type).append("\n");
            }
            // 这里不保存子 Scope 引用（为了“最小实现”），所以只打印全局 scope 的内容即可
            // 如果你想把所有 block scope 都打印出来，需要把子 scope 链表保存起来。
        }
    }

    // ========= Facade：对外入口 =========
    public static Result analyze(Parser.Program program, List<String> outErrors) {
        Analyzer a = new Analyzer();
        Result r = a.run(program);
        if (outErrors != null) outErrors.addAll(r.errors);
        return r;
    }

    // ========= Worker：真正干活的 Analyzer =========
    private static class Analyzer {
        private final List<String> errors = new ArrayList<>();
        private final IdentityHashMap<Parser.Expr, Type> exprTypes = new IdentityHashMap<>();

        private Scope current;

        Result run(Parser.Program program) {
            current = new Scope(null, 0); // global
            if (program != null) {
                for (Parser.Stmt s : program.statements) {
                    checkStmt(s);
                }
            }
            return new Result(current, errors, exprTypes);
        }

        // -------- Stmt --------
        private void checkStmt(Parser.Stmt stmt) {
            if (stmt == null) return;

            if (stmt instanceof Parser.Block) {
                beginScope();
                Parser.Block b = (Parser.Block) stmt;
                for (Parser.Stmt s : b.statements) checkStmt(s);
                endScope();
                return;
            }

            if (stmt instanceof Parser.VarDecl) {
                Parser.VarDecl d = (Parser.VarDecl) stmt;
                Type declared = parseType(d.typeName);

                if (!current.define(d.name, declared)) {
                    err("重复声明变量: " + d.name);
                }

                if (d.init != null) {
                    Type rhs = checkExpr(d.init);
                    if (!assignable(declared, rhs)) {
                        err("类型不兼容：不能把 " + rhs + " 赋值给 " + declared + "（变量 " + d.name + "）");
                    }
                }
                return;
            }

            if (stmt instanceof Parser.Assign) {
                Parser.Assign a = (Parser.Assign) stmt;
                Symbol sym = current.resolve(a.name);
                if (sym == null) {
                    err("变量未声明就使用: " + a.name);
                    checkExpr(a.value); // 尽量继续走，收集更多错误
                    return;
                }
                Type rhs = checkExpr(a.value);
                if (!assignable(sym.type, rhs)) {
                    err("类型不兼容：不能把 " + rhs + " 赋值给 " + sym.type + "（变量 " + a.name + "）");
                }
                return;
            }

            if (stmt instanceof Parser.IfStmt) {
                Parser.IfStmt i = (Parser.IfStmt) stmt;
                Type ct = checkCondition(i.condition);
                if (ct != Type.BOOL && ct != Type.ERROR) {
                    err("if 条件必须是 BOOL（比较表达式），当前是: " + ct);
                }
                checkStmt(i.thenBranch);
                if (i.elseBranch != null) checkStmt(i.elseBranch);
                return;
            }

            if (stmt instanceof Parser.WhileStmt) {
                Parser.WhileStmt w = (Parser.WhileStmt) stmt;
                Type ct = checkCondition(w.condition);
                if (ct != Type.BOOL && ct != Type.ERROR) {
                    err("while 条件必须是 BOOL（比较表达式），当前是: " + ct);
                }
                checkStmt(w.body);
                return;
            }

            if (stmt instanceof Parser.ForStmt) {
                Parser.ForStmt f = (Parser.ForStmt) stmt;
                beginScope(); // for 自带一个局部作用域（for-init 声明的变量在循环体内可见）
                if (f.init != null) checkStmt(f.init);
                if (f.cond != null) {
                    Type ct = checkCondition(f.cond);
                    if (ct != Type.BOOL && ct != Type.ERROR) {
                        err("for 条件必须是 BOOL（比较表达式），当前是: " + ct);
                    }
                }
                if (f.step != null) checkStmt(f.step);
                checkStmt(f.body);
                endScope();
                return;
            }

            if (stmt instanceof Parser.ExprStmt) {
                Parser.ExprStmt e = (Parser.ExprStmt) stmt;
                checkExpr(e.expr);
                return;
            }

            // 兜底：未知 Stmt
            err("未知语句类型: " + stmt.getClass().getSimpleName());
        }

        // -------- Expr --------
        private Type checkExpr(Parser.Expr expr) {
            if (expr == null) return Type.ERROR;

            if (expr instanceof Parser.Literal) {
                Object v = ((Parser.Literal) expr).value;
                Type t;
                if (v instanceof Integer) t = Type.INT;
                else if (v instanceof Double || v instanceof Float) t = Type.DOUBLE;
                else if (v instanceof String) t = Type.STRING;
                else t = Type.ERROR;
                exprTypes.put(expr, t);
                return t;
            }

            if (expr instanceof Parser.Var) {
                String name = ((Parser.Var) expr).name;
                Symbol sym = current.resolve(name);
                if (sym == null) {
                    err("变量未声明就使用: " + name);
                    exprTypes.put(expr, Type.ERROR);
                    return Type.ERROR;
                }
                exprTypes.put(expr, sym.type);
                return sym.type;
            }

            if (expr instanceof Parser.Unary) {
                Parser.Unary u = (Parser.Unary) expr;
                Type inner = checkExpr(u.expr);

                if (u.op.equals("+")) {
                    // +x：允许 numeric
                    if (!inner.isNumeric() && inner != Type.ERROR) {
                        err("一元 + 只能用于数字类型，当前: " + inner);
                        exprTypes.put(expr, Type.ERROR);
                        return Type.ERROR;
                    }
                    exprTypes.put(expr, inner);
                    return inner;
                }

                if (u.op.equals("-")) {
                    // -x：允许 numeric
                    if (!inner.isNumeric() && inner != Type.ERROR) {
                        err("一元 - 只能用于数字类型，当前: " + inner);
                        exprTypes.put(expr, Type.ERROR);
                        return Type.ERROR;
                    }
                    exprTypes.put(expr, inner);
                    return inner;
                }

                err("未知一元运算符: " + u.op);
                exprTypes.put(expr, Type.ERROR);
                return Type.ERROR;
            }

            if (expr instanceof Parser.Binary) {
                Parser.Binary b = (Parser.Binary) expr;
                Type lt = checkExpr(b.left);
                Type rt = checkExpr(b.right);

                // 算术：+ - * /
                if (b.op.equals("+")) {
                    // string + anything -> string（你也可以改成更严格：必须 string+string）
                    if (lt == Type.STRING || rt == Type.STRING) {
                        exprTypes.put(expr, Type.STRING);
                        return Type.STRING;
                    }
                    if (lt.isNumeric() && rt.isNumeric()) {
                        Type res = (lt == Type.DOUBLE || rt == Type.DOUBLE) ? Type.DOUBLE : Type.INT;
                        exprTypes.put(expr, res);
                        return res;
                    }
                    err("运算 + 不支持类型: " + lt + " + " + rt);
                    exprTypes.put(expr, Type.ERROR);
                    return Type.ERROR;
                }

                if (b.op.equals("-") || b.op.equals("*") || b.op.equals("/")) {
                    if (lt.isNumeric() && rt.isNumeric()) {
                        Type res = (lt == Type.DOUBLE || rt == Type.DOUBLE) ? Type.DOUBLE : Type.INT;
                        exprTypes.put(expr, res);
                        return res;
                    }
                    err("运算 " + b.op + " 只支持数字类型，当前: " + lt + " " + b.op + " " + rt);
                    exprTypes.put(expr, Type.ERROR);
                    return Type.ERROR;
                }

                // 比较：> >= < <= == != -> BOOL
                if (isRelOp(b.op)) {
                    if (b.op.equals("==") || b.op.equals("!=")) {
                        // 允许：同类型 or 数字混合
                        boolean ok = (lt == rt) || (lt.isNumeric() && rt.isNumeric());
                        if (!ok && lt != Type.ERROR && rt != Type.ERROR) {
                            err("比较 " + b.op + " 两侧类型不兼容: " + lt + " vs " + rt);
                            exprTypes.put(expr, Type.ERROR);
                            return Type.ERROR;
                        }
                        exprTypes.put(expr, Type.BOOL);
                        return Type.BOOL;
                    } else {
                        // > >= < <=：仅数字
                        if (!lt.isNumeric() || !rt.isNumeric()) {
                            if (lt != Type.ERROR && rt != Type.ERROR) {
                                err("比较 " + b.op + " 只支持数字类型，当前: " + lt + " vs " + rt);
                            }
                            exprTypes.put(expr, Type.ERROR);
                            return Type.ERROR;
                        }
                        exprTypes.put(expr, Type.BOOL);
                        return Type.BOOL;
                    }
                }

                err("未知二元运算符: " + b.op);
                exprTypes.put(expr, Type.ERROR);
                return Type.ERROR;
            }

            // 兜底
            err("未知表达式类型: " + expr.getClass().getSimpleName());
            exprTypes.put(expr, Type.ERROR);
            return Type.ERROR;
        }

        private Type checkCondition(Parser.Expr cond) {
            // 你的语法里条件就是 Expr：我们要求它必须能推到 BOOL
            return checkExpr(cond);
        }

        // -------- helpers --------
        private void beginScope() {
            current = new Scope(current, current.depth() + 1);
        }

        private void endScope() {
            if (current.parent() != null) current = current.parent();
        }

        private Type parseType(String typeName) {
            if (typeName == null) return Type.ERROR;
            String t = typeName.trim().toLowerCase(Locale.ROOT);
            if (t.equals("int")) return Type.INT;
            if (t.equals("double")) return Type.DOUBLE;
            if (t.equals("string")) return Type.STRING;
            return Type.ERROR;
        }

        private boolean assignable(Type target, Type source) {
            if (target == Type.ERROR || source == Type.ERROR) return true; // 避免级联报错
            if (target == source) return true;
            // int -> double 允许隐式提升
            if (target == Type.DOUBLE && source == Type.INT) return true;
            return false;
        }

        private boolean isRelOp(String op) {
            return op.equals(">") || op.equals(">=") || op.equals("<") || op.equals("<=")
                    || op.equals("==") || op.equals("!=");
        }

        private void err(String msg) {
            errors.add("语义错误: " + msg);
        }
    }
}
