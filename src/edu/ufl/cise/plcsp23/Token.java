package edu.ufl.cise.plcsp23;

public class Token implements IToken{

    final Kind kind;
    final int pos;
    final int length;
    final char[] source;
    final int line;
    final int column;

    public Token(Kind kind, int pos, int length, char[] source, int line, int column) {
        super();
        this.kind = kind;
        this.pos = pos;
        this.length = length;
        this.source = source;
        this.line = line;
        this.column = column;

    }

    @Override
    public SourceLocation getSourceLocation() {
        return new SourceLocation(line, column);
    }

    @Override
    public Kind getKind() {
        return kind;
    }

    @Override
    public String getTokenString() {
        String temp = "";
        for(int i = pos; i < pos + length; i++){
            temp += source[i];
        }
        return temp;
    }
    @Override  public String toString() {
        String temp = "";
        for(int i = pos; i < pos + length; i++){
            temp += source[i];
        }
        return temp;
    }
}


