# 中间代码生成器 (IR Generator)

## 1. 模块概述
中间代码生成器负责将 AST 转换为**三地址码 (Three-Address Code)** 形式的**四元式 (Quadruple)** 序列。这一层抽象屏蔽了底层机器细节，便于后续优化和多平台代码生成。

- **输入**: 程序 AST (Parser.Program) + 语义信息 (Semantic.Result)
- **输出**: 四元式列表 (List<IR.Quad>)
- **形式**: `(op, arg1, arg2, result)`

## 2. 门面接口 (Facade)

本模块通过 `com.zds.IR.IR` 类对外提供服务。

### 主要方法
- `public static List<Quad> generate(Parser.Program program, Semantic.Result sem, List<String> outErrors)`
  - 遍历 AST，生成对应的四元式序列。

### 数据结构
- **Quad**: 四元式对象
  - `op`: 操作符（如 `+`, `:=`, `j<`）
  - `arg1`, `arg2`: 操作数（变量名、字面量或临时变量 `t1`）
  - `result`: 结果变量或跳转标签 `L1`

## 3. 内部实现 (Hidden Implementation)

生成逻辑封装在包级私有类 `Generator` 中。

### 核心转换逻辑
1. **表达式翻译**:
   - `a + b` -> 生成 `(+, a, b, t1)`，返回临时变量 `t1`。
   - `a = 10` -> 生成 `(:=, 10, _, a)`。

2. **控制流翻译**:
   - **If语句**:
     ```
     if (a < b) S1 else S2
     ```
     转换为：
     ```
     (j<, a, b, L_then)
     (j, _, _, L_else)
     label L_then
     ... code for S1 ...
     (j, _, _, L_end)
     label L_else
     ... code for S2 ...
     label L_end
     ```

   - **While/For循环**: 类似地，通过 Label 和跳转指令构建循环结构。

### 常用指令集
- 运算: `+`, `-`, `*`, `/`, `neg` (取反)
- 赋值: `:=`
- 跳转: `j` (无条件), `j<`, `j==`, `j!=` (条件跳转)
- 标记: `label`



# IR 中间代码生成 答辩讲解稿
## 一、IR 中间代码生成主流程图（Flow）
### 1）输入与入口（20 秒）
IR 阶段的输入是 Parser 输出的 Program AST，接口上还会接收 Semantic.Result 和 outErrors。
入口是 IR.generate(program, sem, outErrors)（在 IR.java），它是一个门面 Facade：上层（GUI 或服务层）只调用一个方法。

**可扩展设计点**：当前版本 Generator 内部暂时没有用到 sem，但保留 sem 在接口中，后续做“类型驱动”的 IR 生成/优化时无需修改接口，是兼顾扩展性的设计。

### 2）核心生成器 Generator.run（20 秒）
Facade 内部会创建 `new Generator(sem)`，然后调用 `g.run(program)`。
run 的核心逻辑：遍历 `program.statements`，对每条语句调用 `genStmt(stmt)`，生成的四元式追加到 out 列表中，最终返回 out 列表。

### 3）genStmt：语句决定控制流骨架（50 秒）
genStmt 按 Stmt 类型分流，这一步决定 IR 的“骨架”：
- Block：直接递归处理子语句；
- VarDecl / Assign：均生成赋值四元式（`rhs = genExpr(...)` → `emit(':=', rhs, '_', name)`）；
- ExprStmt：调用 `genExpr(expr)`，丢弃最终结果，仅保留过程中 emit 的四元式；
- IfStmt / WhileStmt / ForStmt（控制流重点）：通过 `newLabel()` 创建 L1、L2… 标签，结合 label + j/j< 等跳转指令形成控制流图（CFG 的文本形式）。

### 4）If / While / For 的控制流表达（60 秒）
- IfStmt：生成 L_then/L_else/L_end → `emitCondJump(cond, L_then, L_else)` → label L_then（执行 then 分支）→ j L_end → label L_else（执行 else 分支）→ label L_end；
- WhileStmt：生成 L_begin/L_body/L_end → label L_begin → 条件跳转到 body 或 end → body 结束后 j L_begin 回到循环头 → label L_end；
- ForStmt：先执行 init → label L_begin（cond 可选）→ 有 cond 则走 `emitCondJump`，无 cond 则直接 j L_body → 执行 body → 执行 step → j L_begin → label L_end。

**答辩总结句**：控制流语句在 IR 里统一翻译成 label + 条件/无条件跳转，结构直观且可直接用于后续目标代码生成。

### 5）genExpr：表达式三地址化（50 秒）
genExpr(expr) 的输出是一个 place（常量、变量名、临时变量 t1/t2/...），实现表达式“三地址化”：
- Literal：直接返回常量文本（字符串会加引号）；
- Var：返回变量名；
- Unary '-'：创建临时变量 t → `emit('neg', x, '_', t)` → 返回 t；
- Binary：先生成左右子表达式的 place → 创建临时变量 t → `emit(op, a, c, t)` → 返回 t。

