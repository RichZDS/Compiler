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
    private final Asm asm = new Asm();
    private final Map<String, ValueType> tempTypes = new HashMap<>();

    public Asm getAsm() {
        return asm;
    }

    public void run(List<IR.Quad> quads, Semantic.Result sem) {
        if (quads != null) {
            for (IR.Quad quad : quads) {
                if (quad != null) {
                    emitQuad(quad, sem);
                }
            }
        }
    }

    private void emitQuad(IR.Quad q, Semantic.Result sem) {
        String op = q.op;
        if (":=".equals(op)) {
            asm.add("MOV", q.result, q.arg1);
            if (isTemp(q.result)) {
                tempTypes.put(q.result, typeOf(q.arg1, sem, tempTypes));
            }
            return;
        }
        if ("neg".equals(op)) {
            asm.add("NEG", q.result, q.arg1);
            if (isTemp(q.result)) {
                tempTypes.put(q.result, typeOf(q.arg1, sem, tempTypes));
            }
            return;
        }
        if ("label".equals(op)) {
            asm.add("LABEL", q.arg1);
            return;
        }
        if ("j".equals(op)) {
            asm.add("JMP", q.result);
            return;
        }
        if (op != null && op.startsWith("j") && op.length() > 1) {
            String jmp = jumpMnemonic(op);
            if (jmp != null) {
                asm.add(jmp, q.arg1, q.arg2, q.result);
            } else {
                asm.addRaw(";;UNSUPPORTED " + q);
            }
            return;
        }
        if ("+".equals(op) || "-".equals(op) || "*".equals(op) || "/".equals(op)) {
            ValueType type = inferBinaryType(op, q.arg1, q.arg2, sem, tempTypes);
            String instr = switch (op) {
                case "+" -> type == ValueType.STRING ? "CONCAT" : "ADD";
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

        asm.addRaw(";;UNSUPPORTED " + q);
    }

    private String jumpMnemonic(String op) {
        return switch (op) {
            case "j<" -> "JLT";
            case "j<=" -> "JLE";
            case "j>" -> "JGT";
            case "j>=" -> "JGE";
            case "j==" -> "JEQ";
            case "j!=" -> "JNE";
            default -> null;
        };
    }

    private ValueType inferBinaryType(String op, String left, String right, Semantic.Result sem, Map<String, ValueType> tempTypes) {
        ValueType leftType = typeOf(left, sem, tempTypes);
        ValueType rightType = typeOf(right, sem, tempTypes);
        if ("+".equals(op) && (leftType == ValueType.STRING || rightType == ValueType.STRING)) {
            return ValueType.STRING;
        }
        if (leftType == ValueType.DOUBLE || rightType == ValueType.DOUBLE) {
            return ValueType.DOUBLE;
        }
        if (leftType == ValueType.INT || rightType == ValueType.INT) {
            return ValueType.INT;
        }
        return ValueType.UNKNOWN;
    }

    private ValueType typeOf(String value, Semantic.Result sem, Map<String, ValueType> tempTypes) {
        if (value == null) return ValueType.UNKNOWN;
        if (value.startsWith("\"") && value.endsWith("\"")) return ValueType.STRING;
        if (isNumericLiteral(value)) return value.contains(".") ? ValueType.DOUBLE : ValueType.INT;
        if (isTemp(value)) return tempTypes.getOrDefault(value, ValueType.UNKNOWN);
        if (sem != null && sem.global != null) {
            Semantic.Symbol sym = sem.global.resolve(value);
            if (sym != null) {
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

    private boolean isTemp(String value) {
        return value != null && value.startsWith("t");
    }

    private boolean isNumericLiteral(String value) {
        try {
            Double.parseDouble(value);
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private enum ValueType {
        INT, DOUBLE, STRING, UNKNOWN
    }
}
