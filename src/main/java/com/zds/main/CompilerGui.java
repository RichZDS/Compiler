package com.zds.main;

import com.zds.backend.CompilationArtifacts;
import com.zds.backend.CompilerService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
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

public class CompilerGui {
    private final JTextArea sourceArea = new JTextArea();
    private final JTextArea outputArea = new JTextArea();
    private CompilationArtifacts lastResult = CompilationArtifacts.empty();

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
        buttonPanel.add(sectionButton("词法分析", () -> showOutput(lastResult.lexerText())));
        buttonPanel.add(sectionButton("语法分析", () -> showOutput(lastResult.astText())));
        buttonPanel.add(sectionButton("中间代码", () -> showOutput(lastResult.irBeforeText())));
        buttonPanel.add(sectionButton("优化代码", () -> showOutput(lastResult.irAfterText())));
        buttonPanel.add(sectionButton("目标代码", () -> showOutput(lastResult.asmText())));
        buttonPanel.add(sectionButton("错误信息", () -> showOutput(lastResult.errorText())));

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
        lastResult = CompilerService.compile(source, true);
        showOutput(lastResult.lexerText());
    }

    private void showOutput(String text) {
        outputArea.setText(text == null ? "" : text);
        outputArea.setCaretPosition(0);
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

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new CompilerGui().show());
    }
}
