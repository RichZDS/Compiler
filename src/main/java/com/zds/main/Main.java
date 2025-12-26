package com.zds.main;
import com.zds.service.CompilerService;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

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
        if (args != null && args.length > 0 && "cli".equalsIgnoreCase(args[0])) {
            runConsole();
            return;
        }

        CompilerGui.main(args);
    }

    private static void runConsole() throws Exception {
        String source = readResource("input.txt");

        CompilerService.Artifacts artifacts = CompilerService.compile(source, true);

        if (artifacts.hasErrors()) {
            System.out.println("===== 错误信息 Errors =====");
            System.out.println(artifacts.errorText());
            return;
        }

        System.out.println("===== IR Before =====");
        System.out.print(artifacts.irBeforeText());

        System.out.println("===== IR After =====");
        System.out.print(artifacts.irAfterText());

        System.out.println("===== ASM =====");
        System.out.print(artifacts.asmText());
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