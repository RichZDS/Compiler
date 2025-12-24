package com.zds.backend;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class AsmPrinter {
    public static String print(List<AsmInstr> instructions) {
        StringBuilder sb = new StringBuilder();
        if (instructions != null) {
            for (AsmInstr instr : instructions) {
                if (instr != null) {
                    sb.append(instr).append("\n");
                }
            }
        }
        return sb.toString();
    }

    public static void writeToFile(Path path, List<AsmInstr> instructions) throws IOException {
        if (path == null) {
            return;
        }
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        String content = print(instructions);
        Files.writeString(path, content, StandardCharsets.UTF_8);
    }
}
