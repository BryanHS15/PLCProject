package edu.ufl.cise.plcsp23;
import edu.ufl.cise.plcsp23.ast.*;
import edu.ufl.cise.plcsp23.ast.NumLitExpr;
import edu.ufl.cise.plcsp23.IToken.Kind;
import edu.ufl.cise.plcsp23.ast.BinaryExpr;
import edu.ufl.cise.plcsp23.ast.UnaryExpr;
import edu.ufl.cise.plcsp23.ast.StringLitExpr;

import javax.management.relation.InvalidRoleInfoException;
import javax.swing.plaf.synth.SynthButtonUI;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;

public class Parser implements IParser  {
     Scanner scanner;
    private int currentIndex = 0;
    private ArrayList<IToken> tokenList;



   public Parser(Scanner input) throws LexicalException {
        this.scanner = input;
        this.tokenList = new ArrayList<IToken>();

        while(true){
            try{
                IToken token = scanner.next();
                if(token.getKind() == IToken.Kind.EOF){
                    this.tokenList.add(token);
                    break;
                }
                    this.tokenList.add(token);
            }
            catch(LexicalException e){
                throw new  LexicalException("error");
            }

        }

    }

    private List<Declaration> dec = new ArrayList<Declaration>();
    private List<NameDef> para = new ArrayList<NameDef>();
    private List<Statement> statements = new ArrayList<Statement>();

    private boolean isAtEnd() {

        return peek().getKind() == IToken.Kind.EOF;
    }

    private Token peek() {
        return (Token) tokenList.get(currentIndex);
    }

    private Token previous() {
        return (Token) tokenList.get(currentIndex - 1);
    }

    private Token advance() {
        if (!isAtEnd()) currentIndex++;
        return previous();
    }

    private IToken consume(Kind type) throws PLCException{
        if (check(type))
            return advance();

        throw new PLCException("Inconsumable Token");
    }

