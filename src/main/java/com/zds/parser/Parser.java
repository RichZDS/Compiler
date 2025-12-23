package com.zds.parser;

import com.zds.lexer.Token;
import com.zds.parser.AST.Program;

import java.util.ArrayList;
import java.util.List;

public class Parser {
    public static Program analyze(List<Token> tokens, List<String> outErrors) {
        List<String> errors = (outErrors != null) ? outErrors : new ArrayList<>();
        ErrorHandling err = new ErrorHandling();
        AST.Factory ast = new AST.Factory();
        Recognizer recognizer = new Recognizer(tokens, errors, err, ast);
        return recognizer.parseProgram();
    }
}
