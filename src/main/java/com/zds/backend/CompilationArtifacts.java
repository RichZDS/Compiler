package com.zds.backend;

import com.zds.IR.IR;
import com.zds.lexer.Token;

import java.util.Collections;
import java.util.List;

/**
 * 编译产物类 - 存储编译各个阶段的输出结果
 */
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

    /**
     * 创建空的编译产物
     */
    public static CompilationArtifacts empty() {
        return new CompilationArtifacts("", "", "", "", "", "", List.of(), List.of(), List.of(), List.of());
    }

    /**
     * 获取词法分析结果文本
     */
    public String lexerText() {
        return lexerText;
    }

    /**
     * 获取语法分析结果文本
     */
    public String astText() {
        return astText;
    }

    /**
     * 获取优化前的中间代码文本
     */
    public String irBeforeText() {
        return irBeforeText;
    }

    /**
     * 获取优化后的中间代码文本
     */
    public String irAfterText() {
        return irAfterText;
    }

    /**
     * 获取汇编代码文本
     */
    public String asmText() {
        return asmText;
    }

    /**
     * 获取错误信息文本
     */
    public String errorText() {
        return errorText;
    }

    /**
     * 获取词法分析的Token列表
     */
    public List<Token> tokens() {
        return Collections.unmodifiableList(tokens);
    }

    /**
     * 获取优化前的中间代码四元式列表
     */
    public List<IR.Quad> irBefore() {
        return Collections.unmodifiableList(irBefore);
    }

    /**
     * 获取优化后的中间代码四元式列表
     */
    public List<IR.Quad> irAfter() {
        return Collections.unmodifiableList(irAfter);
    }

    /**
     * 获取汇编指令列表
     */
    public List<AsmInstr> asm() {
        return Collections.unmodifiableList(asm);
    }

    /**
     * 判断是否存在错误
     */
    public boolean hasErrors() {
        return errorText != null && !errorText.isBlank() && !"无错误".equals(errorText);
    }
}