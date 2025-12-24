# 语义分析器 (Semantic Analyzer)

## 1. 模块概述
语义分析器负责对抽象语法树 (AST) 进行**上下文相关**的检查，确保程序不仅符合文法规则，而且在语义上是合法的。

- **输入**: 程序 AST (Parser.Program)
- **输出**: 语义分析结果 (Semantic.Result)，包含符号表、表达式类型映射和错误列表。
- **核心功能**:
  - **符号表管理**: 建立作用域 (Scope)，记录变量声明与查找。
  - **类型检查**: 确保赋值类型兼容、运算符操作数合法。
  - **控制流检查**: 确保 if/while/for 的条件表达式为布尔类型。

## 2. 门面接口 (Facade)

本模块通过 `com.zds.Semantic.Semantic` 类对外提供服务。

### 主要方法
- `public static Result analyze(Parser.Program program, List<String> outErrors)`
  - 执行语义分析。如果有错误，会将错误信息追加到 `outErrors` 并记录在返回的 `Result` 对象中。

### 数据结构
- **Result**: 分析结果容器
  - `global`: 全局作用域 (Scope)
  - `errors`: 错误列表
  - `getType(Expr)`: 获取某个表达式节点的推导类型
- **Scope**: 作用域
  - 支持嵌套 (Parent Scope)
  - `define(name, type)`: 定义符号
  - `resolve(name)`: 查找符号
- **Type**: 类型枚举
  - `INT`, `DOUBLE`, `STRING`, `BOOL`, `VOID`

## 3. 内部实现 (Hidden Implementation)

具体的分析逻辑封装在包级私有类 `Analyzer` 中，采用访问者模式（或类似的遍历模式）遍历 AST。

### 检查规则示例
1. **变量声明**: 检查当前作用域是否已存在同名变量（重复声明错误）。
2. **变量使用**: 递归查找符号表，如果未找到则报“未声明”错误。
3. **二元运算**:
   - `+`: 支持数字相加或字符串拼接。
   - `-`, `*`, `/`: 仅支持数字。
   - `>`: 仅支持数字比较，结果为 BOOL。
4. **类型兼容性**: 允许 `int` 隐式转换为 `double`，但反之不行。

### 符号表结构
```text
Global Scope
  ├── int a
  ├── double b
  └── Block Scope (if/while/block)
        ├── int local_var
        └── ...
```
