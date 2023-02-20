package edu.ufl.cise.plcsp23;

import java.lang.reflect.Array;
import java.lang.String;

public class NumLitToken extends Token implements INumLitToken {
    public NumLitToken(Kind kind, int pos, int length, char[] source, int line, int column) {
        super(kind, pos, length, source, line, column);
    }

    @Override
    public int getValue() {
        //for loop through  char array and for each char change it to a string, then to a int through parsesInt and return the int
       String temp = "";
       for(int i = 0; i < length; i++){
           temp += source[pos + i];
       }
       return Integer.parseInt(temp);
    }
}
