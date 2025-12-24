package com.zds.optimizer;

import com.zds.IR.IR;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 优化算法实现 (Optimization Pass)
 * 包级私有类，包含具体的优化策略实现。
 */
class OptimizationPass {

    /**
     * 执行单遍优化
     */
    static List<IR.Quad> run(List<IR.Quad> quads) {
        List<IR.Quad> output = new ArrayList<>();
        List<IR.Quad> block = new ArrayList<>();
        for (IR.Quad quad : quads) {
            if (isLabel(quad)) {
                flushBlock(output, block);
                output.add(copyQuad(quad));
                continue;
            }
            block.add(copyQuad(quad));
            if (isJump(quad)) {
                flushBlock(output, block);
            }
        }
        flushBlock(output, block);
        return output;
    }

    private static void flushBlock(List<IR.Quad> output, List<IR.Quad> block) {
        if (block.isEmpty()) {
            return;
        }
        List<IR.Quad> simplified = simplify(block);
        List<IR.Quad> dce = eliminateDeadTemps(simplified);
        output.addAll(dce);
        block.clear();
    }

    private static List<IR.Quad> simplify(List<IR.Quad> quads) {
        List<IR.Quad> out = new ArrayList<>();
        for (IR.Quad q : quads) {
            IR.Quad simplified = simplifyQuad(q);
            out.add(simplified);
        }
        return out;
    }

    private static IR.Quad simplifyQuad(IR.Quad q) {
        if (q == null) return q;
        String op = q.op;
        if (isBinaryOp(op)) {
            String a = q.arg1;
            String b = q.arg2;
            if (isNumericLiteral(a) && isNumericLiteral(b)) {
                String folded = foldNumeric(op, a, b);
                if (folded != null) {
                    return new IR.Quad(":=", folded, "_", q.result);
                }
            }
            IR.Quad algebra = simplifyAlgebra(op, a, b, q.result);
            if (algebra != null) {
                return algebra;
            }
            return q;
        }
        if ("neg".equals(op)) {
            if (isNumericLiteral(q.arg1)) {
                String value = foldNumeric("neg", q.arg1, null);
                if (value != null) {
                    return new IR.Quad(":=", value, "_", q.result);
                }
            }
            return q;
        }
        return q;
    }

    private static IR.Quad simplifyAlgebra(String op, String a, String b, String result) {
        if ("+".equals(op)) {
            if (isZero(b)) return new IR.Quad(":=", a, "_", result);
            if (isZero(a)) return new IR.Quad(":=", b, "_", result);
        }
        if ("-".equals(op)) {
            if (isZero(b)) return new IR.Quad(":=", a, "_", result);
        }
        if ("*".equals(op)) {
            if (isOne(a)) return new IR.Quad(":=", b, "_", result);
            if (isOne(b)) return new IR.Quad(":=", a, "_", result);
            if (isZero(a) || isZero(b)) return new IR.Quad(":=", "0", "_", result);
        }
        if ("/".equals(op)) {
            if (isOne(b)) return new IR.Quad(":=", a, "_", result);
        }
        return null;
    }

    private static List<IR.Quad> eliminateDeadTemps(List<IR.Quad> quads) {
        List<IR.Quad> out = new ArrayList<>();
        Set<String> used = new HashSet<>();

        for (int i = quads.size() - 1; i >= 0; i--) {
            IR.Quad q = quads.get(i);
            if (q == null) continue;
            if (isLabel(q) || isJump(q)) {
                markUsed(used, q.arg1);
                markUsed(used, q.arg2);
                out.add(0, q);
                continue;
            }
            String def = q.result;
            boolean definesTemp = isTemp(def);
            if (definesTemp && !used.contains(def)) {
                continue;
            }
            markUsed(used, q.arg1);
            markUsed(used, q.arg2);
            if (definesTemp) {
                used.remove(def);
            }
            out.add(0, q);
        }
        return out;
    }

    private static void markUsed(Set<String> used, String value) {
        if (isTemp(value)) {
            used.add(value);
        }
    }

    // Helpers

    private static boolean isBinaryOp(String op) {
        return "+".equals(op) || "-".equals(op) || "*".equals(op) || "/".equals(op);
    }

    private static boolean isLabel(IR.Quad q) {
        return q != null && "label".equals(q.op);
    }

    private static boolean isJump(IR.Quad q) {
        if (q == null || q.op == null) return false;
        if ("j".equals(q.op)) return true;
        return q.op.startsWith("j") && q.op.length() > 1;
    }

    private static boolean isTemp(String value) {
        return value != null && value.startsWith("t");
    }

    private static boolean isZero(String value) {
        return "0".equals(value) || "0.0".equals(value);
    }

    private static boolean isOne(String value) {
        return "1".equals(value) || "1.0".equals(value);
    }

    private static boolean isNumericLiteral(String value) {
        if (value == null || value.isBlank()) return false;
        if (value.startsWith("\"") && value.endsWith("\"")) return false;
        try {
            Double.parseDouble(value);
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private static String foldNumeric(String op, String a, String b) {
        try {
            double left = Double.parseDouble(a);
            double right = b == null ? 0.0 : Double.parseDouble(b);
            double result;
            switch (op) {
                case "+" -> result = left + right;
                case "-" -> result = left - right;
                case "*" -> result = left * right;
                case "/" -> {
                    if (right == 0.0) return null;
                    result = left / right;
                }
                case "neg" -> result = -left;
                default -> { return null; }
            }
            return formatNumber(result);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static String formatNumber(double value) {
        if (Math.rint(value) == value) {
            return Long.toString((long) value);
        }
        return Double.toString(value);
    }

    static IR.Quad copyQuad(IR.Quad q) {
        if (q == null) return null;
        return new IR.Quad(q.op, q.arg1, q.arg2, q.result);
    }
}
