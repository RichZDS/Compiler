# 词法分析器 (Lexer)

## 1. 模块概述
词法分析器是编译器的第一个阶段，负责将源代码（字符流）转换为**Token序列**。

- **输入**: 源代码字符串 (String)
- **输出**: Token 列表 (List<Lexer.Token>)
- **核心逻辑**: 有限状态机 (DFA) 扫描

## 2. 门面接口 (Facade)

本模块通过 `com.zds.lexer.Lexer` 类对外提供服务。

### 主要方法
- `public static List<Token> scan(String source)`
  - 执行完整的词法分析，返回 Token 列表。即使遇到错误，也会尝试继续扫描直到文件结束。

### 数据结构
- **Token**: 表示一个单词符号，包含：
  - `type`: 种别码（如 `INT`, `IDENT`, `PLUS`）
  - `lexeme`: 单词原文
  - `literal`: 字面量值（数字或字符串的具体值）
  - `line`, `col`: 源代码中的位置

## 3. 内部实现 (Hidden Implementation)

具体的扫描逻辑封装在包级私有类 `ScannerCore` 中，对外不可见。

### 扫描流程
1. 读取下一个字符。
2. 根据字符特征判断 Token 类型：
   - 空白字符 -> 跳过
   - 数字 -> 进入数字状态 (Integer/Double)
   - 字母 -> 进入标识符/关键字状态
   - 符号 -> 匹配运算符或分隔符
3. 生成 Token 并加入列表。
4. 重复直到文件结束 (EOF)。

### 错误处理
- 遇到非法字符或未闭合的字符串/注释时，生成 `ERROR` 类型的 Token，并记录错误信息。
- 词法错误不会立即终止编译，而是尽可能恢复以发现更多错误。
