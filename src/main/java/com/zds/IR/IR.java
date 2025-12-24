package com.zds.IR;

import com.zds.Semantic.Semantic;
import com.zds.parser.Parser;

import java.util.List;

/**
 * 中间代码生成器 (IR Generator)
 * 门面类 (Facade)：将 AST 转换为三地址码（四元式）。
 *
 * 输入：Parser.Program, Semantic.Result
 * 输出：List<Quad>
 */
public class IR {

    /**
     * 生成中间代码
     * @param program 程序 AST
     * @param sem 语义分析结果
     * @param outErrors 错误收集列表
     * @return 四元式列表
     */
    public static List<Quad> generate(Parser.Program program, Semantic.Result sem, List<String> outErrors) {
        Generator g = new Generator(sem);
        List<Quad> quads = g.run(program);
        if (outErrors != null) outErrors.addAll(g.getErrors());
        return quads;
    }

    /**
     * 打印四元式列表到控制台
     */
    public static void print(List<Quad> quads) {
        for (int i = 0; i < quads.size(); i++) {
            System.out.printf("%d: %s%n", i, quads.get(i));
        }
    }

    // ==========================================
    // Data Structures
    // ==========================================

    /**
     * 四元式 (Quadruple)
     * 结构: (op, arg1, arg2, result)
     */
    public static class Quad {
        public final String op;      // 操作符 (如 +, -, j<, :=)
        public final String arg1;    // 第一个操作数
        public final String arg2;    // 第二个操作数 (可为 "_")
        public final String result;  // 结果变量或跳转目标

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
}