**核心本质**：复杂表达式拆分为多条四元式，每一步仅包含三个地址（arg1/arg2/result），这是“三地址化”的核心。

### 6）emitCondJump：条件跳转生成的亮点（40 秒）
emitCondJump(cond, T, F) 设计了两层策略：
- 策略1：cond 是比较二元表达式（< <= > >= == !=）→ 生成 j</j== 等指令跳至 trueLabel，再生成 j 指令跳至 falseLabel；
- 策略2（兜底）：cond 非比较表达式 → 将 cond 作为数值判断 → `emit('j!=', place, '0', trueLabel)`，否则跳至 falseLabel。

**答辩加分点**：我们对条件表达式既支持比较表达式，也支持一般表达式的真假判断，保证语言特性的完整性。

### 7）输出（10 秒）
IR.generate 最终返回 `List<Quad>`，并将 Generator 收集的 IR 错误追加到 outErrors 中，确保 GUI 能完整展示所有错误信息。

---

## 二、IR 备注对照图（Notes）
### 8）为什么要 Facade + Core（20 秒）
IR.generate 是 Facade 层，Generator 是 Core 核心层；
优势：调用方使用简单，核心算法独立解耦（可单独测试、可替换），便于后续替换为更复杂的 IR 生成逻辑。

### 9）为什么四元式 Quad 是关键结构（20 秒）
Quad 结构固定为 (op, arg1, arg2, result)；
价值：后续优化（DAG/常量折叠）或目标代码生成仅需遍历 Quad 列表，无需回溯 AST，降低后续流程的复杂度。

### 10）newTemp / newLabel 的意义（20 秒）
- newTemp：保证表达式分步计算，每一步结果可映射到寄存器/内存地址；
- newLabel：明确标识控制流结构的入口，后续翻译成汇编/伪汇编时更直接。


# 4.4 优化阶段 答辩讲解稿
各位老师好，接下来我为大家讲解 4.4 优化阶段的设计与实现。

我们的优化阶段核心目标是：**输入 IR 四元式列表 List<Quad>，输出语义等价但更短、更精炼的 List<Quad>**，为后续目标代码生成减少指令数量、提升执行效率。

## 1. 优化入口：多轮迭代的门面设计
优化的入口是 `Optimizer.optimize（Optimizer.java）`，作为门面类提供统一的优化接口。
这里的核心设计是 **“多轮迭代直到收敛”**（默认最多 5 轮）：
- 单轮优化完成后，可能产生新的可优化机会（比如常量折叠简化表达式后，会出现未使用的临时变量，需后续 DCE 消除）；
- 我们通过 `equalsQuads` 判断优化前后的四元式列表是否一致（达到稳定状态），若稳定则提前停止迭代，既保证优化效果，又避免无意义的循环。

## 2. 单遍优化：基于基本块的安全策略
门面类内部会调用 `OptimizationPass.run（OptimizationPass.java）` 完成“单遍优化”，核心策略是 **按 label/jump 切分基本块**：
- 切分规则：遇到 label 或 jump 时，立即 `flush`（处理）当前基本块；
- 设计原因：死代码消除若跨越控制流边界执行，极易误删未来路径会用到的值，将优化限制在基本块内部，是兼顾效果与安全性的设计。

## 3. 块内优化：两步式精简逻辑
每个基本块在 `flushBlock` 方法中按固定顺序执行两步核心优化：
### （1）simplify：四元式化简
对块内每条四元式做针对性精简，核心包含三类优化：
- 常量折叠：数字 op 数字直接计算出常量，替换为 `:= 常量` 四元式；
- 代数化简：处理 `x+0`、`x*1`、`x*0`、`x/1` 等冗余运算；
- neg 常量折叠：`neg(数字)` 直接替换为 `:= -数字`；
- 效果：显著减少运行时需要执行的运算量。

### （2）eliminateDeadTemps：临时变量死代码消除
针对临时变量（t*）做精准的死代码删除，算法逻辑如下：
- 从后往前扫描块内四元式，维护 `used` 集合记录后续会用到的临时变量；
- 删除规则：若某条语句定义了临时变量作为结果，但该变量未被 `used` 集合包含，则直接删除该语句；
- 安全保障：仅删除临时变量相关的冗余语句，不改动用户变量赋值，且 label/jump（控制流边界）永远保留。

## 4. 优化输出与价值
最终输出的 Quad 列表具备两个核心特点：
- 语义等价：完全保留原程序的业务逻辑，无功能变更；
- 更短、更少 temp：减少了冗余运算和无效临时变量定义，为后续目标代码生成减少指令数量，直接提升程序执行效率。