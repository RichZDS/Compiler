package com.zds.main;

import com.zds.backend.CompilationArtifacts;
import com.zds.backend.CompilerService;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
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
    private final JTextArea lexerArea = new JTextArea();
    private final JTextArea astArea = new JTextArea();
    private final JTextArea irBeforeArea = new JTextArea();
    private final JTextArea irAfterArea = new JTextArea();
    private final JTextArea asmArea = new JTextArea();
    private final JTextArea errorArea = new JTextArea();
    private CompilationArtifacts lastResult = CompilationArtifacts.empty();

    public void show() {
        JFrame frame = new JFrame("简易编译器 GUI");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        sourceArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        configureOutputArea(lexerArea);
        configureOutputArea(astArea);
        configureOutputArea(irBeforeArea);
        configureOutputArea(irAfterArea);
        configureOutputArea(asmArea);
        configureOutputArea(errorArea);

        sourceArea.setText(readResource("input.txt"));

        JScrollPane sourceScroll = new JScrollPane(sourceArea);
        sourceScroll.setBorder(BorderFactory.createTitledBorder("源代码 (input.txt)"));

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("词法分析", wrapOutput("词法分析", lexerArea));
        tabs.addTab("语法分析", wrapOutput("语法分析", astArea));
        tabs.addTab("中间代码", wrapOutput("中间代码", irBeforeArea));
        tabs.addTab("优化代码", wrapOutput("优化代码", irAfterArea));
        tabs.addTab("目标代码", wrapOutput("目标代码", asmArea));
        tabs.addTab("错误信息", wrapOutput("错误信息", errorArea));

        JPanel rightTop = new JPanel(new BorderLayout());
        javax.swing.JButton runButton = new javax.swing.JButton("运行编译");
        runButton.addActionListener(event -> runCompilation());

        JPanel runPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        runPanel.add(runButton);
        rightTop.add(runPanel, BorderLayout.NORTH);
        rightTop.add(tabs, BorderLayout.CENTER);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, sourceScroll, rightTop);
        splitPane.setResizeWeight(0.5);
        splitPane.setDividerLocation(520);

        frame.add(splitPane, BorderLayout.CENTER);
        frame.setPreferredSize(new Dimension(1100, 700));
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private void runCompilation() {
        String source = sourceArea.getText();
        lastResult = CompilerService.compile(source, true);
        lexerArea.setText(lastResult.lexerText());
        astArea.setText(lastResult.astText());
        irBeforeArea.setText(lastResult.irBeforeText());
        irAfterArea.setText(lastResult.irAfterText());
        asmArea.setText(lastResult.asmText());
        errorArea.setText(lastResult.errorText());

        resetCarets();
    }

    private void configureOutputArea(JTextArea area) {
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        area.setEditable(false);
    }

    private JScrollPane wrapOutput(String title, JTextArea area) {
        JScrollPane scrollPane = new JScrollPane(area);
        scrollPane.setBorder(BorderFactory.createTitledBorder(title));
        return scrollPane;
    }

    private void resetCarets() {
        lexerArea.setCaretPosition(0);
        astArea.setCaretPosition(0);
        irBeforeArea.setCaretPosition(0);
        irAfterArea.setCaretPosition(0);
        asmArea.setCaretPosition(0);
        errorArea.setCaretPosition(0);
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
