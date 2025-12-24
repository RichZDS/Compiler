package com.zds.codegen;

import com.zds.IR.IR;
import com.zds.Semantic.Semantic;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 代码生成器 (Code Generator)
 * 门面类 (Facade)：将中间代码转换为伪汇编指令。
 *
 * 输入：List<IR.Quad>, Semantic.Result
 * 输出：List<Instr>
 */
public class CodeGen {

    /**
     * 生成汇编指令
     * @param quads 四元式列表
     * @param sem 语义信息（用于辅助判断变量类型）
     * @return 汇编指令列表
     */
    public static List<Instr> generate(List<IR.Quad> quads, Semantic.Result sem) {
        AsmBuilder builder = new AsmBuilder();
        builder.run(quads, sem);
        return builder.getAsm().instructions();
    }

    /**
     * 将指令列表转换为格式化的文本
     */
    public static String print(List<Instr> instructions) {
        StringBuilder sb = new StringBuilder();
        if (instructions != null) {
            for (Instr instr : instructions) {
                if (instr != null) {
                    sb.append(instr).append("\n");
                }
            }
        }
        return sb.toString();
    }

    /**
     * 将指令写入文件
     */
    public static void writeToFile(Path path, List<Instr> instructions) throws IOException {
        if (path == null) return;
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        String content = print(instructions);
        Files.writeString(path, content, StandardCharsets.UTF_8);
    }

    // ==========================================
    // Data Structures
    // ==========================================

    /**
     * 汇编指令 (Instruction)
     */
    public static class Instr {
        private final String op;
        private final List<String> args;
        private final String raw;

        private Instr(String op, List<String> args, String raw) {
            this.op = op;
            this.args = args == null ? List.of() : List.copyOf(args);
            this.raw = raw;
        }

        public static Instr of(String op, String... args) {
            List<String> list = new ArrayList<>();
            if (args != null) {
                for (String arg : args) {
                    if (arg != null && !arg.isBlank()) {
                        list.add(arg);
                    }
                }
            }
            return new Instr(op, list, null);
        }

        public static Instr raw(String line) {
            return new Instr(null, List.of(), line);
        }

        @Override
        public String toString() {
            if (raw != null) return raw;
            if (args.isEmpty()) return op;
            return op + " " + String.join(", ", args);
        }
    }
}
