package edu.ufl.cise.plcsp23;
import edu.ufl.cise.plcsp23.ast.*;
import edu.ufl.cise.plcsp23.ast.NumLitExpr;

import java.util.List;

public class Parser implements IParser  {
    Scanner scanner;
    private int current = 0;

    Parser(Scanner input) {
        scanner = input;
    }
    public AST parse() throws  PLCException {
        Token t = (Token) scanner.next();
        Token current = t;
        NumLitExpr nxp = new NumLitExpr(current);
        return nxp;
    }
}
