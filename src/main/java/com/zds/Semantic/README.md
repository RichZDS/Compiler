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



第一张：语义分析主流程图（Flow）
1）输入和入口（20 秒）

语义分析的输入是 Parser 输出的 Program AST，也就是整棵抽象语法树的根节点。
对外的统一入口是 Semantic.analyze(program, outErrors)，它是一个 Facade：外部只需要调用这个方法，不用关心内部怎么遍历。

2）Analyzer 初始化（20 秒）

进入 Analyzer.run(program) 后，第一步会初始化三类核心状态：

current = new Scope(null, 0)：创建全局作用域

errors = []：收集语义错误

exprTypes = IdentityHashMap<Expr, Type>：记录每个表达式节点推导出的类型

这三样就是语义分析阶段的核心产物与中间状态。

3）遍历语句 checkStmt（30 秒）

接下来对 program.statements 做循环，逐条调用 checkStmt(s)。
checkStmt 内部会根据语句类型做 instanceof 分流，也就是图中菱形节点。

4）各类语句规则（60 秒）

Block：进入块时 beginScope()，离开块时 endScope()，实现局部变量作用域

VarDecl：解析声明类型 parseType，然后 current.define 放进符号表；如果有初始化表达式则 checkExpr(init) 推导右值类型，最后用 assignable 判断是否能赋值

Assign：先 resolve(name) 查变量是否声明，没声明就报错；但为了收集更多错误仍然会继续检查右值表达式；再用 assignable 做类型兼容判断

If / While / For：都会先做 checkCondition，并且要求结果必须是 BOOL（或 ERROR），否则报错

For：它会先 beginScope()，让 for-init 声明的变量在循环体里可见，再检查 init/cond/step/body，最后 endScope()

5）表达式 checkExpr（60 秒）

右侧是表达式检查 checkExpr(expr)：它不仅推导类型，还做语义规则检查，并把结果写入 exprTypes。

Literal：根据值推导 INT/DOUBLE/STRING

Var：必须在符号表中能 resolve，否则报 “未声明使用” 并记为 ERROR

Unary：只支持 +/- 作用在数字类型上

Binary：按运算符分类

+：若任一侧是 STRING，则做字符串拼接；否则两侧 numeric 做数字加法

- * /：只允许 numeric

比较 >,>=,<,<=,==,!=：结果是 BOOL，但对左右类型有约束（例如大小比较只允许 numeric）

6）输出（15 秒）

遍历结束后返回 Semantic.Result：包含全局 Scope、errors 列表、exprTypes 映射。
同时 Facade 会把 errors 追加进 outErrors，方便 GUI/CLI 一次性展示全部语义错误。
