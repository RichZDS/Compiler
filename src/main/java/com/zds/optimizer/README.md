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
