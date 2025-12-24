package com.zds.service;

import com.zds.codegen.AsmInstr;
import com.zds.codegen.AsmPrinter;
import com.zds.codegen.CodeGenPseudoAsm;
import com.zds.optimizer.Optimizer;

import com.zds.IR.IR;
import com.zds.Semantic.Semantic;
import com.zds.lexer.Lexer;
import com.zds.lexer.Token;
import com.zds.parser.AST;
import com.zds.parser.Parser;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 编译服务类 - 编译器的主要入口，执行完整的编译流程
 */
public class CompilerService {
    /**
     * 编译源代码，返回编译产物
     */
    public static CompilationArtifacts compile(String source, boolean enableOpt) {
        String safeSource = source == null ? "" : source;
        List<String> errors = new ArrayList<>();

        Lexer lexer = new Lexer(safeSource);
        List<Token> tokens = lexer.scanTokens();
        List<String> lexErrors = lexer.getErrors();
        String lexerText = formatTokens(tokens);
        if (!lexErrors.isEmpty()) {
            errors.addAll(lexErrors);
            return new CompilationArtifacts(
                    lexerText,
                    "",
                    "",
                    "",
                    "",
                    joinErrors(errors),
                    tokens,
                    List.of(),
                    List.of(),
                    List.of()
            );
        }

        List<String> parseErrors = new ArrayList<>();
        AST.Program program = Parser.analyze(tokens, parseErrors);
        if (!parseErrors.isEmpty()) {
            errors.addAll(parseErrors);
            return new CompilationArtifacts(
                    lexerText,
                    "",
                    "",
                    "",
                    "",
                    joinErrors(errors),
                    tokens,
                    List.of(),
                    List.of(),
                    List.of()
            );
        }

        String astText = AST.Printer.print(program);

        List<String> semErrors = new ArrayList<>();
        Semantic.Result sem = Semantic.analyze(program, semErrors);
        if (!semErrors.isEmpty()) {
            errors.addAll(semErrors);
            return new CompilationArtifacts(
                    lexerText,
                    astText,
                    "",
                    "",
                    "",
                    joinErrors(errors),
                    tokens,
                    List.of(),
                    List.of(),
                    List.of()
            );
        }

        List<String> irErrors = new ArrayList<>();
        List<IR.Quad> irBefore = IR.generate(program, sem, irErrors);
        if (!irErrors.isEmpty()) {
            errors.addAll(irErrors);
            return new CompilationArtifacts(
                    lexerText,
                    astText,
                    "",
                    "",
                    "",
                    joinErrors(errors),
                    tokens,
                    irBefore,
                    List.of(),
                    List.of()
            );
        }

        List<IR.Quad> irAfter = enableOpt ? Optimizer.optimize(irBefore) : copyQuads(irBefore);
        List<AsmInstr> asm = CodeGenPseudoAsm.generate(irAfter, sem);
        String irBeforeText = formatQuads(irBefore);
        String irAfterText = formatQuads(irAfter);
        String asmText = AsmPrinter.print(asm);

        try {
            AsmPrinter.writeToFile(Path.of("target", "out.asm"), asm);
        } catch (Exception ex) {
            errors.add("写文件失败: " + ex.getMessage());
        }

        return new CompilationArtifacts(
                lexerText,
                astText,
                irBeforeText,
                irAfterText,
                asmText,
                joinErrors(errors),
                tokens,
                irBefore,
                irAfter,
                asm
        );
    }

    /**
     * 格式化Token列表为文本
     */
    private static String formatTokens(List<Token> tokens) {
        StringBuilder sb = new StringBuilder();
        if (tokens != null) {
            for (Token token : tokens) {
                sb.append(token).append("\n");
            }
        }
        return sb.toString();
    }

    /**
     * 格式化四元式列表为文本
     */
    private static String formatQuads(List<IR.Quad> quads) {
        StringBuilder sb = new StringBuilder();
        if (quads != null) {
            for (int i = 0; i < quads.size(); i++) {
                sb.append(i).append(": ").append(quads.get(i)).append("\n");
            }
        }
        return sb.toString();
    }

    /**
     * 合并错误信息
     */
    private static String joinErrors(List<String> errors) {
        if (errors == null || errors.isEmpty()) {
            return "无错误";
        }
        return String.join("\n", errors);
    }

    /**
     * 复制四元式列表
     */
    private static List<IR.Quad> copyQuads(List<IR.Quad> input) {
        List<IR.Quad> out = new ArrayList<>();
        if (input != null) {
            for (IR.Quad q : input) {
                if (q != null) {
                    out.add(new IR.Quad(q.op, q.arg1, q.arg2, q.result));
                }
            }
        }
        return out;
    }
}