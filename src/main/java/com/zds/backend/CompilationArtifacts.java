package com.zds.backend;

import com.zds.IR.IR;
import com.zds.lexer.Token;

import java.util.Collections;
import java.util.List;

public class CompilationArtifacts {
    private final String lexerText;
    private final String astText;
    private final String irBeforeText;
    private final String irAfterText;
    private final String asmText;
    private final String errorText;
    private final List<Token> tokens;
    private final List<IR.Quad> irBefore;
    private final List<IR.Quad> irAfter;
    private final List<AsmInstr> asm;

    public CompilationArtifacts(
            String lexerText,
            String astText,
            String irBeforeText,
            String irAfterText,
            String asmText,
            String errorText,
            List<Token> tokens,
            List<IR.Quad> irBefore,
            List<IR.Quad> irAfter,
            List<AsmInstr> asm
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

    public static CompilationArtifacts empty() {
        return new CompilationArtifacts("", "", "", "", "", "", List.of(), List.of(), List.of(), List.of());
    }

    public String lexerText() {
        return lexerText;
    }

    public String astText() {
        return astText;
    }

    public String irBeforeText() {
        return irBeforeText;
    }

    public String irAfterText() {
        return irAfterText;
    }

    public String asmText() {
        return asmText;
    }

    public String errorText() {
        return errorText;
    }

    public List<Token> tokens() {
        return Collections.unmodifiableList(tokens);
    }

    public List<IR.Quad> irBefore() {
        return Collections.unmodifiableList(irBefore);
    }

    public List<IR.Quad> irAfter() {
        return Collections.unmodifiableList(irAfter);
    }

    public List<AsmInstr> asm() {
        return Collections.unmodifiableList(asm);
    }

    public boolean hasErrors() {
        return errorText != null && !errorText.isBlank() && !"无错误".equals(errorText);
    }
}
