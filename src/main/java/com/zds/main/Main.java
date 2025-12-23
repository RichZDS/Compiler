package com.zds.main;
import com.zds.lexer.Lexer;
import com.zds.lexer.Token;
import com.zds.parser.AST;
import com.zds.parser.Parser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class Main {

    public static void main(String[] args) throws Exception {
        // 1) 读取 src/main/resources/input.txt
        String source = readResourceOrFallback("input.txt", "src/main/resources/input.txt");

        // 2) 词法分析：source -> tokens
        Lexer lexer = new Lexer(source);
        List<Token> tokens = lexer.scanTokens();

        System.out.println("===== TOKENS =====");
        for (Token t : tokens) System.out.println(t);

        if (!lexer.getErrors().isEmpty()) {
            System.out.println("\n===== LEXER ERRORS =====");
            lexer.getErrors().forEach(System.out::println);
            return;
        }

        // 3) 语法分析：tokens -> AST（Program）
        List<String> parseErrors = new ArrayList<>();
        AST.Program program = Parser.analyze(tokens, parseErrors);

        if (!parseErrors.isEmpty()) {
            System.out.println("\n===== PARSER ERRORS =====");
            parseErrors.forEach(System.out::println);
            return;
        }

        // 4) 输出 AST（语法分析树）
        System.out.println("\n===== AST =====");
        System.out.println(AST.Printer.print(program));
    }

    private static String readResourceOrFallback(String resourceName, String fallbackPath) throws IOException {
        InputStream is = Main.class.getClassLoader().getResourceAsStream(resourceName);
        if (is != null) return readAll(is);

        // fallback：IDE 直接跑时常用
        File f = new File(fallbackPath);
        if (f.exists()) return readAll(new FileInputStream(f));

        throw new FileNotFoundException("找不到输入文件：resources/" + resourceName + " 或 " + fallbackPath);
    }

    private static String readAll(InputStream is) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append('\n');
            return sb.toString();
        }
    }
}
