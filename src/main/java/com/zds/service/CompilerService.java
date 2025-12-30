package com.zds.service;

import com.zds.IR.IR;
import com.zds.Semantic.Semantic;
import com.zds.codegen.CodeGen;
import com.zds.lexer.Lexer;
import com.zds.optimizer.Optimizer;
import com.zds.parser.Parser;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 编译服务类 (Compiler Service)
 * 门面类 (Facade)：对外提供一站式编译服务，协调各阶段子模块。
 *
 * 输入：源代码 (String)
 * 输出：编译产物 (Artifacts)
 */
public class CompilerService {

    /**
     * 执行完整的编译流程
     * @param source 源代码
     * @param enableOpt 是否启用优化
     * @return 编译全过程的产物（包括中间结果和最终代码）
     */
    public static Artifacts compile(String source, boolean enableOpt) {
        String safeSource = source == null ? "" : source;
        List<String> errors = new ArrayList<>();

        // 1. 词法分析
        List<Lexer.Token> tokens = Lexer.scan(safeSource);
        List<String> lexErrors = Lexer.scanErrors(safeSource);
        String lexerText = formatTokens(tokens);

        if (!lexErrors.isEmpty()) {
            errors.addAll(lexErrors);
            return createErrorArtifacts(lexerText, errors);
        }

        // 2. 语法分析
        List<String> parseErrors = new ArrayList<>();
        Parser.Program program = Parser.analyze(tokens, parseErrors);
        if (!parseErrors.isEmpty()) {
            errors.addAll(parseErrors);
            return createErrorArtifacts(lexerText, errors);
        }
        String astText = Parser.Printer.print(program);

        // 3. 语义分析
        List<String> semErrors = new ArrayList<>();
        Semantic.Result sem = Semantic.analyze(program, semErrors);
        if (!semErrors.isEmpty()) {
            errors.addAll(semErrors);
            return createErrorArtifacts(lexerText, astText, errors);
        }

        // 4. 中间代码生成
        List<String> irErrors = new ArrayList<>();
        List<IR.Quad> irBefore = IR.generate(program, sem, irErrors);
        if (!irErrors.isEmpty()) {
            errors.addAll(irErrors);
            return createErrorArtifacts(lexerText, astText, errors);
        }

        // 5. 代码优化
        List<IR.Quad> irAfter = enableOpt ? Optimizer.optimize(irBefore) : copyQuads(irBefore);

        // 6. 目标代码生成
        List<CodeGen.Instr> asm = CodeGen.generate(irAfter, sem);

        // 格式化输出
        String irBeforeText = formatQuads(irBefore);
        String irAfterText = formatQuads(irAfter);
        String asmText = CodeGen.print(asm);

        // 写文件
        try {
            CodeGen.writeToFile(Path.of("src", "main", "resources", "out.asm"), asm);CodeGen.writeToFile(Path.of("target", "out.asm"), asm);
        } catch (Exception ex) {
            errors.add("写文件失败: " + ex.getMessage());
        }

        return new Artifacts(
                lexerText, astText, irBeforeText, irAfterText, asmText,
                joinErrors(errors), tokens, irBefore, irAfter, asm
        );
    }

    // ==========================================
    // Data Structures
    // ==========================================

    /**
     * 编译产物 (Compilation Artifacts)
     * 存储编译器各阶段的输出结果，用于 GUI 展示或调试。
     */
    public static class Artifacts {
        private final String lexerText;
        private final String astText;
        private final String irBeforeText;
        private final String irAfterText;
        private final String asmText;
        private final String errorText;

        private final List<Lexer.Token> tokens;
        private final List<IR.Quad> irBefore;
        private final List<IR.Quad> irAfter;
        private final List<CodeGen.Instr> asm;

        public Artifacts(
                String lexerText, String astText, String irBeforeText, String irAfterText, String asmText, String errorText,
                List<Lexer.Token> tokens, List<IR.Quad> irBefore, List<IR.Quad> irAfter, List<CodeGen.Instr> asm
        ) {
            this.lexerText = lexerText == null ? "" : lexerText;
            this.astText = astText == null ? "" : astText;
            this.irBeforeText = irBeforeText == null ? "" : irBeforeText;
            this.irAfterText = irAfterText == null ? "" : irAfterText;
            this.asmText = asmText == null ? "" : asmText;
            this.errorText = errorText == null ? "" : errorText;
            this.tokens = tokens == null ? List.of() : List.copyOf(tokens);
            this.irBefore = irBefore == null ? List.of() : List.copyOf(irBefore);
            this.irAfter = irAfter == null ? List.of() : List.copyOf(irAfter);
            this.asm = asm == null ? List.of() : List.copyOf(asm);
        }

        public static Artifacts empty() {
            return new Artifacts("", "", "", "", "", "", List.of(), List.of(), List.of(), List.of());
        }

        public String lexerText() { return lexerText; }
        public String astText() { return astText; }
        public String irBeforeText() { return irBeforeText; }
        public String irAfterText() { return irAfterText; }
        public String asmText() { return asmText; }
        public String errorText() { return errorText; }

        public List<Lexer.Token> tokens() { return Collections.unmodifiableList(tokens); }
        public List<IR.Quad> irBefore() { return Collections.unmodifiableList(irBefore); }
        public List<IR.Quad> irAfter() { return Collections.unmodifiableList(irAfter); }
        public List<CodeGen.Instr> asm() { return Collections.unmodifiableList(asm); }

        public boolean hasErrors() {
            return errorText != null && !errorText.isBlank() && !"无错误".equals(errorText);
        }
    }

    // ==========================================
    // Helpers
    // ==========================================

    private static Artifacts createErrorArtifacts(String lexerText, List<String> errors) {
        return createErrorArtifacts(lexerText, "", errors);
    }

    private static Artifacts createErrorArtifacts(String lexerText, String astText, List<String> errors) {
        return new Artifacts(
                lexerText, astText, "", "", "",
                joinErrors(errors), List.of(), List.of(), List.of(), List.of()
        );
    }

    private static String formatTokens(List<Lexer.Token> tokens) {
        StringBuilder sb = new StringBuilder();
        if (tokens != null) {
            for (Lexer.Token token : tokens) sb.append(token).append("\n");
        }
        return sb.toString();
    }

    private static String formatQuads(List<IR.Quad> quads) {
        StringBuilder sb = new StringBuilder();
        if (quads != null) {
            for (int i = 0; i < quads.size(); i++) sb.append(i).append(": ").append(quads.get(i)).append("\n");
        }
        return sb.toString();
    }

    private static String joinErrors(List<String> errors) {
        if (errors == null || errors.isEmpty()) return "无错误";
        return String.join("\n", errors);
    }

    private static List<IR.Quad> copyQuads(List<IR.Quad> input) {
        List<IR.Quad> out = new ArrayList<>();
        if (input != null) {
            for (IR.Quad q : input) {
                if (q != null) out.add(new IR.Quad(q.op, q.arg1, q.arg2, q.result));
            }
        }
        return out;
    }
}
