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
     * 通过多次迭代应用优化，直到达到最大迭代次数或优化不再产生变化
     * @param input 原始四元式列表
     * @param maxPasses 最大优化迭代遍数
     * @return 优化后的四元式列表
     */
    public static List<IR.Quad> optimize(List<IR.Quad> input, int maxPasses) {
        List<IR.Quad> current = copy(input); // 创建输入的副本，避免修改原始数据
        for (int i = 0; i < maxPasses; i++) {
            List<IR.Quad> optimized = OptimizationPass.run(current); // 执行一次优化遍
            if (equalsQuads(current, optimized)) { // 如果优化后没有变化，则提前结束
                return optimized;
            }
            current = optimized; // 更新当前四元式列表为优化后的版本
        }
        return current; // 返回最后一次优化的结果
    }

    // ==========================================
    // Utils
    // ==========================================

    /**
     * 深拷贝四元式列表
     * @param input 待拷贝的四元式列表
     * @return 拷贝后的四元式列表
     */
    private static List<IR.Quad> copy(List<IR.Quad> input) {
        List<IR.Quad> out = new ArrayList<>();
        if (input != null) {
            for (IR.Quad q : input) {
                out.add(OptimizationPass.copyQuad(q)); // 使用优化过程中的拷贝方法
            }
        }
        return out;
    }

    /**
     * 比较两个四元式列表是否相等
     * @param a 第一个四元式列表
     * @param b 第二个四元式列表
     * @return 如果两个列表相等则返回true，否则返回false
     */
    private static boolean equalsQuads(List<IR.Quad> a, List<IR.Quad> b) {
        if (a == null || b == null) return a == b; // 如果任一列表为null，只有都为null时才相等
        if (a.size() != b.size()) return false; // 大小不等则不相等
        for (int i = 0; i < a.size(); i++) {
            IR.Quad qa = a.get(i);
            IR.Quad qb = b.get(i);
            if (!quadEquals(qa, qb)) return false; // 逐个比较四元式
        }
        return true;
    }

    /**
     * 比较两个四元式是否相等
     * @param a 第一个四元式
     * @param b 第二个四元式
     * @return 如果两个四元式相等则返回true，否则返回false
     */
    private static boolean quadEquals(IR.Quad a, IR.Quad b) {
        if (a == null || b == null) return a == b; // 如果任一四元式为null，只有都为null时才相等
        // 比较四元式的四个组成部分
        return equalsStr(a.op, b.op)
                && equalsStr(a.arg1, b.arg1)
                && equalsStr(a.arg2, b.arg2)
                && equalsStr(a.result, b.result);
    }

    /**
     * 比较两个字符串是否相等
     * @param a 第一个字符串
     * @param b 第二个字符串
     * @return 如果两个字符串相等则返回true，否则返回false
     */
    private static boolean equalsStr(String a, String b) {
        if (a == null) return b == null; // 如果a为null，只有b也为null时才相等
        return a.equals(b); // 使用String的equals方法比较
    }
}
