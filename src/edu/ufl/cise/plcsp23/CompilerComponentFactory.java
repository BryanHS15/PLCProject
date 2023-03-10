/*Copyright 2023 by Beverly A Sanders
 * 
 * This code is provided for solely for use of students in COP4020 Programming Language Concepts at the 
 * University of Florida during the spring semester 2023 as part of the course project.  
 * 
 * No other use is authorized. 
 * 
 * This code may not be posted on a public web site either during or after the course.  
 */

package edu.ufl.cise.plcsp23;

public class CompilerComponentFactory {
	public static IScanner makeScanner(String input) {
		Scanner newScan = new Scanner(input);

		//Add statement to return an instance of your scanner
		return newScan;
	}

	public static IParser makeAssignment2Parser(String input) throws LexicalException {
		//add code to create a scanner and parser and return the parser.
		Scanner scanner = new Scanner(input);
		Parser parser = new Parser(scanner);
		return parser;
	}


	public static IParser makeParser(String input) throws LexicalException {
		//add code to create a scanner and parser and return the parser.
		Scanner scanner = new Scanner(input);
		Parser parser = new Parser(scanner);
		return parser;
	}
}
