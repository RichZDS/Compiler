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


1. 数据移动指令
   MOV dest, src: 将源操作数 src 的值移动到目标操作数 dest。
   映射自四元式 (:=, src, _, dest)。
   示例: MOV t1, 5 (将常量5赋值给临时变量t1)。
2. 算术运算指令
   ADD res, a, b: 计算 a + b 并将结果存入 res。
   映射自四元式 (+, a, b, res)。
   示例: ADD t2, t1, 3。
   SUB res, a, b: 计算 a - b 并将结果存入 res。
   映射自四元式 (-, a, b, res)。
   MUL res, a, b: 计算 a * b 并将结果存入 res。
   映射自四元式 (*, a, b, res)。
   DIV res, a, b: 计算 a / b 并将结果存入 res。
   映射自四元式 (/, a, b, res)。
   NEG res, a: 计算 -a 并将结果存入 res。
   映射自四元式 (neg, a, _, res)。
   示例: NEG t3, t2 (计算t2的负值)。
3. 字符串操作指令
   CONCAT res, a, b: 将字符串 a 和 b 拼接起来，结果存入 res。
   映射自四元式 (+, a, b, res)，但当类型推断发现操作数为字符串时使用此指令。
   示例: CONCAT t4, "Hello", "World"。
4. 控制流指令
   JMP Lx: 无条件跳转到标签 Lx。
   映射自四元式 (j, _, _, Lx)。
   JLT Lx, a, b: 如果 a < b，则跳转到标签 Lx。
   映射自四元式 (j<, a, b, Lx)。
   JLE Lx, a, b: 如果 a <= b，则跳转到标签 Lx。
   映射自四元式 (j<=, a, b, Lx)。
   JGT Lx, a, b: 如果 a > b，则跳转到标签 Lx。
   映射自四元式 (j>, a, b, Lx)。
   JGE Lx, a, b: 如果 a >= b，则跳转到标签 Lx。
   映射自四元式 (j>=, a, b, Lx)。
   JEQ Lx, a, b: 如果 a == b，则跳转到标签 Lx。
   映射自四元式 (j==, a, b, Lx)。
   JNE Lx, a, b: 如果 a != b，则跳转到标签 Lx。
   映射自四元式 (j!=, a, b, Lx)。
5. 标签定义指令
   LABEL Lx: 定义一个标签 Lx，供跳转指令使用。
   映射自四元式 (label, Lx, _, _)。
   示例: LABEL L1。
6. 其他指令
   ;;UNSUPPORTED ...: 这不是一个真正的指令，而是一个注释行，用于标记那些无法处理或不支持的四元式。它由 asm.addRaw(";;UNSUPPORTED " + q); 生成，便于调试。