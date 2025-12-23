package com.zds.main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.zds.lexer.Lexer;
import com.zds.parser.Parser;

/**
 * 编译器主控入口（框架阶段）
 *
 * ✅ 按"编译原理PPT"常用术语来理解：
 * 1) 输入：源程序（source program）
 * 2) 词法分析：把字符流切成单词符号/记号（Token），也就是二元式 <种别码, 属性值>
 * 3) 语法分析（预留）：用 LL(1) 的递归下降（自顶向下预测分析）把 Token 串组织成语法结构（AST）
 *
 * 现在先把 Main + Lexer + Parser 框架跑通：
 * 源程序 -> Lexer -> Token 序列 -> (Parser 占位校验)
 */
public class Main {
    public static void main(String[] args) {

        // 约定：源程序文件放在 src/main/resources 下
        // 运行时你只需要传资源文件名，例如：java com.zds.Main input.txt
        // 不传参则默认读取 input.txt
        String resourceName = (args != null && args.length > 0 && args[0] != null && !args[0].isBlank())
                ? args[0].trim()
                : "input.txt";

        try {
            String source = readFromResources(resourceName);
            System.out.println("==== SOURCE (from resources: " + resourceName + ") ====");
            System.out.println(source);

            // 1) 词法分析：输出 Token（二元式 <种别码, 属性值>）
            Lexer lexer = new Lexer(source);
            List<com.zds.lexer.Token> tokens = lexer.scanTokens();

            System.out.println("\n==== TOKENS (二元式 <种别码, 属性值>) ====");
            for (int i = 0; i < tokens.size(); i++) {
                System.out.printf("%4d  %s%n", i, tokens.get(i));
            }

            if (!lexer.getErrors().isEmpty()) {
                System.out.println("\n==== LEXER ERRORS (词法错误) ====");
                for (String e : lexer.getErrors()) {
                    System.out.println(e);
                }
                // 词法阶段都出错了，后面语法分析就不继续了
                return;
            }

            // 2) 语法分析：当前阶段先做语法校验（后续再扩展为构建 AST）
            com.zds.parser.Parser parser = new Parser(tokens);
            parser.parse();

            if (!parser.getErrors().isEmpty()) {
                System.out.println("\n==== PARSER ERRORS (语法错误) ====");
                for (String e : parser.getErrors()) {
                    System.out.println(e);
                }
            } else {
                System.out.println("\n==== PARSER ====");
                System.out.println("语法分析通过（当前阶段：Parser 只做基本校验，占位框架OK）");
            }

        } catch (Exception ex) {
            System.err.println("运行失败：" + ex.getMessage());
            ex.printStackTrace();
        }
    }

    /**
     * 从 classpath(resources) 读取源程序。
     * 你只要把文件放到：src/main/resources/<resourceName>
     */
    private static String readFromResources(String resourceName) throws IOException {
        InputStream in = Main.class.getClassLoader().getResourceAsStream(resourceName);
        if (in == null) {
            throw new IOException("在 resources 下找不到文件：" + resourceName + "（请确认放在 src/main/resources/ 下并且已被构建系统打包到 classpath）");
        }

        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append('\n');
            }
        }
        return sb.toString();
    }
}