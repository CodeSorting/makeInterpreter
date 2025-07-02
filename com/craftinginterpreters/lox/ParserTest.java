package com.craftinginterpreters.lox;

import java.util.List;

public class ParserTest {
    public static void main(String[] args) {
        String source = "30 + 5 * 2 - 10 / 2";
        Scanner scanner = new Scanner(source);
        List<Token> tokens = scanner.scanTokens();
        Parser parser = new Parser(tokens);
        Expr expression = parser.parse();
        System.out.println(new AstPrinter().print(expression));
    }
}
