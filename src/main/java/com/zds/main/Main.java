package com.zds.main;
import com.zds.IR;
import com.zds.Semantic;
import com.zds.lexer.Lexer;
import com.zds.lexer.Token;
import com.zds.parser.AST;
import com.zds.parser.Parser;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 编译器主入口（Main）
 *
 * 读取：src/main/resources/input.txt
 * 流程：
 * 1) 词法分析 Lexer -> tokens
 * 2) 语法分析 Parser -> AST
 * 3) 输出 AST
 * 4) 语义分析 Semantic -> 符号表/类型检查
 * 5) IR 生成 -> 四元式输出
 */
public class Main {

    public static void main(String[] args) throws Exception {
        String source = readResource("input.txt");

        // 1) Lexer
        Lexer lexer = new Lexer(source);
        List<Token> tokens = lexer.scanTokens();
        List<String> lexErrors = lexer.getErrors();

        if (!lexErrors.isEmpty()) {
            System.out.println("===== 词法错误 Lexer Errors =====");
            for (String e : lexErrors) System.out.println(e);
            return;
        }

        // 2) Parser -> AST
        List<String> parseErrors = new ArrayList<>();
        AST.Program program = Parser.analyze(tokens, parseErrors);

        if (!parseErrors.isEmpty()) {
            System.out.println("===== 语法错误 Parser Errors =====");
            for (String e : parseErrors) System.out.println(e);
            return;
        }

        // 3) Print AST
        System.out.println("===== AST 语法分析树 =====");
        System.out.print(AST.Printer.print(program));

        // 4) Semantic
        List<String> semErrors = new ArrayList<>();
        Semantic.Result sem = Semantic.analyze(program, semErrors);

        if (!semErrors.isEmpty()) {
            System.out.println("===== 语义错误 Semantic Errors =====");
            for (String e : semErrors) System.out.println(e);
            return;
        }

        System.out.println("\n===== 符号表 Symbol Table（最小版：global）=====");
        System.out.print(sem.dumpSymbolTable());

        // 5) IR
        List<String> irErrors = new ArrayList<>();
        List<IR.Quad> quads = IR.generate(program, sem, irErrors);

        if (!irErrors.isEmpty()) {
            System.out.println("===== IR 生成错误 IR Errors =====");
            for (String e : irErrors) System.out.println(e);
            return;
        }

        System.out.println("\n===== 四元式 Quadruples =====");
        IR.print(quads);
    }

    private static String readResource(String name) throws Exception {
        try (InputStream in = Main.class.getClassLoader().getResourceAsStream(name)) {
            if (in == null) {
                throw new RuntimeException("找不到资源文件: src/main/resources/" + name);
            }
            byte[] bytes = in.readAllBytes();
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }
}
