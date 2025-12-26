# 优化器 (Optimizer)

## 1. 模块概述
优化器负责对中间代码（四元式）进行**机器无关优化**，以提高代码运行效率或减少代码体积。

- **输入**: 四元式列表 (List<IR.Quad>)
- **输出**: 优化后的四元式列表
- **策略**: 多遍扫描 (Multi-pass)，直到代码不再变化或达到最大迭代次数。

## 2. 门面接口 (Facade)

本模块通过 `com.zds.optimizer.Optimizer` 类对外提供服务。

### 主要方法
- `public static List<IR.Quad> optimize(List<IR.Quad> input)`
  - 执行默认策略的优化（最大 5 遍）。

### 优化特性
- **纯函数式**: 输入列表不会被修改，返回一个新的列表。
- **不动点迭代**: 反复执行优化 Pass，直到代码收敛。

## 3. 内部实现 (Hidden Implementation)

优化逻辑封装在包级私有类 `OptimizationPass` 中。

### 支持的优化技术
1. **常量折叠 (Constant Folding)**:
   - `(:=, 1, _, t1)` + `(+, t1, 2, t2)` -> `(:=, 3, _, t2)`
   - 在编译期直接计算出已知常量的运算结果。

2. **代数化简 (Algebraic Simplification)**:
   - `x + 0` -> `x`
   - `x * 1` -> `x`
   - `x * 0` -> `0`

3. **死代码消除 (Dead Code Elimination, DCE)**:
   - 移除赋值了但从未被使用的临时变量。
   - 分析每个基本块（Block）内的变量活跃性。

### 局限性
- 目前主要针对**基本块内**的局部优化 (Local Optimization)。
- 尚未实现全局的数据流分析（如跨基本块的常量传播）。


# 4.5 目标代码生成 答辩讲解稿
各位老师好，接下来我为大家讲解 4.5 目标代码生成阶段，核心是把 IR 四元式翻译成 B2 伪汇编指令序列。

## 1. 输入与统一入口设计
本阶段的输入包含两部分：
- 核心输入：优化后的 IR 四元式列表 `List<IR.Quad>`；
- 辅助输入：`Semantic.Result`（语义分析阶段的类型信息，用于后续类型判断）。

生成入口是 `CodeGen.generate(quads, sem)`（定义在 CodeGen.java），我们延续了“门面类”的设计思路：
- 内部逻辑：创建 `AsmBuilder` 实例 → 调用 `builder.run(quads, sem)` 完成指令映射 → 返回 `builder.getAsm().instructions()`；
- 对外价值：GUI 或 Service 层只需拿到返回的 `List<Instr>`，调用 `CodeGen.print` 就能得到可直接展示的伪汇编文本，调用方式简洁统一。

## 2. 核心规则：Quad.op 到伪汇编的精准映射
指令映射的核心逻辑集中在 `AsmBuilder.emitQuad`（AsmBuilder.java），按 `Quad.op` 做一对一规则映射：
- 赋值操作 `:=` → `MOV result, arg1`；
- 取反操作 `neg` → `NEG result, arg1`；
- 标签操作 `label` → `LABEL Lx`；
- 无条件跳转 `j` → `JMP Lx`；
- 条件跳转（`j<`/`j<=`/`j>`/`j>=`/`j==`/`j!=`）→ 对应 `JLT/JLE/JGT/JGE/JEQ/JNE a, b, Lx`；
- 算术运算（`+`/`-`/`*`/`/`）→ 对应 `ADD/SUB/MUL/DIV result, a, b`。

## 3. 加分亮点：支持字符串拼接的类型推断
针对 `+` 运算，我们没有简单固定为 `ADD`，而是结合类型推断实现了“数值加法”和“字符串拼接”的区分：
### （1）类型追踪基础
AsmBuilder 内部维护 `tempTypes`（`Map<temp, ValueType>`），专门追踪临时变量 `t*` 的类型；
### （2）typeOf 类型识别
`typeOf` 方法精准识别不同值的类型：
- 字符串字面量 → `STRING`；
- 数字字面量 → `INT/DOUBLE`；
- 临时变量 `t*` → 从 `tempTypes` 中查询；
- 普通变量名 → 从 `sem.global.resolve(name)` 查符号表获取类型；
### （3）inferBinaryType 运算类型判断
`inferBinaryType(op, left, right)` 决定最终运算类型：
- 若 `op='+'` 且任意一侧为 `STRING` → 映射为 `CONCAT`（字符串拼接）；
- 否则按类型优先级：含 `DOUBLE` 则为浮点运算，否则为整型运算。

## 4. 容错与扩展设计
针对暂不支持的 `op`，我们设计了保底策略：输出 `;;UNSUPPORTED <quad>` 注释行。
该设计的价值：调试时能快速定位缺失的映射规则，也为后续扩展指令集预留了清晰的扩展入口。

## 5. 输出与阶段价值
- 指令管理：`Asm`（Asm.java）负责统一收集映射后的指令，`Instr`（CodeGen.Instr）定义标准化的指令输出格式（`"OP arg1, arg2, ..."`）；
- 核心价值：将 IR 层面的控制流（label/jump）和计算逻辑（MOV/ADD/CONCAT 等），完整映射为线性的伪汇编指令序列，既支持可视化展示，也为后续的指令执行/模拟提供了直接可用的输出。