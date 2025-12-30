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
     * 遍历四元式列表，将基本块（basic block）分组并进行优化
     * 基本块以标签开始，以跳转指令结束
     */
    static List<IR.Quad> run(List<IR.Quad> quads) {
        List<IR.Quad> output = new ArrayList<>(); // 存储优化后的四元式
        List<IR.Quad> block = new ArrayList<>(); // 存储当前基本块的四元式
        
        for (IR.Quad quad : quads) {
            if (isLabel(quad)) {
                // 遇到标签时，先处理当前块，然后将标签添加到输出
                flushBlock(output, block);
                output.add(copyQuad(quad));
                continue;
            }
            // 将当前四元式添加到当前基本块
            block.add(copyQuad(quad));
            if (isJump(quad)) {
                // 遇到跳转指令时，处理当前块
                flushBlock(output, block);
            }
        }
        // 处理最后一个基本块
        flushBlock(output, block);
        return output;
    }

    /**
     * 处理当前基本块，执行简化和死代码消除优化
     */
    private static void flushBlock(List<IR.Quad> output, List<IR.Quad> block) {
        if (block.isEmpty()) {
            return;
        }
        // 先执行简化优化
        List<IR.Quad> simplified = simplify(block);
        // 再执行死代码消除
        List<IR.Quad> dce = eliminateDeadTemps(simplified);
        output.addAll(dce);
        block.clear();
    }

    /**
     * 对基本块中的每个四元式进行简化
     */
    private static List<IR.Quad> simplify(List<IR.Quad> quads) {
        List<IR.Quad> out = new ArrayList<>();
        for (IR.Quad q : quads) {
            IR.Quad simplified = simplifyQuad(q);
            out.add(simplified);
        }
        return out;
    }

    /**
     * 简化单个四元式，包括常量折叠和代数简化
     */
    private static IR.Quad simplifyQuad(IR.Quad q) {
        if (q == null) return q;
        String op = q.op;
        if (isBinaryOp(op)) {
            String a = q.arg1;
            String b = q.arg2;
            // 常量折叠：如果两个操作数都是数字字面量，直接计算结果
            if (isNumericLiteral(a) && isNumericLiteral(b)) {
                String folded = foldNumeric(op, a, b);
                if (folded != null) {
                    return new IR.Quad(":=", folded, "_", q.result);
                }
            }
            // 代数简化：如 x + 0 = x, x * 1 = x 等
            IR.Quad algebra = simplifyAlgebra(op, a, b, q.result);
            if (algebra != null) {
                return algebra;
            }
            return q;
        }
        // 处理负数操作
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

    /**
     * 执行代数简化，如加零、乘一等
     */
    private static IR.Quad simplifyAlgebra(String op, String a, String b, String result) {
        if ("+".equals(op)) {
            if (isZero(b)) return new IR.Quad(":=", a, "_", result); // x + 0 = x
            if (isZero(a)) return new IR.Quad(":=", b, "_", result); // 0 + x = x
        }
        if ("-".equals(op)) {
            if (isZero(b)) return new IR.Quad(":=", a, "_", result); // x - 0 = x
        }
        if ("*".equals(op)) {
            if (isOne(a)) return new IR.Quad(":=", b, "_", result); // x * 1 = x
            if (isOne(b)) return new IR.Quad(":=", a, "_", result); // x * 1 = x
            if (isZero(a) || isZero(b)) return new IR.Quad(":=", "0", "_", result); // x * 0 = 0
        }
        if ("/".equals(op)) {
            if (isOne(b)) return new IR.Quad(":=", a, "_", result); // x / 1 = x
        }
        return null;
    }

    /**
     * 消除死临时变量 - 使用反向遍历找出未被使用的临时变量并删除其定义
     */
    private static List<IR.Quad> eliminateDeadTemps(List<IR.Quad> quads) {
        List<IR.Quad> out = new ArrayList<>();
        Set<String> used = new HashSet<>(); // 存储被使用的临时变量

        // 从后往前遍历，找出被使用的临时变量
        for (int i = quads.size() - 1; i >= 0; i--) {
            IR.Quad q = quads.get(i);
            if (q == null) continue;
            if (isLabel(q) || isJump(q)) {
                // 标签和跳转指令的参数总是被使用的
                markUsed(used, q.arg1);
                markUsed(used, q.arg2);
                out.add(0, q);
                continue;
            }
            String def = q.result;
            boolean definesTemp = isTemp(def);
            // 如果定义的是临时变量且未被使用，则跳过（即删除该四元式）
            if (definesTemp && !used.contains(def)) {
                continue;
            }
            // 标记操作数为已使用
            markUsed(used, q.arg1);
            markUsed(used, q.arg2);
            if (definesTemp) {
                // 如果是临时变量定义，使用后从集合中移除
                used.remove(def);
            }
            out.add(0, q);
        }
        return out;
    }

    /**
     * 如果值是临时变量，则标记为已使用
     */
    private static void markUsed(Set<String> used, String value) {
        if (isTemp(value)) {
            used.add(value);
        }
    }

    // 辅助方法

    /**
     * 判断操作符是否为二元运算符
     */
    private static boolean isBinaryOp(String op) {
        return "+".equals(op) || "-".equals(op) || "*".equals(op) || "/".equals(op);
    }

    /**
     * 判断四元式是否为标签指令
     */
    private static boolean isLabel(IR.Quad q) {
        return q != null && "label".equals(q.op);
    }

    /**
     * 判断四元式是否为跳转指令
     */
    private static boolean isJump(IR.Quad q) {
        if (q == null || q.op == null) return false;
        if ("j".equals(q.op)) return true;
        return q.op.startsWith("j") && q.op.length() > 1;
    }

    /**
     * 判断值是否为临时变量（以t开头）
     */
    private static boolean isTemp(String value) {
        return value != null && value.startsWith("t");
    }

    /**
     * 判断值是否为零
     */
    private static boolean isZero(String value) {
        return "0".equals(value) || "0.0".equals(value);
    }

    /**
     * 判断值是否为一
     */
    private static boolean isOne(String value) {
        return "1".equals(value) || "1.0".equals(value);
    }

    /**
     * 判断值是否为数字字面量
     */
    private static boolean isNumericLiteral(String value) {
        if (value == null || value.isBlank()) return false;
        if (value.startsWith("\"") && value.endsWith("\"")) return false; // 排除字符串
        try {
            Double.parseDouble(value);
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    /**
     * 执行数值计算，实现常量折叠
     */
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
                    if (right == 0.0) return null; // 避免除零错误
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

    /**
     * 格式化数字，整数以整数形式输出，小数以小数形式输出
     */
    private static String formatNumber(double value) {
        if (Math.rint(value) == value) {
            return Long.toString((long) value);
        }
        return Double.toString(value);
    }

    /**
     * 复制四元式
     */
    static IR.Quad copyQuad(IR.Quad q) {
        if (q == null) return null;
        return new IR.Quad(q.op, q.arg1, q.arg2, q.result);
    }
}
