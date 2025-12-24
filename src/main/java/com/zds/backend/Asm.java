package com.zds.backend;

import java.util.ArrayList;
import java.util.List;

public class Asm {
    private final List<AsmInstr> instructions = new ArrayList<>();

    public void add(AsmInstr instr) {
        if (instr != null) {
            instructions.add(instr);
        }
    }

    public void add(String op, String... args) {
        add(AsmInstr.of(op, args));
    }

    public void addRaw(String line) {
        add(AsmInstr.raw(line));
    }

    public List<AsmInstr> instructions() {
        return List.copyOf(instructions);
    }
}
