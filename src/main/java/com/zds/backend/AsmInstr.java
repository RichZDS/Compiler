package com.zds.backend;

import java.util.ArrayList;
import java.util.List;

public class AsmInstr {
    private final String op;
    private final List<String> args;
    private final String raw;

    private AsmInstr(String op, List<String> args, String raw) {
        this.op = op;
        this.args = args == null ? List.of() : List.copyOf(args);
        this.raw = raw;
    }

    public static AsmInstr of(String op, String... args) {
        List<String> list = new ArrayList<>();
        if (args != null) {
            for (String arg : args) {
                if (arg != null && !arg.isBlank()) {
                    list.add(arg);
                }
            }
        }
        return new AsmInstr(op, list, null);
    }

    public static AsmInstr raw(String line) {
        return new AsmInstr(null, List.of(), line);
    }

    @Override
    public String toString() {
        if (raw != null) {
            return raw;
        }
        if (args.isEmpty()) {
            return op;
        }
        return op + " " + String.join(", ", args);
    }
}
