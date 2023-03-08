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

        throw new SyntaxException("Inconsumable Token");
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
        return program();
    }

    private Program program() throws PLCException {
       IToken first = peek();
       Type t = type();
       Ident i =  ident();


       if(match(Kind.LPAREN)){
            List<NameDef> parameterList = parameterList();
           if(match(Kind.RPAREN)){
               Block block = block();
               return new Program(first,t, i, parameterList, block);
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

        try{
            Type t = Type.getType(tokenList.get(currentIndex));
            advance();
            return t;
        }
        catch(RuntimeException e){
            throw new SyntaxException("Incorrect type");
        }
    }

    private Ident ident() throws SyntaxException{
       IToken t = tokenList.get(currentIndex);

       if(t.getKind() != Kind.IDENT){
           throw new SyntaxException("No Indet");
       }
       else{
           return new Ident(advance());
       }

    }

    private Block block() throws PLCException{
       IToken first = peek();

       if(match(Kind.LCURLY)){
           List<Declaration> dec = declarationList();
           List<Statement> statements = statementList();
           if(match(Kind.RCURLY)){
               return new Block(first, dec, statements);
           }
           else{
               throw new SyntaxException("No curly");
           }
       }
       else{
           throw new SyntaxException("Error 2");
       }
    }

    private List<Statement> statementList() throws PLCException{
       List<Statement> statements = new ArrayList<Statement>();
        IToken k = tokenList.get(currentIndex);

        while(k.getKind() == Kind.RES_write || k.getKind() == Kind.IDENT || k.getKind() == Kind.RES_while){

            Statement statement = statement();
            if(statement != null){
                statements.add(statement);
            }
            consume(Kind.DOT);
        }
        return statements;
    }

    private Statement statement() throws PLCException{
        IToken first = peek();
        IToken k = tokenList.get(currentIndex);

        if(k.getKind() == Kind.RCURLY){
            return null;
        }
        else if(match(Kind.RES_write)){
            Expr e = expression();
            return new WriteStatement(first, e);
        }
        else if(match(Kind.RES_while)){
            Expr e = expression();
            Block b = block();
            return new WhileStatement(first, e, b);
        }
        else{
            LValue lValue = lValue();
            consume(Kind.ASSIGN);
            Expr e = expression();
            return new AssignmentStatement(first, lValue,e);
        }


    }

    private List<NameDef> parameterList() throws PLCException{
        List<NameDef> parameterList = new ArrayList<NameDef>();
        IToken k = tokenList.get(currentIndex);

        if(k.getKind() == Kind.RES_image || k.getKind() == Kind.RES_pixel || k.getKind() == Kind.RES_int||
                k.getKind() == Kind.RES_string || k.getKind() == Kind.RES_void){
            NameDef nameDef = nameDef();
            parameterList.add(nameDef);
            while (match(Kind.COMMA)){
                nameDef = nameDef();
                parameterList.add(nameDef);
            }
        }
            return parameterList;
    }

    private List<Declaration> declarationList() throws PLCException{

        List<Declaration> declarationList = new ArrayList<Declaration>();
        IToken k = tokenList.get(currentIndex);

        while(k.getKind() == Kind.RES_image || k.getKind() == Kind.RES_pixel || k.getKind() == Kind.RES_int||
        k.getKind() == Kind.RES_string || k.getKind() == Kind.RES_void){

            Declaration declaration = declaration();
            if(declaration != null){
                declarationList.add(declaration);
            }
            consume(Kind.DOT);
        }
        return declarationList;

    }
    private Declaration declaration() throws PLCException{
        IToken first = peek();
        NameDef nameDef = nameDef();
        IToken k = tokenList.get(currentIndex);

        if(match(Kind.ASSIGN)){
           Expr e = expression();
           return new Declaration(first, nameDef, e);
        }
        else if(k.getKind() == Kind.DOT){
            return new Declaration(first, nameDef, null);
        }
        else{
            return null;
        }
    }

    private NameDef nameDef() throws PLCException{
        IToken first = peek();
        Type type = type();
        IToken k = tokenList.get(currentIndex);

        if(k.getKind() == Kind.IDENT){
            Ident ident = ident();
            return new NameDef(first, type, null, ident);
        }
        else{
            Dimension dimension = dimension();
            Ident ident = ident();
            return new NameDef(first, type, dimension, ident);
        }

    }


    private Dimension dimension() throws PLCException {
        IToken first = peek();


        if(match(Kind.LSQUARE)){
            Expr temp1 = expression();
            consume(Kind.COMMA);
            Expr temp2 = expression();
            if(match(Kind.RSQUARE)){

                return new Dimension(first, temp1, temp2);
            }
            else{
                throw new SyntaxException("No RSquare");
            }
        }
        else{
            return null;
        }

    }

    private LValue lValue() throws PLCException{
        IToken first = peek();
        Ident i = ident();
        IToken k = tokenList.get(currentIndex);

        if(k.getKind() == Kind.ASSIGN){
            return new LValue(first, i, null,null);
        }
        else{
            PixelSelector p = pixelSelector();
            if(match(Kind.COLON)){
                ColorChannel c = channelSelector();
                return new LValue(first, i, p, c);
            }
            else{
                return new LValue(first, i, p, null);
            }
        }

    }

    private ColorChannel channelSelector() throws PLCException{

       try{
           ColorChannel c = ColorChannel.getColor(tokenList.get(currentIndex));
           advance();
           return c;
       }
       catch(RuntimeException e){
           throw new SyntaxException("Incorrect Color");
        }

    }

    private PixelSelector pixelSelector() throws PLCException{
        IToken first = peek();

        if(match(Kind.LSQUARE)){
            Expr temp1 = expression();
            consume(Kind.COMMA);
            Expr temp2 = expression();

            if(match(Kind.RSQUARE)){
                return new PixelSelector(first, temp1, temp2);
            }
            else{
                throw new SyntaxException("No RSquare");
            }
        }
        else{
            throw new SyntaxException("Error 3");
        }
    }

   private ExpandedPixelExpr expandedPixelExpr() throws PLCException{
       IToken first = previous();

       if(match(Kind.LSQUARE)){
           Expr temp1 = expression();
           consume(Kind.COMMA);
           Expr temp2 = expression();
           consume(Kind.COMMA);
           Expr temp3 = expression();
           if(match(Kind.RSQUARE)){
               return new ExpandedPixelExpr(first, temp1, temp2,temp3);
           }
           else{
               throw new SyntaxException("No RSquare");
           }
       }
       else{
           throw new SyntaxException("Error 5");
       }


   }

   private PixelFuncExpr pixelFuncExpr() throws PLCException{
        IToken first = peek();
        advance();

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
            IToken k = tokenList.get(currentIndex);

        if (match(IToken.Kind.BANG, IToken.Kind.MINUS, IToken.Kind.RES_atan, IToken.Kind.RES_cos, IToken.Kind.RES_sin)) {

            IToken operator = previous();
            Expr right_expr = unary();
            return new UnaryExpr(operator, operator.getKind(), right_expr);
        }
        else{
            Expr p = primary();

            if(k.getKind() == Kind.LSQUARE){
                PixelSelector ps = pixelSelector();

                if(match(Kind.COLON)){
                    ColorChannel c = channelSelector();
                    return new UnaryExprPostfix(first, p, ps, c);
                }
                else{
                    return new UnaryExprPostfix(first, p, ps, null);
                }
            }
            else{
                return p;
            }
        }
    }

    //<primary_expr> ::= STRING_LIT |NUM_LIT |IDENT |( <expr> ) |Z rand
    private Expr primary() throws PLCException{

        IToken k = tokenList.get(currentIndex);

        if (match(IToken.Kind.STRING_LIT)) return new StringLitExpr(previous());
        else if (match(IToken.Kind.NUM_LIT)) return new NumLitExpr(previous());
        else if (match(IToken.Kind.IDENT)) return new IdentExpr(previous());
        else if(match(IToken.Kind.RES_Z)) return new ZExpr(previous());
        else if(match(IToken.Kind.RES_rand)) return new RandomExpr(previous());
        else if(match(Kind.RES_r, Kind.RES_a, Kind. RES_x, Kind.RES_y)) return new PredeclaredVarExpr(previous());
        else if(k.getKind() == Kind.LSQUARE ) return expandedPixelExpr();
        else if(k.getKind() == Kind.RES_x_cart || k.getKind() == Kind.RES_y_cart || k.getKind() ==  Kind.RES_a_polar||
                k.getKind() == Kind.RES_r_polar) return pixelFuncExpr();
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
