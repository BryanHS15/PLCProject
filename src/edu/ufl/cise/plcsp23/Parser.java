package edu.ufl.cise.plcsp23;
import edu.ufl.cise.plcsp23.ast.*;
import edu.ufl.cise.plcsp23.ast.NumLitExpr;

import java.util.ArrayList;
import java.util.List;

public class Parser implements IParser  {
    public Scanner scanner;
    private int currentIndex = 0;
    public ArrayList<Token> tokenList;

    public AST parse() throws PLCException {
        char[] tokens = scanner.inputChars;
        Token nextToken = (Token) scanner.next();
        Token current = nextToken;
        NumLitExpr nxp = new NumLitExpr(current);
        return nxp;
    }

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

    private Expr expression() throws PLCException{

        if(tokenList.isEmpty()){
            throw new PLCException("Error in expression");
        }
        if(match(IToken.Kind.RES_if)){
            return conditional_expr();
        }
        else{
            return or_expr();
        }

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

    private boolean matchHelper(IToken.Kind... types) {
        for (IToken.Kind type : types) {
            if (check(type)) {
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

    private Token consume (IToken.Kind kind) throws SyntaxException{
        if(check(kind)){
            return advance();
        }
        throw new SyntaxException("Consume error");

    }

    // <conditional_expr>  ::= if <expr> ? <expr> ? <expr>
    private Expr conditional_expr() throws PLCException{
       Expr expr = expression();
       IToken first = previous();
        consume(IToken.Kind.QUESTION);
       Expr true_Expr = expression();
       consume(IToken.Kind.QUESTION);
       Expr false_Expr = expression();
       Expr left_expr = new ConditionalExpr(first, expr, true_Expr, false_Expr);

       return left_expr;
    }

    //<and_expr> ::=  <comparison_expr> ( ( & | && )  <comparison_expr>)*
    private Expr and_expr() throws PLCException{
        Expr left_expr = additive_expr();
        IToken first_token = peek();

        while(match(IToken.Kind.AND, IToken.Kind.BITAND)) {
            Token operator = previous();
            Expr right_expr = power_expr();
            left_expr = new BinaryExpr(first_token, left_expr, operator.getKind(), right_expr);
        }
        return left_expr;

    }

    // <or_expr> ::=  <and_expr> (  ( | | || ) <and_expr>)*

    private Expr or_expr() throws PLCException{
        Expr left_expr = and_expr();
        IToken first_token = peek();

        while(match(IToken.Kind.OR, IToken.Kind.BITOR)) {
            Token operator = previous();
            Expr right_expr = power_expr();
            left_expr = new BinaryExpr(first_token, left_expr, operator.getKind(), right_expr);
        }
        return left_expr;

    }

    //<power_expr> ::=    <additive_expr> ** <power_expr> |  <additive_expr>
    private Expr power_expr() throws PLCException{
       Expr left_expr = additive_expr();
       IToken first_token = peek();

       while(match(IToken.Kind.EXP)) {
           Token operator = previous();
           Expr right_expr = power_expr();
           left_expr = new BinaryExpr(first_token, left_expr, operator.getKind(), right_expr);
       }
       return left_expr;

    }


    //<comparison_expr> ::=   <power_expr> ( (< | > | == | <= | >=) <power_expr>)*
    private Expr comparison() throws PLCException{
        Expr left_expr = power_expr();
        Token first_token = peek();

        while (match(IToken.Kind.GT, IToken.Kind.GE, IToken.Kind.LT, IToken.Kind.LE)) {
            Token operator = previous();
            Expr right_expr = power_expr();
            left_expr = new BinaryExpr(first_token,left_expr, operator.getKind(), right_expr);
        }

        return left_expr;
    }


    //<additive_expr> ::=  <multiplicative_expr> ( ( + | - ) <multiplicative_expr> )*
    private Expr additive_expr() throws PLCException{
        Expr left_expr = multiplicative_expr();
        Token first_token = peek();

        while (match(IToken.Kind.MINUS, IToken.Kind.PLUS)) {
            Token operator = previous();
            Expr right_expr = additive_expr();
            left_expr = new BinaryExpr(first_token, left_expr, operator.getKind(), right_expr);
        }

        return left_expr;
    }


    //<multiplicative_expr> ::= <unary_expr> (( * | / | % ) <unary_expr>)*
    private Expr multiplicative_expr() throws PLCException{
        Expr left_expr = unary();
        Token first_token = peek();
        while (match(IToken.Kind.DIV, IToken.Kind.TIMES, IToken.Kind.MOD)) {
            Token operator = previous();
            Expr right_expr = unary();
            left_expr = new BinaryExpr(first_token, left_expr, operator.getKind(), right_expr);
        }

        return left_expr;
    }

//<unary_expr> ::= ( ! | - | sin | cos | atan) <unary_expr> |   <primary_expr>
    private Expr unary() throws PLCException {
        if (match(IToken.Kind.BANG, IToken.Kind.MINUS, IToken.Kind.RES_atan, IToken.Kind.RES_cos, IToken.Kind.RES_sin)) {
            Token first = peek();
            Token operator = previous();
            Expr right = unary();
            return new UnaryExpr(first, operator.getKind(), right);
        }

        return primary();
    }

    //<primary_expr> ::= STRING_LIT |NUM_LIT |IDENT |( <expr> ) |Z rand
    private Expr primary() throws PLCException{
        Token first_token = peek();
        Expr left_expr = expression();

        if (match(IToken.Kind.STRING_LIT)) return new StringLitExpr(first_token);
        else if (match(IToken.Kind.NUM_LIT)) return new NumLitExpr(first_token);
        else if (match(IToken.Kind.IDENT)) return new IdentExpr(first_token);
        else  if (match(IToken.Kind.LPAREN)) {

            consume(IToken.Kind.RPAREN);
            return left_expr;
        }
        else if(match(IToken.Kind.RES_Z)) return new ZExpr(first_token);
        else if(match(IToken.Kind.RES_rand)) return new RandomExpr(first_token);
        throw new PLCException("Error in Primary");

    }





}
