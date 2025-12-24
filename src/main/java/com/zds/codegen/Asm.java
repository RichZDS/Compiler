package com.zds.codegen;

import java.util.ArrayList;
import java.util.List;

/**
 * 汇编代码类 - 存储汇编指令序列
 */
class Asm {
    private final List<CodeGen.Instr> instructions = new ArrayList<>();

    /**
     * 添加汇编指令
     */
    public void add(CodeGen.Instr instr) {
        if (instr != null) {
            instructions.add(instr);
        }
    }

    /**
     * 添加汇编指令
     */
    public void add(String op, String... args) {
        add(CodeGen.Instr.of(op, args));
    }

    /**
     * 添加原始汇编行
     */
    public void addRaw(String line) {
        add(CodeGen.Instr.raw(line));
    }

    /**
     * 获取汇编指令列表
     */
    public List<CodeGen.Instr> instructions() {
        return List.copyOf(instructions);
    }
}
