我将重构代码库以强制执行严格的封装，确保每个包（`main` 除外）只暴露 **一个公共接口类**，所有数据类型和辅助逻辑都嵌套在该类中或作为包级私有实现细节隐藏。

### 1. 重构 `com.zds.lexer`
- **目标**：`Lexer` 是唯一的公共类。
- **变更**：
    - 将 `Token` 类和 `TokenType` 枚举移入 `Lexer` 作为 `public static` 嵌套成员（`Lexer.Token`, `Lexer.TokenType`）。
    - 将 `LexerUtils` 逻辑移入 `Lexer`（或使其成为同包下的包级私有辅助类）。
    - 删除 `Token.java` 和 `TokenType.java`。
    - 更新 `Lexer.java` 以暴露 `scanTokens()` 作为主要入口点。

### 2. 重构 `com.zds.parser`
- **目标**：`Parser` 是唯一的公共类。
- **变更**：
    - 将所有 `AST` 类（`Program`, `Stmt`, `Expr` 等）移入 `Parser` 作为 `public static` 嵌套类（例如 `Parser.Program`）。
    - 将 `parse()` 方法移至 `Parser`。
    - 将 `Recognizer` 和 `ErrorHandling` 降级为包级私有（去掉 `public` 修饰符）。
    - 删除 `AST.java`（因为其内容移至 `Parser`）。

### 3. 重构 `com.zds.Semantic`
- **目标**：`Semantic` 是唯一的公共类。
- **状态**：已符合要求（`Result`, `Type`, `Scope` 均为内部类）。
- **变更**：更新导入以使用 `Parser.Program` 和 `Lexer.Token`。

### 4. 重构 `com.zds.IR`
- **目标**：`IR` 是唯一的公共类。
- **状态**：已符合要求（`Quad` 为内部类）。
- **变更**：更新导入以使用 `Parser.Program` 和 `Semantic.Result`。

### 5. 重构 `com.zds.optimizer`
- **目标**：`Optimizer` 是唯一的公共类。
- **状态**：已符合要求。
- **变更**：更新导入以使用 `IR.Quad`。

### 6. 重构 `com.zds.codegen`
- **目标**：`CodeGen` 是唯一的公共类。
- **变更**：
    - 将 `CodeGenPseudoAsm` 重命名/重构为 `CodeGen`。
    - 将 `AsmInstr` 移入 `CodeGen` 作为 `public static class Instr`。
    - 将 `Asm` 和 `AsmPrinter` 降级为包级私有。
    - 删除 `AsmInstr.java`。

### 7. 重构 `com.zds.service`
- **目标**：`CompilerService` 是唯一的公共类。
- **变更**：
    - 将 `CompilationArtifacts` 移入 `CompilerService` 作为 `public static class Artifacts`。
    - 删除 `CompilationArtifacts.java`。
    - 更新所有逻辑以使用其他包的新嵌套类型。

### 8. 更新 `com.zds.main`
- **变更**：更新 `Main.java` 和 `CompilerGui.java` 以使用新的包接口（例如 `Lexer.Token`, `Parser.Program`, `CompilerService.Artifacts`）。

我将按照依赖顺序（Lexer -> Parser -> ... -> Service -> Main）执行这些更改，以保持代码库的稳定性。