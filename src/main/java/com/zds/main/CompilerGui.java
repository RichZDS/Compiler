package com.zds.main;


import com.zds.IR.IR;
import com.zds.Semantic.Semantic;
import com.zds.lexer.Lexer;
import com.zds.lexer.Token;
import com.zds.parser.AST;
import com.zds.parser.Parser;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import java.util.ArrayList;
import java.util.List;

public class CompilerGui {
    private final JTextArea sourceArea = new JTextArea();
    private final JTextArea outputArea = new JTextArea();

    private CompilationResult lastResult = CompilationResult.empty();

    public void show() {
        JFrame frame = new JFrame("简易编译器 GUI");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        sourceArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        outputArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        outputArea.setEditable(false);

        sourceArea.setText(readResource("input.txt"));

        JScrollPane sourceScroll = new JScrollPane(sourceArea);
        sourceScroll.setBorder(BorderFactory.createTitledBorder("源代码 (input.txt)"));

        JScrollPane outputScroll = new JScrollPane(outputArea);
        outputScroll.setBorder(BorderFactory.createTitledBorder("输出"));

        JPanel rightTop = new JPanel(new BorderLayout());
        JButton runButton = new JButton("运行编译");
        runButton.addActionListener(event -> runCompilation());

        JPanel runPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        runPanel.add(runButton);
        rightTop.add(runPanel, BorderLayout.NORTH);
        rightTop.add(outputScroll, BorderLayout.CENTER);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, sourceScroll, rightTop);
        splitPane.setResizeWeight(0.5);
        splitPane.setDividerLocation(520);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

        buttonPanel.add(sectionButton("词法分析", () -> showOutput(lastResult.lexerOutput)));
        buttonPanel.add(sectionButton("语法分析", () -> showOutput(lastResult.parserOutput)));
        buttonPanel.add(sectionButton("中间代码", () -> showOutput(lastResult.irOutput)));
        buttonPanel.add(sectionButton("优化代码", () -> showOutput(lastResult.optimizedOutput)));
        buttonPanel.add(sectionButton("目标代码", () -> showOutput(lastResult.targetOutput)));
        buttonPanel.add(sectionButton("错误信息", () -> showOutput(lastResult.errorOutput)));
        frame.add(splitPane, BorderLayout.CENTER);
        frame.add(buttonPanel, BorderLayout.SOUTH);
        frame.setPreferredSize(new Dimension(1100, 700));
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private JButton sectionButton(String label, Runnable action) {
        JButton button = new JButton(label);
        button.addActionListener(event -> action.run());
        return button;
    }

    private void runCompilation() {
        String source = sourceArea.getText();

        lastResult = compileSource(source);
        showOutput(lastResult.lexerOutput);
    }

    private void showOutput(String text) {
        outputArea.setText(text == null ? "" : text);
        outputArea.setCaretPosition(0);
    }


    private CompilationResult compileSource(String source) {
        List<String> errors = new ArrayList<>();

        Lexer lexer = new Lexer(source);
        List<Token> tokens = lexer.scanTokens();
        List<String> lexErrors = lexer.getErrors();
        String lexerOutput = formatTokens(tokens);
        if (!lexErrors.isEmpty()) {
            errors.addAll(lexErrors);
        }

        String parserOutput = "";
        String irOutput = "";
        if (errors.isEmpty()) {
            List<String> parseErrors = new ArrayList<>();
            AST.Program program = Parser.analyze(tokens, parseErrors);
            if (!parseErrors.isEmpty()) {
                errors.addAll(parseErrors);
            } else {
                parserOutput = AST.Printer.print(program);

                List<String> semErrors = new ArrayList<>();
                Semantic.Result sem = Semantic.analyze(program, semErrors);
                if (!semErrors.isEmpty()) {
                    errors.addAll(semErrors);
                } else {
                    List<String> irErrors = new ArrayList<>();
                    List<IR.Quad> quads = IR.generate(program, sem, irErrors);
                    if (!irErrors.isEmpty()) {
                        errors.addAll(irErrors);
                    } else {
                        irOutput = formatQuads(quads);
                    }
                }
            }
        }

        String errorOutput = errors.isEmpty() ? "无错误" : String.join("\n", errors);

        return new CompilationResult(
                lexerOutput,
                parserOutput.isEmpty() ? "(无语法分析输出)" : parserOutput,
                irOutput.isEmpty() ? "(无中间代码输出)" : irOutput,
                "优化代码尚未实现",
                "目标代码尚未实现",
                errorOutput
        );
    }

    private String formatTokens(List<Token> tokens) {
        StringBuilder sb = new StringBuilder();
        for (Token token : tokens) {
            sb.append(token).append("\n");
        }
        return sb.toString();
    }

    private String formatQuads(List<IR.Quad> quads) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < quads.size(); i++) {
            sb.append(i).append(": ").append(quads.get(i)).append("\n");
        }
        return sb.toString();
    }
    private String readResource(String name) {
        try (InputStream in = CompilerGui.class.getClassLoader().getResourceAsStream(name)) {
            if (in == null) {
                return "";
            }
            byte[] bytes = in.readAllBytes();
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            return "";
        }
    }


    private record CompilationResult(
            String lexerOutput,
            String parserOutput,
            String irOutput,
            String optimizedOutput,
            String targetOutput,
            String errorOutput
    ) {
        static CompilationResult empty() {
            return new CompilationResult("", "", "", "", "", "");
        }
    }
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new CompilerGui().show());
    }
}