    private boolean check(IToken.Kind type) {
        if (isAtEnd()) return false;
        return peek().getKind() == type;
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


    public AST parse() throws PLCException {
        if(tokenList.size() == 1){
            throw new SyntaxException("The Array is empty");
        }
        return expression();
    }

    private Program program() throws PLCException {
       IToken first = peek();
       Type t = type();

        Ident i =  new Ident(consume(first.getKind()));
       if(match(Kind.LPAREN)){
           List<NameDef> para = parameterList();
           if(match(Kind.RPAREN)){
               Token rParen = previous();
               Block block = block();
               return new Program(first,t, i, para, block);
           }
           else{
               throw new SyntaxException("No parenthesis");
           }
       }
       else{
           throw new SyntaxException("Error");
       }
    }

    private Type type() throws PLCException {
        IToken first = peek();
        Type t;
        try{
            t = Type.getType(first);
            advance();
        }
        catch (RuntimeException e){
            throw new SyntaxException("Incorrect type");
        }

        return t;
    }

    private Ident ident() throws SyntaxException{
       Token temp = advance();

       if(temp.kind != Kind.IDENT){
           throw new SyntaxException("Does not have type");
       }
       else{
           return new Ident(temp);
       }

    }

    private Block block() throws SyntaxException{
       IToken first = peek();

       if(match(Kind.LCURLY)){
           List <Declaration> dec = new ArrayList<Declaration>();
           List <Statement> statement = new ArrayList<Statement>();
           if(match(Kind.RCURLY)){
               Token rCurly = previous();
               return new Block(first, dec, statement);
           }
           else{
               throw new SyntaxException("No parenthesis");
           }
       }
       else{
           throw new SyntaxException("No parenthesis");
       }
    }
    private List<NameDef> parameterList(){
       IToken first = peek();

       if(match(Kind.RPAREN)){
        List <NameDef> para = new ArrayList<NameDef>();

       }
         return null;

    }

    private NameDef nameDef(){
        IToken first = peek();


        return null;


    }


    private Dimension dimension() throws PLCException {
        IToken first = peek();
        Expr temp1 = null;
        Expr temp2 = null;

        if(match(Kind.LSQUARE)){
            temp1 = expression();
            consume(Kind.COMMA);
            temp2 = expression();
            if(match(Kind.RSQUARE)){
                return new Dimension(first, temp1, temp2);
            }
            else{
                throw new SyntaxException("No parenthesis");
            }
        }
        else{
            throw new SyntaxException("No parenthesis");
        }

    }


    private Expr expression() throws PLCException{

        if(match(IToken.Kind.RES_if)){
            return conditional_expr();
        }
        else {
            return or_expr();
        }

    }

    // <conditional_expr>  ::= if <expr> ? <expr> ? <expr>
    private Expr conditional_expr() throws PLCException{
       Expr expr = expression();
       IToken first = previous();

       if(!match(Kind.QUESTION)){
           throw new SyntaxException("Conditional failed");
       }

       Expr true_Expr = expression();
       if(!match(Kind.QUESTION)){
           throw new SyntaxException("Conditional failed");
       }

       Expr false_Expr = expression();

       Expr left_expr = new ConditionalExpr(first, expr, true_Expr, false_Expr);

       return left_expr;
    }

    // <or_expr> ::=  <and_expr> (  ( | | || ) <and_expr>)*

    private Expr or_expr() throws PLCException{
        Expr left_expr = and_expr();
        IToken first_token = peek();

        while(match(IToken.Kind.OR, IToken.Kind.BITOR)) {
            IToken operator = previous();
            Expr right_expr = and_expr();
            left_expr = new BinaryExpr(first_token, left_expr, operator.getKind(), right_expr);
        }
        return left_expr;

    }


    //<and_expr> ::=  <comparison_expr> ( ( & | && )  <comparison_expr>)*
    private Expr and_expr() throws PLCException{
        Expr left_expr = comparison();
        IToken first_token = peek();

        while(match(IToken.Kind.AND, IToken.Kind.BITAND)) {
            IToken operator = previous();
            Expr right_expr = comparison();
            left_expr = new BinaryExpr(first_token, left_expr, operator.getKind(), right_expr);
        }
        return left_expr;

    }


    //<comparison_expr> ::=   <power_expr> ( (< | > | == | <= | >=) <power_expr>)*
    private Expr comparison() throws PLCException{
        Expr left_expr = power_expr();


        while (match(IToken.Kind.GT, IToken.Kind.GE, IToken.Kind.LT, IToken.Kind.LE,Kind.EQ)) {
            IToken operator = previous();
            Expr right_expr = power_expr();
            left_expr = new BinaryExpr(operator,left_expr, operator.getKind(), right_expr);
        }

        return left_expr;
    }


    //<power_expr> ::=    <additive_expr> ** <power_expr> |  <additive_expr>
    private Expr power_expr() throws PLCException{
       Expr left_expr = additive_expr();


       if (match(IToken.Kind.EXP)) {
           IToken operator = previous();
           Expr right_expr = power_expr();
           left_expr = new BinaryExpr(operator, left_expr, operator.getKind(), right_expr);
       }
       return left_expr;

    }



    //<additive_expr> ::=  <multiplicative_expr> ( ( + | - ) <multiplicative_expr> )*
    private Expr additive_expr() throws PLCException{
        Expr left_expr = multiplicative_expr();

        while (match(IToken.Kind.MINUS, IToken.Kind.PLUS)) {
            IToken operator = previous();
            Expr right_expr = multiplicative_expr();
            left_expr = new BinaryExpr(operator, left_expr, operator.getKind(), right_expr);
        }

        return left_expr;
    }


    //<multiplicative_expr> ::= <unary_expr> (( * | / | % ) <unary_expr>)*
    private Expr multiplicative_expr() throws PLCException{
        Expr left_expr = unary();


        while (match(IToken.Kind.DIV, IToken.Kind.TIMES, IToken.Kind.MOD)) {
            IToken operator = previous();
            Expr right_expr = unary();
            left_expr = new BinaryExpr(operator, left_expr, operator.getKind(), right_expr);
        }

        return left_expr;
    }

//<unary_expr> ::= ( ! | - | sin | cos | atan) <unary_expr> |   <primary_expr>
    private Expr unary() throws PLCException {


        if (match(IToken.Kind.BANG, IToken.Kind.MINUS, IToken.Kind.RES_atan, IToken.Kind.RES_cos, IToken.Kind.RES_sin)) {

            IToken operator = previous();
            Expr right_expr = unary();
            return new UnaryExpr(operator, operator.getKind(), right_expr);
        }

        return primary();
    }

    //<primary_expr> ::= STRING_LIT |NUM_LIT |IDENT |( <expr> ) |Z rand
    private Expr primary() throws PLCException{

        if (match(IToken.Kind.STRING_LIT)) return new StringLitExpr(previous());
        else if (match(IToken.Kind.NUM_LIT)) return new NumLitExpr(previous());
        else if (match(IToken.Kind.IDENT)) return new IdentExpr(previous());
        else if(match(IToken.Kind.RES_Z)) return new ZExpr(previous());
        else if(match(IToken.Kind.RES_rand)) return new RandomExpr(previous());
        else if (match(IToken.Kind.LPAREN)) {

           Expr e = expression();
           if(!match(Kind.RPAREN)){
               throw new SyntaxException("Conditional fail for parenthesis");
           }
           return e;
        }

        throw new SyntaxException("Error in Primary");

    }

}
