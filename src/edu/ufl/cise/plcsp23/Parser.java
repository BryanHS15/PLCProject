package edu.ufl.cise.plcsp23;
import edu.ufl.cise.plcsp23.ast.*;
import edu.ufl.cise.plcsp23.ast.NumLitExpr;

import java.util.ArrayList;
import java.util.List;

public class Parser implements IParser  {
    public Scanner scanner;
    private int currentIndex = 0;
    public ArrayList<Token> tokenList;



   public Parser(Scanner input) throws LexicalException{
        this.scanner = input;
        this.currentIndex = 0;
        this.tokenList = new ArrayList<>();
        getNextToken();

    }

    private void getNextToken() throws LexicalException{
        Token token = (Token) scanner.next();
        tokenList.add(token);

    }


    private Expr equality() {
        Expr expr = comparison();
        int i = 0;
        while (match(IToken.Kind.EQ)) {
            Token operator = previous();
            Expr right = comparison();
            expr = new BinaryExpr(tokenList.get(i),expr, operator.getKind(), right);
            ++i;
        }


        return expr;
    }

    private boolean match(IToken.Kind... types) {
        for (IToken.Kind type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }

        return false;
    }

    private boolean check(IToken.Kind type) {
        if (isAtEnd()) return false;
        return peek().kind == type;
    }


    private Token advance() {
        if (!isAtEnd()) currentIndex++;
        return previous();
    }

    private boolean isAtEnd() {
        return peek().kind == IToken.Kind.EOF;
    }

    private Token peek() {
        return tokenList.get(currentIndex);
    }

    private Token previous() {
        return tokenList.get(currentIndex - 1);
    }

    private Expr comparison() {
        Expr expr = term();

        while (match(IToken.Kind.GT, IToken.Kind.GE, IToken.Kind.LT, IToken.Kind.LE)) {
            Token operator = previous();
            Expr right = term();
            expr = new BinaryExpr(expr, operator.getKind(), right);
        }

        return expr;
    }


    private Expr term() {
        Expr expr = factor();

        while (match(IToken.Kind.MINUS, IToken.Kind.PLUS)) {
            Token operator = previous();
            Expr right = factor();
            expr = new BinaryExpr(expr, operator, right);
        }

        return expr;
    }

    private Expr multiplicative_expr() {
        Expr expr = unary();
        Token left = peek();
        while (match(IToken.Kind.DIV, IToken.Kind.TIMES, IToken.Kind.MOD)) {
            Token operator = previous();
            Expr right = unary();
            expr = new BinaryExpr(expr,operator, right);
        }

        return expr;
    }


    private Expr unary() {
        if (match(IToken.Kind.BANG, IToken.Kind.MINUS, IToken.Kind.RES_atan, IToken.Kind.RES_cos, IToken.Kind.RES_sin)) {
            Token first = peek();
            Token operator = previous();
            Expr right = unary();
            return new UnaryExpr(first, operator.getKind(), right);
        }

        return primary();
    }

    private Expr primary() {
        if (match(FALSE)) return new Expr.Literal(false);
        if (match(TRUE)) return new Expr.Literal(true);
        if (match(NIL)) return new Expr.Literal(null);

        if (match(IToken.Kind.NUM_LIT, IToken.Kind.STRING_LIT)) {
            return new Expr.Literal(previous().literal);
        }

        if (match(IToken.Kind.LPAREN)) {
            Expr expr = expression();
            consume(IToken.Kind.RPAREN, "Expect ')' after expression.");
            return new Expr.Grouping(expr);
        }
    }




    public AST parse() throws PLCException {
        char[] tokens = scanner.inputChars;
        Token nextToken = (Token) scanner.next();
        Token current = nextToken;
        NumLitExpr nxp = new NumLitExpr(current);
        return nxp;
    }
}
