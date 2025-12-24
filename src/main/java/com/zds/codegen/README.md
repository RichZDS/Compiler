# 代码生成器 (Code Generator)

## 1. 模块概述
代码生成器是编译器的后端，负责将平台无关的中间代码（四元式）翻译为**目标机器代码**（这里是自定义的伪汇编指令）。

- **输入**: 四元式列表 (List<IR.Quad>) + 语义信息 (Semantic.Result)
- **输出**: 汇编指令列表 (List<CodeGen.Instr>)
- **目标**: 生成线性的、类似汇编的指令序列。

## 2. 门面接口 (Facade)

本模块通过 `com.zds.codegen.CodeGen` 类对外提供服务。

### 主要方法
- `public static List<Instr> generate(List<IR.Quad> quads, Semantic.Result sem)`
  - 执行代码生成。
- `public static String print(List<Instr> instructions)`
  - 将指令列表转换为文本格式。
- `public static void writeToFile(Path path, List<Instr> instructions)`
  - 将指令写入文件。

### 数据结构
- **Instr**: 汇编指令
  - `op`: 操作码（如 `MOV`, `ADD`, `JMP`）
  - `args`: 操作数列表

## 3. 内部实现 (Hidden Implementation)

生成逻辑封装在包级私有类 `AsmBuilder` 中。

### 指令映射规则
简单的一对一或一对多映射：

| 四元式 (Quad) | 汇编 (Asm) | 说明 |
| :--- | :--- | :--- |
| `(:=, src, _, dest)` | `MOV dest, src` | 数据移动 |
| `(+, a, b, res)` | `ADD res, a, b` | 加法 |
| `(neg, a, _, res)` | `NEG res, a` | 取反 |
| `(label, L1, _, _)` | `LABEL L1` | 标签定义 |
| `(j, _, _, L1)` | `JMP L1` | 无条件跳转 |
| `(j<, a, b, L1)` | `JLT L1, a, b` | 小于跳转 |

### 类型推断
在生成 `ADD` 等指令时，会利用语义分析阶段的类型信息，区分整数加法 (`ADD`) 和字符串拼接 (`CONCAT`)。
