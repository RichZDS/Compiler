package com.zds.backend;

import com.zds.IR;
import com.zds.Semantic;
import com.zds.lexer.Lexer;
import com.zds.lexer.Token;
import com.zds.parser.AST;
import com.zds.parser.Parser;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class CompilerService {
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

    private static String formatTokens(List<Token> tokens) {
        StringBuilder sb = new StringBuilder();
        if (tokens != null) {
            for (Token token : tokens) {
                sb.append(token).append("\n");
            }
        }
        return sb.toString();
    }

    private static String formatQuads(List<IR.Quad> quads) {
        StringBuilder sb = new StringBuilder();
        if (quads != null) {
            for (int i = 0; i < quads.size(); i++) {
                sb.append(i).append(": ").append(quads.get(i)).append("\n");
            }
        }
        return sb.toString();
    }

    private static String joinErrors(List<String> errors) {
        if (errors == null || errors.isEmpty()) {
            return "无错误";
        }
        return String.join("\n", errors);
    }

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
