package edu.ufl.cise.plcsp23;

import java.util.Arrays;
import java.util.HashMap;
public class Scanner implements  IScanner {
    String input;
    char[] inputChars;

    int pos;
    char ch;
    int line;
    int column;

    private enum State {
        START,
        HAVE_EQ,
        IN_IDENT,
        IN_NUM_LIT,
        HAVE_GREATER,
        HAVE_AMP,
        IN_STRING_LIT,
        HAVE_EXCHANGE,
        HAVE_OR,
        HAVE_MULT,
        HAVE_COMMENT,
        ESCAPE
    }

    HashMap<String, IToken.Kind> reservedWords;
    public void populateMap(){
        reservedWords = new HashMap<String,IToken.Kind>();
        reservedWords.put("image", IToken.Kind.RES_image);
        reservedWords.put("pixel", IToken.Kind.RES_pixel);
        reservedWords.put("int", IToken.Kind.RES_int);
        reservedWords.put("string", IToken.Kind.RES_string);
        reservedWords.put("void", IToken.Kind.RES_void);
        reservedWords.put("nil", IToken.Kind.RES_nil);
        reservedWords.put("load", IToken.Kind.RES_load);
        reservedWords.put("display", IToken.Kind.RES_display);
        reservedWords.put("write", IToken.Kind.RES_write);
        reservedWords.put("x", IToken.Kind.RES_x);
        reservedWords.put("y", IToken.Kind.RES_y);
        reservedWords.put("a", IToken.Kind.RES_a);
        reservedWords.put("r", IToken.Kind.RES_r);
        reservedWords.put("X", IToken.Kind.RES_X);
        reservedWords.put("Y", IToken.Kind.RES_Y);
        reservedWords.put("Z", IToken.Kind.RES_Z);
        reservedWords.put("x_cart", IToken.Kind.RES_x_cart);
        reservedWords.put("y_cart", IToken.Kind.RES_y_cart);
        reservedWords.put("a_polar", IToken.Kind.RES_a_polar);
        reservedWords.put("r_polar", IToken.Kind.RES_r_polar);
        reservedWords.put("rand", IToken.Kind.RES_rand);
        reservedWords.put("sin", IToken.Kind.RES_sin);
        reservedWords.put("cos", IToken.Kind.RES_cos);
        reservedWords.put("atan", IToken.Kind.RES_atan);
        reservedWords.put("if", IToken.Kind.RES_if);
        reservedWords.put("while", IToken.Kind.RES_while);


    }


    private boolean isDigit(int ch) {
        return '0' <= ch && ch <= '9';
    }
    private boolean isLetter(int ch) {
        return ('A' <= ch && ch <= 'Z') || ('a' <= ch && ch <= 'z');
    }
    private boolean isIdentStart(int ch) {
        return isLetter(ch) || (ch == '$') || (ch == '_');
    }
    private boolean isEscapeSequence(char letter){
        boolean res = false;
        switch(letter){
            case 'n', 't', 'r', 'f', 'b', '"' -> res = true;
            default -> res = false;
        }
        return res;
    }

    private void error(String message) throws LexicalException{
        throw new LexicalException("Error at pos " + pos + ": " + message);
    }
    public void nextChar(){
        if(ch == '\n'){
            line++;
            column = 1;
        }
        else{
            column++;
        }

        pos++;
        ch = inputChars[pos];
        // a /n would increase line and reset column
        // a /t would increase column by 2
    }

    public Scanner(String _input) {
        input = _input;
        inputChars = Arrays.copyOf(input.toCharArray(), input.length() + 1);
        pos = 0;
        ch = inputChars[pos];
        this.populateMap();
        line = 1;
        column = 1;

    }

    @Override
    public IToken next() throws LexicalException {

        return scanToken();
    }

