# 语法分析器 (Parser)

## 1. 模块概述
语法分析器是编译器的第二个阶段，负责将 Token 序列转换为**抽象语法树 (AST)**。

- **输入**: Token 列表 (List<Lexer.Token>)
- **输出**: 程序 AST 根节点 (Parser.Program)
- **核心算法**: 递归下降分析 (Recursive Descent Parsing)

## 2. 门面接口 (Facade)

本模块通过 `com.zds.parser.Parser` 类对外提供服务。

### 主要方法
- `public static Program analyze(List<Lexer.Token> tokens, List<String> outErrors)`
  - 执行语法分析，返回 AST。如果遇到语法错误，会尝试同步并继续解析，错误信息收集到 `outErrors` 中。

### 数据结构 (AST Nodes)
所有 AST 节点都定义在 `Parser` 类的内部，主要包括：

#### 语句 (Stmt)
- `Block`: 代码块 `{ ... }`
- `VarDecl`: 变量声明 `int a = 10;`
- `Assign`: 赋值语句 `a = 20;`
- `IfStmt`: 条件语句 `if (...) ... else ...`
- `WhileStmt`: 循环语句 `while (...) ...`
- `ForStmt`: 循环语句 `for (...;...;...) ...`
- `ExprStmt`: 表达式语句 `a + b;`

#### 表达式 (Expr)
- `Binary`: 二元运算 `a + b`, `a > b`
- `Unary`: 一元运算 `-a`, `!b`
- `Literal`: 字面量 `123`, `"hello"`
- `Var`: 变量引用 `a`

## 3. 内部实现 (Hidden Implementation)

解析逻辑封装在包级私有类 `Recognizer` 和 `ErrorHandling` 中。

### 文法规则 (简略)
```text
program     -> statement* EOF
statement   -> block | ifStmt | whileStmt | forStmt | varDecl | assignStmt | exprStmt
block       -> "{" statement* "}"
varDecl     -> type IDENT ("=" expression)? ";"
expression  -> additive
additive    -> multiplicative (("+" | "-") multiplicative)*
```

### 错误恢复
当遇到语法错误时（如缺少分号），解析器会进入 `panic mode`，调用 `ErrorHandling.synchronize()` 方法，丢弃 Token 直到找到语句边界（如分号或关键字），从而避免错误的级联效应。
