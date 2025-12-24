package com.zds.backend;


import com.zds.IR.IR;
import com.zds.Semantic.Semantic;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CodeGenPseudoAsm {
    private enum ValueType {
        INT, DOUBLE, STRING, UNKNOWN
    }

    public static List<AsmInstr> generate(List<IR.Quad> quads, Semantic.Result sem) {
        Asm asm = new Asm();
        Map<String, ValueType> tempTypes = new HashMap<>();

        if (quads != null) {
            for (IR.Quad quad : quads) {
                if (quad == null) {
                    continue;
                }
                emitQuad(asm, quad, sem, tempTypes);
            }
        }
        return asm.instructions();
    }

    private static void emitQuad(Asm asm, IR.Quad q, Semantic.Result sem, Map<String, ValueType> tempTypes) {
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

    private static String jumpMnemonic(String op) {
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

    private static ValueType inferBinaryType(
            String op,
            String left,
            String right,
            Semantic.Result sem,
            Map<String, ValueType> tempTypes
    ) {
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

    private static ValueType typeOf(String value, Semantic.Result sem, Map<String, ValueType> tempTypes) {
        if (value == null) {
            return ValueType.UNKNOWN;
        }
        if (value.startsWith("\"") && value.endsWith("\"")) {
            return ValueType.STRING;
        }
        if (isNumericLiteral(value)) {
            return value.contains(".") ? ValueType.DOUBLE : ValueType.INT;
        }
        if (isTemp(value)) {
            return tempTypes.getOrDefault(value, ValueType.UNKNOWN);
        }
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

    private static boolean isTemp(String value) {
        return value != null && value.startsWith("t");
    }

    private static boolean isNumericLiteral(String value) {
        try {
            Double.parseDouble(value);
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }
}
