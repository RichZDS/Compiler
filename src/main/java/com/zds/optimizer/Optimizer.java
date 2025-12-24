package com.zds.optimizer;

import com.zds.IR.IR;

import java.util.ArrayList;
import java.util.List;

/**
 * 优化器 (Optimizer)
 * 门面类 (Facade)：对外提供统一的代码优化接口。
 *
 * 输入：四元式列表 (List<IR.Quad>)
 * 输出：优化后的四元式列表 (List<IR.Quad>)
 */
public class Optimizer {
    private static final int MAX_PASSES = 5;

    /**
     * 优化四元式列表（使用默认最大遍数）
     * @param input 原始四元式列表
     * @return 优化后的四元式列表
     */
    public static List<IR.Quad> optimize(List<IR.Quad> input) {
        return optimize(input, MAX_PASSES);
    }

    /**
     * 优化四元式列表
     * @param input 原始四元式列表
     * @param maxPasses 最大优化迭代遍数
     * @return 优化后的四元式列表
     */
    public static List<IR.Quad> optimize(List<IR.Quad> input, int maxPasses) {
        List<IR.Quad> current = copy(input);
        for (int i = 0; i < maxPasses; i++) {
            List<IR.Quad> optimized = OptimizationPass.run(current);
            if (equalsQuads(current, optimized)) {
                return optimized;
            }
            current = optimized;
        }
        return current;
    }

    // ==========================================
    // Utils
    // ==========================================

    private static List<IR.Quad> copy(List<IR.Quad> input) {
        List<IR.Quad> out = new ArrayList<>();
        if (input != null) {
            for (IR.Quad q : input) {
                out.add(OptimizationPass.copyQuad(q));
            }
        }
        return out;
    }

    private static boolean equalsQuads(List<IR.Quad> a, List<IR.Quad> b) {
        if (a == null || b == null) return a == b;
        if (a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++) {
            IR.Quad qa = a.get(i);
            IR.Quad qb = b.get(i);
            if (!quadEquals(qa, qb)) return false;
        }
        return true;
    }

    private static boolean quadEquals(IR.Quad a, IR.Quad b) {
        if (a == null || b == null) return a == b;
        return equalsStr(a.op, b.op)
                && equalsStr(a.arg1, b.arg1)
                && equalsStr(a.arg2, b.arg2)
                && equalsStr(a.result, b.result);
    }

    private static boolean equalsStr(String a, String b) {
        if (a == null) return b == null;
        return a.equals(b);
    }
}
