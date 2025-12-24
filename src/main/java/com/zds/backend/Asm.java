package com.zds.backend;

import java.util.ArrayList;
import java.util.List;

/**
 * 汇编代码类 - 存储汇编指令序列
 */
public class Asm {
    private final List<AsmInstr> instructions = new ArrayList<>();

    /**
     * 添加汇编指令
     */
    public void add(AsmInstr instr) {
        if (instr != null) {
            instructions.add(instr);
        }
    }

    /**
     * 添加汇编指令
     */
    public void add(String op, String... args) {
        add(AsmInstr.of(op, args));
    }

    /**
     * 添加原始汇编行
     */
    public void addRaw(String line) {
        add(AsmInstr.raw(line));
    }

    /**
     * 获取汇编指令列表
     */
    public List<AsmInstr> instructions() {
        return List.copyOf(instructions);
    }
}