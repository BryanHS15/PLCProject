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

        Type t = type(); // returns Type.getType(advance())
        Ident i = ident();
        if (match(Kind.LPAREN)) {
            List<NameDef> parameters = parameterList();
            if (match(Kind.RPAREN)) {

                Block b = block();
                return new Program(first, t, i, parameters, b);
            }
            else {
                throw new SyntaxException("No right parentheses");
            }
        }
        else {
            throw new SyntaxException("All are wrong 1");
        }
    }

    private Block block() throws PLCException{
        IToken first = peek();

        if (match(Kind.LCURLY)) {
            List<Declaration> declarations = declarationList();
            List<Statement> statements = statementList();
            if (match(Kind.RCURLY)) {

                return new Block(first, declarations, statements);
            }
            else {
                throw new SyntaxException("No right curly");
            }
        }
        else {
            throw new SyntaxException("All are wrong 2");
        }
    }

    private List<Statement> statementList() throws PLCException{
        List<Statement> statements = new ArrayList<Statement>();

        while (tokenList.get(currentIndex).getKind() == Kind.RES_write || tokenList.get(currentIndex).getKind() == Kind.IDENT ||
                tokenList.get(currentIndex).getKind() == Kind.RES_while) {

            Statement s = statement();
            if (s != null) statements.add(s);

            consume(Kind.DOT);
        }

        return statements;
    }

    private Statement statement() throws PLCException{
        IToken first = peek();
        if (tokenList.get(currentIndex).getKind() == Kind.RCURLY){
            return null;
        }
        else if (match(Kind.RES_write)) {
            Expr expr = expression();
            return new WriteStatement(first, expr);
        }
        else if (match(Kind.RES_while)) {
            Expr expr = expression();
            Block b = block();
            return new WhileStatement(first, expr, b);
        }
        else {
            LValue lv = lValue();
            consume(Kind.ASSIGN);
            Expr expr = expression();

            return new AssignmentStatement(first, lv, expr);
        }

    }




    private LValue lValue() throws PLCException{
        IToken first = peek();

        Ident i = ident();
        if (tokenList.get(currentIndex).getKind() == Kind.ASSIGN) {
            return new LValue(first, i, null, null);
        }
        else {
            PixelSelector ps = pixelSelector();
            if (match(Kind.COLON)) {
                ColorChannel cc = channelSelector();
                return new LValue(first, i, ps, cc);
            }
            else {
                return new LValue(first, i, ps, null);
            }
        }

    }

    private ColorChannel channelSelector() throws PLCException{
        try {
            ColorChannel c = ColorChannel.getColor(tokenList.get(currentIndex));
            advance();
            return c;
        }
        catch(RuntimeException e) {
            throw new SyntaxException("Wrong color");
        }

    }


    private PixelSelector pixelSelector() throws PLCException{
        IToken first = peek();
        if (match(Kind.LSQUARE)) {
            Expr x = expression();
            consume(Kind.COMMA);
            Expr y = expression();
            if (match(Kind.RSQUARE)) {

                IToken rSquare = previous();
                return new PixelSelector(first, x, y);
            }
            else {
                throw new SyntaxException("No right square");
            }
        }
        else {
            throw new SyntaxException("All are wrong 3");
        }
    }



    private List<Declaration> declarationList() throws PLCException{
        List<Declaration> declarations = new ArrayList<Declaration>();


        while (tokenList.get(currentIndex).getKind() == Kind.RES_image || tokenList.get(currentIndex).getKind() == Kind.RES_pixel ||
                tokenList.get(currentIndex).getKind() == Kind.RES_int || tokenList.get(currentIndex).getKind() == Kind.RES_string ||
                tokenList.get(currentIndex).getKind() == Kind.RES_void) {

            Declaration d = declaration();
            if (d != null) declarations.add(d);

            consume(Kind.DOT);


        }

        return declarations;

    }
    private Declaration declaration() throws PLCException{
        IToken first = peek();
        NameDef nd = nameDef();
        if (match(Kind.ASSIGN)) {
            Expr expr = expression();
            return new Declaration(first, nd, expr);
        }
        else if (tokenList.get(currentIndex).getKind() == Kind.DOT) {
            return new Declaration(first,nd,null);
        }
        else {

            return null;
        }
    }


    private Type type() throws PLCException {
        try {
            Type t = Type.getType(tokenList.get(currentIndex));
            advance();
            return t;
        }
        catch(RuntimeException e) {
            throw new SyntaxException("Incorrect Type");
        }
    }


    private Ident ident() throws SyntaxException{
        IToken t = tokenList.get(currentIndex);

        if(t.getKind() != Kind.IDENT){
            throw new SyntaxException("No Indent");
        }
        else{
            return new Ident(advance());
        }

    }

    private List<NameDef> parameterList() throws PLCException{
        List<NameDef> parameters = new ArrayList<NameDef>();

        if ((tokenList.get(currentIndex).getKind() == Kind.RES_image || tokenList.get(currentIndex).getKind() == Kind.RES_pixel ||
                tokenList.get(currentIndex).getKind() == Kind.RES_int || tokenList.get(currentIndex).getKind() == Kind.RES_string ||
                tokenList.get(currentIndex).getKind() == Kind.RES_void)) {
            NameDef nd = nameDef();
            parameters.add(nd);
            while(match(Kind.COMMA)) {
                nd = nameDef();
                parameters.add(nd);
            }
        }

        return parameters;
    }

    private NameDef nameDef() throws PLCException{
        IToken first = peek();
        Type t = type();
        if (tokenList.get(currentIndex).getKind() == Kind.IDENT) {
            Ident i = ident();
            return new NameDef(first, t, null,i);
        }
        else {
            Dimension d = dimension();
            Ident i = ident();
            return new NameDef(first, t, d, i);
        }

    }


    private Dimension dimension() throws PLCException {
        IToken first = peek();

        if (match(Kind.LSQUARE)) {
            Expr width = expression();
            consume(Kind.COMMA);
            Expr height = expression();
            if (match(Kind.RSQUARE)) {
                return new Dimension(first, width, height);
            }
            else {
                throw new SyntaxException("No right square");
            }
        }
        else {
            throw new SyntaxException("");
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




   private PixelFuncExpr pixelFuncExpr() throws PLCException{
       currentIndex--;
       IToken first = peek();

       advance();

       PixelSelector ps = pixelSelector();

       return new PixelFuncExpr(first, first.getKind(), ps);

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
        IToken firstToken = peek();
        if (match(Kind.BANG, Kind.RES_cos, Kind.RES_sin, Kind.RES_atan, Kind.MINUS)) {

            IToken operator = previous();
            Expr right = unary();
            return new UnaryExpr(operator, operator.getKind(), right);
        } else {
            Expr pe = primary();
            if (tokenList.get(currentIndex).getKind() == Kind.LSQUARE) {
                PixelSelector ps = pixelSelector();
                if (match(Kind.COLON)) {
                    ColorChannel cc = channelSelector();
                    return new UnaryExprPostfix(firstToken, pe, ps, cc);
                }
                else {
                    return new UnaryExprPostfix(firstToken, pe, ps, null);
                }
            }
            else {
                return pe;
            }
        }
    }

    //<primary_expr> ::= STRING_LIT |NUM_LIT |IDENT |( <expr> ) |Z rand
    private Expr primary() throws PLCException{

        if (match(Kind.STRING_LIT)) return new StringLitExpr(previous());
        else if (match(Kind.NUM_LIT)) return new NumLitExpr(previous());
        else if (match(Kind.IDENT)) return new IdentExpr(previous());
        else if (match(Kind.RES_Z)) return new ZExpr(previous());
        else if (match(Kind.RES_rand)) return new RandomExpr(previous());
        else if (match(Kind.RES_x_cart, Kind.RES_y_cart, Kind.RES_a_polar, Kind.RES_r_polar)) return pixelFuncExpr();
        else if (match(Kind.LSQUARE)) return expandedPixelExpr();
        else if(match(Kind.RES_x,Kind.RES_y, Kind.RES_a, Kind.RES_r)) return new PredeclaredVarExpr(previous());
        else if (match(Kind.LPAREN)) {
            Expr expr = expression();
            if(!match(Kind.RPAREN)) {
                throw new SyntaxException("conditional fail for '('");
            }
            return expr;
        }
        throw new SyntaxException("primary func L");
    }

    private ExpandedPixelExpr expandedPixelExpr() throws PLCException{
        currentIndex--;
        IToken first = peek();


        if (match(Kind.LSQUARE)) {
            Expr x = expression();
            consume(Kind.COMMA);
            Expr y = expression();
            consume(Kind.COMMA);
            Expr z = expression();
            if (match(Kind.RSQUARE)) {
                return new ExpandedPixelExpr(first, x, y, z);
            }
            else {
                throw new SyntaxException("No right square");
            }
        }
        else {
            throw new SyntaxException("All are wrong 5");
        }

    }



}
