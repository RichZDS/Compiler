# 编译器服务 (Compiler Service)

## 1. 模块概述
编译器服务模块是整个项目的**总指挥**，负责将词法分析、语法分析、语义分析、中间代码生成、优化和代码生成等各个子模块串联起来，形成完整的编译流水线。

- **输入**: 源代码字符串
- **输出**: 编译产物 (Artifacts)，包含各阶段的中间结果和最终目标代码。
- **角色**: Facade of Facades (总门面)

## 2. 门面接口 (Facade)

本模块通过 `com.zds.service.CompilerService` 类对外提供服务。

### 主要方法
- `public static Artifacts compile(String source, boolean enableOpt)`
  - 一站式编译方法。
  - 如果任何阶段出错，会立即停止后续步骤，并返回包含错误信息的 Artifacts。

### 数据结构
- **Artifacts**: 编译产物容器
  - `tokens`: 词法单元列表
  - `ast`: 抽象语法树
  - `irBefore`: 优化前的四元式
  - `irAfter`: 优化后的四元式
  - `asm`: 汇编指令
  - `errorText`: 错误汇总信息
  - 提供对应的文本格式化方法（如 `lexerText()`, `astText()`），便于 UI 展示。

## 3. 编译流水线 (Pipeline)

1. **Lexer**: String -> List<Token>
2. **Parser**: List<Token> -> AST
3. **Semantic**: AST -> Semantic.Result (Symbol Table + Type Check)
4. **IR**: AST + Semantic.Result -> List<Quad>
5. **Optimizer**: List<Quad> -> List<Quad> (Optional)
6. **CodeGen**: List<Quad> -> List<Instr> -> File

任何步骤抛出错误都会中断流程，确保错误信息准确传导至用户界面。
