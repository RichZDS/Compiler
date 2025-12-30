package com.zds.codegen;

import com.zds.IR.IR;
import com.zds.Semantic.Semantic;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 汇编生成器核心实现 (AsmBuilder)
 * 包级私有类，负责将四元式映射为伪汇编指令。
 */
class AsmBuilder {
    private final Asm asm = new Asm(); // 存储生成的汇编指令
    private final Map<String, ValueType> tempTypes = new HashMap<>(); // 临时变量的类型映射

    /**
     * 获取生成的汇编对象
     * @return 汇编对象
     */
    public Asm getAsm() {
        return asm;
    }

    /**
     * 遍历所有四元式并转换为汇编指令
     * @param quads 四元式列表
     * @param sem 语义分析结果
     */
    public void run(List<IR.Quad> quads, Semantic.Result sem) {
        if (quads != null) {
            for (IR.Quad quad : quads) {
                if (quad != null) {
                    emitQuad(quad, sem); // 处理每个四元式
                }
            }
        }
    }

    /**
     * 根据四元式的操作类型生成相应的汇编指令
     * @param q 四元式
     * @param sem 语义分析结果
     */
    private void emitQuad(IR.Quad q, Semantic.Result sem) {
        String op = q.op;
        // 处理赋值操作
        if (":=".equals(op)) {
            asm.add("MOV", q.result, q.arg1);
            if (isTemp(q.result)) {
                tempTypes.put(q.result, typeOf(q.arg1, sem, tempTypes));
            }
            return;
        }
        // 处理负号操作
        if ("neg".equals(op)) {
            asm.add("NEG", q.result, q.arg1);
            if (isTemp(q.result)) {
                tempTypes.put(q.result, typeOf(q.arg1, sem, tempTypes));
            }
            return;
        }
        // 处理标签定义
        if ("label".equals(op)) {
            asm.add("LABEL", q.arg1);
            return;
        }
        // 处理无条件跳转
        if ("j".equals(op)) {
            asm.add("JMP", q.result);
            return;
        }
        // 处理条件跳转操作（如 j<, j<=, j>, j>=, j==, j!=）
        if (op != null && op.startsWith("j") && op.length() > 1) {
            String jmp = jumpMnemonic(op);
            if (jmp != null) {
                asm.add(jmp, q.arg1, q.arg2, q.result);
            } else {
                asm.addRaw(";;UNSUPPORTED " + q); // 不支持的操作，添加注释
            }
            return;
        }
        // 处理算术运算（+、-、*、/）
        if ("+".equals(op) || "-".equals(op) || "*".equals(op) || "/".equals(op)) {
            ValueType type = inferBinaryType(op, q.arg1, q.arg2, sem, tempTypes);
            String instr = switch (op) {
                case "+" -> type == ValueType.STRING ? "CONCAT" : "ADD"; // 字符串相加使用CONCAT指令
                case "-" -> "SUB";
                case "*" -> "MUL";
                case "/" -> "DIV";
                default -> "ADD";
            };
            asm.add(instr, q.result, q.arg1, q.arg2);
            if (isTemp(q.result)) {
                tempTypes.put(q.result, type);
            }
            return;
        }

        // 不支持的操作，添加注释
        asm.addRaw(";;UNSUPPORTED " + q);
    }

    /**
     * 将条件跳转操作转换为汇编跳转助记符
     * @param op 条件跳转操作
     * @return 对应的汇编跳转助记符
     */
    private String jumpMnemonic(String op) {
        return switch (op) {
            case "j<" -> "JLT";  // 小于跳转
            case "j<=" -> "JLE"; // 小于等于跳转
            case "j>" -> "JGT";  // 大于跳转
            case "j>=" -> "JGE"; // 大于等于跳转
            case "j==" -> "JEQ"; // 等于跳转
            case "j!=" -> "JNE"; // 不等于跳转
            default -> null;
        };
    }

    /**
     * 推断二元运算的结果类型
     * @param op 操作符
     * @param left 左操作数
     * @param right 右操作数
     * @param sem 语义分析结果
     * @param tempTypes 临时变量类型映射
     * @return 运算结果类型
     */
    private ValueType inferBinaryType(String op, String left, String right, Semantic.Result sem, Map<String, ValueType> tempTypes) {
        ValueType leftType = typeOf(left, sem, tempTypes);
        ValueType rightType = typeOf(right, sem, tempTypes);
        // 如果是字符串连接操作，结果为字符串类型
        if ("+".equals(op) && (leftType == ValueType.STRING || rightType == ValueType.STRING)) {
            return ValueType.STRING;
        }
        // 如果任一操作数为double类型，结果为double类型
        if (leftType == ValueType.DOUBLE || rightType == ValueType.DOUBLE) {
            return ValueType.DOUBLE;
        }
        // 如果任一操作数为int类型，结果为int类型
        if (leftType == ValueType.INT || rightType == ValueType.INT) {
            return ValueType.INT;
        }
        return ValueType.UNKNOWN;
    }

    /**
     * 获取值的类型
     * @param value 值
     * @param sem 语义分析结果
     * @param tempTypes 临时变量类型映射
     * @return 值的类型
     */
    private ValueType typeOf(String value, Semantic.Result sem, Map<String, ValueType> tempTypes) {
        if (value == null) return ValueType.UNKNOWN;
        if (value.startsWith("\"") && value.endsWith("\"")) return ValueType.STRING; // 字符串字面量
        if (isNumericLiteral(value)) return value.contains(".") ? ValueType.DOUBLE : ValueType.INT; // 数值字面量
        if (isTemp(value)) return tempTypes.getOrDefault(value, ValueType.UNKNOWN); // 临时变量类型
        if (sem != null && sem.global != null) {
            Semantic.Symbol sym = sem.global.resolve(value);
            if (sym != null) {
                // 根据符号表中的类型返回对应值类型
                return switch (sym.type) {
                    case STRING -> ValueType.STRING;
                    case DOUBLE -> ValueType.DOUBLE;
                    case INT, BOOL -> ValueType.INT;
                    default -> ValueType.UNKNOWN;
                };
            }
        }
        return ValueType.UNKNOWN;
    }

    /**
     * 判断是否为临时变量
     * @param value 变量名
     * @return 是否为临时变量
     */
    private boolean isTemp(String value) {
        return value != null && value.startsWith("t");
    }

    /**
     * 判断是否为数值字面量
     * @param value 值
     * @return 是否为数值字面量
     */
    private boolean isNumericLiteral(String value) {
        try {
            Double.parseDouble(value);
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    /**
     * 值类型枚举
     */
    private enum ValueType {
        INT,    // 整型
        DOUBLE, // 双精度浮点型
        STRING, // 字符串型
        UNKNOWN // 未知类型
    }
}
