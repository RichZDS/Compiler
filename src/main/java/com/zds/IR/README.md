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
