package edu.ufl.cise.plcsp23;
import edu.ufl.cise.plcsp23.ast.*;
import edu.ufl.cise.plcsp23.ast.NumLitExpr;
import edu.ufl.cise.plcsp23.IToken.Kind;
import edu.ufl.cise.plcsp23.ast.BinaryExpr;
import edu.ufl.cise.plcsp23.ast.UnaryExpr;
import edu.ufl.cise.plcsp23.ast.StringLitExpr;
import org.hamcrest.generator.FactoryMethod;

import javax.management.relation.InvalidRoleInfoException;
import javax.naming.Name;
import javax.swing.plaf.nimbus.State;
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

        Ident i =  ident();
       if(match(Kind.LPAREN)){
            para = parameterList();
           if(match(Kind.RPAREN)){
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

    private Block block() throws PLCException{
       IToken first = peek();

       if(match(Kind.LCURLY)){
           dec = declarationList();
           statements = statementList();
           if(match(Kind.RCURLY)){
               return new Block(first, dec, statements);
           }
           else{
               throw new SyntaxException("No curly");
           }
       }
       else{
           throw new SyntaxException("No curly");
       }
    }

    private List<Statement> statementList() throws PLCException{
        IToken first = peek();
        if(first.getKind() == Kind.RCURLY){
            return new ArrayList<Statement>();
        }
        else{
            Statement statement = statement();
            statements.add(statement);
            while(match(Kind.DOT)){
                statement = statement();
                if(statement != null){
                    statements.add(statement);
                }
            }
            return statements;
        }

    }

    private Statement statement() throws PLCException{
        IToken first = peek();


        if(match(Kind.RES_write)){
            Expr expr = expression();
            return new WriteStatement(first, expr);
        }
        else if (match(Kind.RES_while)){
            Expr expr = expression();
            Block b = block();
            return new WhileStatement(first, expr, b);
        }
        else{
            LValue val = lValue();
            consume(Kind.ASSIGN);
            Expr expr = expression();
            return new AssignmentStatement(first, val , expr);
        }
    }

    private List<NameDef> parameterList() throws PLCException{
        IToken first = peek();
        if(first.getKind() == Kind.RPAREN){
            return new ArrayList<NameDef>();
        }
        else{
            NameDef nameDef = nameDef();
            para.add(nameDef);
            while(match(Kind.COMMA)){
                nameDef = nameDef();
                para.add(nameDef);
            }
            return  para;
        }

    }

    private List<Declaration> declarationList() throws PLCException{
        IToken first = peek();
        if(first.getKind() == Kind.RCURLY){
            return new ArrayList<Declaration>();
        }
        else{
           Declaration declaration = declaration();
            dec.add(declaration);
            while(match(Kind.DOT)){
                declaration = declaration();
                if(declaration != null){
                    dec.add(declaration);
                }
            }
            return dec;
        }

    }
    private Declaration declaration() throws PLCException{
        IToken first = peek();
        NameDef nameDef = nameDef();
        advance();
        if(match(Kind.ASSIGN)){
            if(peek() != null)
                return new Declaration(first, nameDef, expression());
            else{
                return new Declaration(first, nameDef, null);
            }
        }
        else{
            throw new SyntaxException("No Equal sign");
        }
    }

    private NameDef nameDef() throws PLCException{
        IToken first = peek();
        Type t = type();

        if(peek().getKind() == Kind.IDENT){
            Ident ident = new Ident(peek());
            return new NameDef(first, t, null, ident);
        }
        else{
            Dimension d = dimension();
            Ident ident = new Ident(peek());
            return new NameDef(first, t, d, ident);

        }
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
                advance();
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

    private LValue lValue() throws PLCException{
        IToken first = peek();


        if(first.getKind() == Kind.IDENT){
            Ident ident = new Ident(first);

            if(match(Kind.LSQUARE)){
                PixelSelector p = pixelSelector();
                if(match(Kind.COLON)){
                    IToken current = peek();
                    ColorChannel c = ColorChannel.getColor(current);
                    return new LValue(first, ident,p, c );
                }
                else{
                    return new LValue(first, ident, p, null);
                }
            }
            else{
               return new LValue(first, ident, null, null);
            }
        }

        else{
            throw new SyntaxException("No Ident");
        }
    }

    private PixelSelector pixelSelector() throws PLCException{
        IToken first = peek();
        Expr temp1 = null;
        Expr temp2 = null;

        if(match(Kind.LSQUARE)){
            temp1 = expression();
            consume(Kind.COMMA);
            temp2 = expression();
            if(match(Kind.RSQUARE)){
                advance();
                return new PixelSelector(first, temp1, temp2);
            }
            else{
                throw new SyntaxException("No parenthesis");
            }
        }
        else{
            throw new SyntaxException("No parenthesis");
        }

    }

   private ExpandedPixelExpr expandedPixelExpr() throws PLCException{
       IToken first = previous();
       Expr temp1 = null;
       Expr temp2 = null;
       Expr temp3 = null;

       if(match(Kind.LSQUARE)){
           temp1 = expression();
           consume(Kind.COMMA);
           temp2 = expression();
           consume(Kind.COMMA);
           temp3 = expression();
           if(match(Kind.RSQUARE)){
               advance();
               return new ExpandedPixelExpr(first, temp1, temp2,temp3);
           }
           else{
               throw new SyntaxException("No Bracket");
           }
       }
       else{
           throw new SyntaxException("No Bracket");
       }


   }

   private PixelFuncExpr pixelFuncExpr() throws PLCException{
        IToken first = peek();
        PixelSelector pSelector = pixelSelector();
        return new PixelFuncExpr(first,first.getKind(), pSelector);



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

    private UnaryExprPostfix unaryExprPostfix() throws PLCException{
        IToken first = peek();
        Expr primary = primary();

        if(match(Kind.LSQUARE)){

            PixelSelector p = pixelSelector();
            if(match(Kind.COLON)){
                IToken current = peek();
                ColorChannel c = ColorChannel.getColor(current);
                return new UnaryExprPostfix(first,primary, p, c);
            }
            else{
                return new UnaryExprPostfix(first, primary, p, null);
            }
        }
        else{
            return new UnaryExprPostfix(first, primary, null,null);
        }
    }

//<unary_expr> ::= ( ! | - | sin | cos | atan) <unary_expr> |   <primary_expr>
    private Expr unary() throws PLCException {
            IToken first = peek();

        if (match(IToken.Kind.BANG, IToken.Kind.MINUS, IToken.Kind.RES_atan, IToken.Kind.RES_cos, IToken.Kind.RES_sin)) {

            IToken operator = previous();
            Expr right_expr = unary();
            return new UnaryExpr(operator, operator.getKind(), right_expr);
        }

        return unaryExprPostfix();
    }

    //<primary_expr> ::= STRING_LIT |NUM_LIT |IDENT |( <expr> ) |Z rand
    private Expr primary() throws PLCException{

        if (match(IToken.Kind.STRING_LIT)) return new StringLitExpr(previous());
        else if (match(IToken.Kind.NUM_LIT)) return new NumLitExpr(previous());
        else if (match(IToken.Kind.IDENT)) return new IdentExpr(previous());
        else if(match(IToken.Kind.RES_Z)) return new ZExpr(previous());
        else if(match(IToken.Kind.RES_rand)) return new RandomExpr(previous());
        else if(match(Kind.RES_r, Kind.RES_a, Kind. RES_x, Kind.RES_y)) return new PredeclaredVarExpr(previous());
        else if(tokenList.get(currentIndex).getKind() == Kind.LSQUARE ) return expandedPixelExpr();
        else if(match(Kind.RES_x_cart, Kind.RES_y_cart, Kind.RES_a_polar, Kind.RES_r_polar)) return pixelFuncExpr();
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
