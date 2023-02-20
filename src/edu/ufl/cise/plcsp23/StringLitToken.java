package edu.ufl.cise.plcsp23;

public class StringLitToken extends Token implements IStringLitToken{
    public StringLitToken(Kind kind, int pos, int length, char[] source, int line, int column) {
        super(kind, pos, length, source, line, column);
    }

    @Override
    public String getValue() {
        String res = "";
        String temp1 = "";
        for(int i = pos+1; i < pos+length - 1; i++) {
            temp1 = "";
            if(source[i] != '\\'){
                res += source[i];
            }
            else{
                switch(source[i + 1]){
                    case 't':
                        temp1 += '\t';
                        break;
                    case 'b':
                        temp1 += '\b';
                        break;
                    case 'f':
                        temp1 += '\f';
                        break;
                    case 'n':
                        temp1 += '\n';
                        break;
                    case 'r':
                        temp1 += '\r';
                        break;
                    case '\"':
                        temp1 += '\"';
                        break;
                    case '\\':
                        temp1 += '\\';
                        break;
                }
                res += temp1;
                ++i;
            }
        }

        return res;
    }


}
