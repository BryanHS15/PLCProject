package edu.ufl.cise.plcsp23;
import edu.ufl.cise.plcsp23.ast.*;
import edu.ufl.cise.plcsp23.ast.NumLitExpr;

import java.util.List;

public class Parser implements IParser  {
    Scanner scanner;
    private int currentIndex = 0;


    private Expr expression() {
        return equality();
    }
    private Expr equality() {
        Expr expr = comparison();

        while (match("!=", "==")) {
            Token operator = previous();
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }

        return false;
    }

    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().type == type;
    }
    Parser(Scanner input) {
        scanner = input;
    }
    public AST parse() throws PLCException {
        char[] tokens = scanner.inputChars;
        Token nextToken = (Token) scanner.next();
        Token current = nextToken;
        NumLitExpr nxp = new NumLitExpr(current);
        return nxp;
    }
}