    private Token scanToken() throws LexicalException {
        State state = State.START;
        int tokenStart = -1;
        while (true) { //read chars, loop terminates when a Token is returned
            switch (state) {
                case START -> {
                    tokenStart = pos;
                    switch (ch) {
                        case 0 -> { //end of input
                            return new Token(IToken.Kind.EOF, tokenStart, 0, inputChars, line, column);
                        }
                        case ' ', '\n', '\r', '\t', '\f' -> nextChar();
                        case '_' -> {
                            state = State.IN_IDENT;
                            nextChar();
                        }
                        case '+' -> {
                            nextChar();
                            return new Token(IToken.Kind.PLUS, tokenStart, 1, inputChars, line, column - 1);
                        }
                        case '*' -> {
                            state = State.HAVE_MULT;
                            nextChar();
                        }
                        case '0' -> {
                            nextChar();
                            return new NumLitToken(IToken.Kind.NUM_LIT, tokenStart, 1, inputChars, line, column - 1);
                        }
                        case '=' -> {
                            state = State.HAVE_EQ;
                            nextChar();
                        }
                        case '&' -> {
                            state = State.HAVE_AMP;
                            nextChar();
                        }
                        case '<' -> {
                            state = State.HAVE_EXCHANGE;
                            nextChar();
                        }
                        case '>' -> {
                            state = State.HAVE_GREATER;
                            nextChar();
                        }
                        case '|' -> {
                            state = State.HAVE_OR;
                            nextChar();
                        }
                        case '.' -> {
                            nextChar();
                            return new Token(IToken.Kind.DOT, tokenStart, 1, inputChars, line, column - 1);
                        }
                        case ',' -> {
                            nextChar();
                            return new Token(IToken.Kind.COMMA, tokenStart, 1, inputChars, line, column - 1);
                        }
                        case '?' -> {
                            nextChar();
                            return new Token(IToken.Kind.QUESTION, tokenStart, 1, inputChars, line, column - 1);
                        }
                        case ':' -> {
                            nextChar();
                            return new Token(IToken.Kind.COLON, tokenStart, 1, inputChars, line, column - 1);
                        }
                        case '(' -> {
                            nextChar();
                            return new Token(IToken.Kind.LPAREN, tokenStart, 1, inputChars, line, column- 1);
                        }
                        case ')' -> {
                            nextChar();
                            return new Token(IToken.Kind.RPAREN, tokenStart, 1, inputChars, line, column - 1);
                        }
                        case '[' -> {
                            nextChar();
                            return new Token(IToken.Kind.LSQUARE, tokenStart, 1, inputChars, line, column - 1);
                        }
                        case ']' -> {
                            nextChar();
                            return new Token(IToken.Kind.RSQUARE, tokenStart, 1, inputChars, line, column - 1);
                        }
                        case '{' -> {
                            nextChar();
                            return new Token(IToken.Kind.LCURLY, tokenStart, 1, inputChars, line, column- 1);
                        }
                        case '}' -> {
                            nextChar();
                            return new Token(IToken.Kind.RCURLY, tokenStart, 1, inputChars, line, column- 1);
                        }
                        case '!' -> {
                            nextChar();
                            return new Token(IToken.Kind.BANG, tokenStart, 1, inputChars, line, column- 1);
                        }
                        case '-' -> {
                            nextChar();
                            return new Token(IToken.Kind.MINUS, tokenStart, 1, inputChars, line, column- 1);
                        }
                        case '/' -> {
                            nextChar();
                            return new Token(IToken.Kind.DIV, tokenStart, 1, inputChars, line, column- 1);
                        }
                        case '%' -> {
                            nextChar();
                            return new Token(IToken.Kind.MOD, tokenStart, 1, inputChars, line, column- 1);
                        }

                        case '1', '2', '3', '4', '5', '6', '7', '8', '9' -> {
                            state = State.IN_NUM_LIT;
                            nextChar();
                        }
                        case '"' -> {
                            state = State.IN_STRING_LIT;
                            nextChar();
                        }
                        case '~' -> {
                            state = State.HAVE_COMMENT;
                            nextChar();
                        }
                        default -> {
                            if(isLetter(ch)){
                                state = State.IN_IDENT;
                                nextChar();
                            }
                            else{
                                throw new LexicalException("Illegal Character here");
                            }
                        }
                    }
                }
                case HAVE_EQ -> {
                        if(ch == '='){
                            state = state.START;
                            nextChar();
                            return new Token(IToken.Kind.EQ, tokenStart, 2, inputChars, line, column - 2);
                        }
                        else{
                            return new Token(IToken.Kind.ASSIGN, tokenStart, 1, inputChars, line, column- 1);
                        }
                    }
                case HAVE_EXCHANGE -> {
                    if (ch == '-' && inputChars[pos+1] == '>') {
                        state = state.START;
                        nextChar();
                        nextChar();
                        return new Token(IToken.Kind.EXCHANGE, tokenStart, 3, inputChars, line, column - 3);
                    }
                    else if (ch == '=') {
                        state = state.START;
                        nextChar();
                        return new Token(IToken.Kind.LE, tokenStart, 2, inputChars, line, column- 2);
                    }
                    else if (ch == '-' && inputChars[pos + 1] != '>') {
                        throw new LexicalException("is incomplete");
                    }
                    else {
                        state = state.START;
                        return new Token(IToken.Kind.LT, tokenStart, 1, inputChars, line, column - 1);
                    }
                }
                case HAVE_GREATER -> {
                    if (ch == '=') {
                        state = state.START;
                        nextChar();
                        return new Token(IToken.Kind.GE, tokenStart, 2, inputChars, line, column - 2);
                    }
                    else {
                        state = state.START;
                        return new Token(IToken.Kind.GT, tokenStart, 1, inputChars, line, column- 1);
                    }
                }
                case HAVE_MULT -> {
                    if(ch == '*'){
                        state = state.START;
                        nextChar();
                        return new Token(IToken.Kind.EXP, tokenStart, 2, inputChars, line, column - 2);
                    }
                    else{
                        state = state.START;
                        return new Token(IToken.Kind.TIMES, tokenStart, 1, inputChars, line, column- 1);
                    }

                }
                case HAVE_AMP -> {
                    if (ch == '&') {
                        state = state.START;
                        nextChar();
                        return new Token(IToken.Kind.AND, tokenStart, 2, inputChars, line, column - 2);
                    }
                    else {
                        state = state.START;
                        return new Token(IToken.Kind.BITAND, tokenStart, 1, inputChars, line, column - 1);
                    }
                }
                case HAVE_OR ->  {
                    if(ch == '|'){
                        state = State.START;
                        nextChar();
                        return new Token(IToken.Kind.OR, tokenStart, 2, inputChars, line, column - 2);
                    }
                    else{
                        state = State.START;
                        return new Token(IToken.Kind.BITOR, tokenStart, 1, inputChars, line, column - 1);
                    }

                }
                case IN_NUM_LIT -> {
                    if(isDigit(ch)){
                        nextChar();
                    }
                    else{
                        int length = pos - tokenStart;
                        try{
                            NumLitToken numLitToken = new NumLitToken(IToken.Kind.NUM_LIT, tokenStart, length, inputChars, line, column);
                            numLitToken.getValue();
                            return numLitToken;
                        }
                        catch(NumberFormatException e){
                            throw new LexicalException("numLitTooBig");
                        }

                    }
                }
                case IN_IDENT -> {
                    if(isIdentStart(ch) || isDigit(ch)){
                        nextChar();
                    }
                    else{
                        int length = pos - tokenStart;
                        String text = input.substring(tokenStart,tokenStart + length);
                        IToken.Kind kind = reservedWords.get(text);
                        if(kind == null) {
                            kind = IToken.Kind.IDENT;
                        }
                        return new Token(kind, tokenStart, length, inputChars, line, column-length);
                    }
                }
                case IN_STRING_LIT -> {
                    while(true) {
                        if (ch == 0) {
                            throw new UnsupportedOperationException("\" Not Found");
                        }
                        else if (ch == '\\' && !isEscapeSequence(inputChars[pos + 1])) {
                            //switch statements for escape sequences
                            throw new LexicalException("Bad escape sequence here...");
                        }
                        else if(ch == '\n' || ch == '\r'){
                            throw new LexicalException("Illegal escape sequence");
                        }

                        else if (ch == '"' && inputChars[pos - 1] != '\\') {
                            break;
                        }
                        else {
                            nextChar();
                        }
                    }
                    nextChar();
                    int lengthString = pos - tokenStart;
                    return new StringLitToken(IToken.Kind.STRING_LIT, tokenStart, lengthString, inputChars, line, column-lengthString);
                }
                case HAVE_COMMENT -> {
                    if (ch == '\n') {
                        state = State.START;
                    }
                    else {
                        nextChar();
                    }
                }
                default -> {
                    throw new UnsupportedOperationException("Bug in Scanner");
                }

                }
            }
        }
    }

