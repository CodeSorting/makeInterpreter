package com.craftinginterpreters.lox;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import static com.craftinginterpreters.lox.TokenType.*;
import static com.craftinginterpreters.lox.Lox.*;
/*
program -> declaration* EOF;
declaration -> funDecl | varDecl | statement;
funDecl -> "fun" function ;
function -> IDENTIFIER "(" parameters? ")" block ;
parameters -> IDENTIFIER ( "," IDENTIFIER )* ;
statement -> exprStmt | forStmt | ifStmt | printStmt | whileStmt | block ;
forStmt -> "for" "(" ( varDecl | exprStmt | ";" ) expression? ";" expression? ")" statement ;
ifStmt -> "if" "(" expression ")" statement ( "else" statement )? ;
whileStmt -> "while" "(" expression ")" statement ;
block -> "{" declaration* "}" ;
varDecl -> "var" IDENTIFIER ( "=" expression ) ? ";" ;
expression  → assignment;
assignment -> IDENTIFIER "=" assignment | logic_or ;
logic_or -> logic_and ( "or" logic_and )* ;
logic_and -> equality ( "and" equality )* ;
equality    → comparison ( ( "!=" | "==" ) comparison )* ;
comparison  → term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
term        → factor ( ( "-" | "+" ) factor )* ;
factor      → unary ( ( "/" | "*" ) unary )* ;
unary       → ( "!" | "-" ) unary | call ;
call        -> primary ( "(" arguments? ")" )* ;
primary     → NUMBER | STRING | "true" | "false" | "nil" | "(" expression ")" | IDENTIFIER;

arguments -> expression ( "," expression )* ;
*/
class Parser {
    private static class ParseError extends RuntimeException {}

    private final List<Token> tokens;
    private int current = 0;
    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }
    List<Stmt> parse() {
        List<Stmt> statements = new ArrayList<>();
        while (!isAtEnd()) {
            statements.add(declaration());
        }
        return statements;
    }
    private Stmt declaration() {
        try {
            if (match(FUN)) return function("function");
            if (match(VAR)) return varDeclaration();
            return statement();
        } catch (ParseError error) {
            synchronize();
            return null;
        }
    }
    private Expr expression() {
        return assignment();
    }
    private Expr assignment() {
        Expr expr = or();
        if (match(EQUAL)) {
            Token equals = previous();
            Expr value = assignment();

            if (expr instanceof Expr.Variable) {
                Token name = ((Expr.Variable)expr).name;
                return new Expr.Assign(name, value);
            }
            error(equals,"Invalid assignment target.");
        }
        return expr;
    }
    private Expr or() {
        Expr expr = and();
        while (match(OR)) {
            Token operator = previous();
            Expr right = and();
            expr = new Expr.Logical(expr,operator,right);
        }
        return expr;
    }
    private Expr and() {
        Expr expr = equality();
        while (match(AND)) {
            Token operator = previous();
            Expr right = equality();
            expr = new Expr.Logical(expr,operator,right);
        }
        return expr;
    }
    //print문 or 표현식
    private Stmt statement() {
        if (match(FOR)) return forStatement();
        if (match(IF)) return ifStatement();
        if (match(PRINT)) return printStatement();
        if (match(WHILE)) return whileStatement();
        if (match(LEFT_BRACE)) return new Stmt.Block(block());
        return expressionStatement();
    }
    //Stmt.객체 를 리턴하면 Interpreter.java 에서 해당 객체를 실행함.
    private Stmt forStatement() {
        consume(LEFT_PAREN, "Expect '(' after 'for'.");
        //초기 값
        Stmt initializer;
        if (match(SEMICOLON)) {
            initializer = null;
        } else if (match(VAR)) {
            initializer = varDeclaration();
        } else {
            initializer = expressionStatement();
        }
        //조건문
        Expr condition = null;
        if (!check(SEMICOLON)) {
            condition = expression();
        }
        consume(SEMICOLON, "Expect ';' after loop condition.");
        //증분
        Expr increment = null;
        if (!check(RIGHT_PAREN)) {
            increment = expression();
        }
        consume(RIGHT_PAREN, "Expect ')' after for clauses.");
        
        //for문을 이제 while문으로 파싱한다.
        Stmt body = statement(); //body = Stmt.Print(i);
        if (increment!=null) { //증감식이 있다면 body 뒤에 실행시킴.
            body = new Stmt.Block(Arrays.asList(body,new Stmt.Expression(increment)));
        } // body = Block( [ Print(i), Expression(i = i + 1) ] )
        if (condition==null) condition = new Expr.Literal(true); //조건식 없으면 무한루프
        body = new Stmt.While(condition,body); //body = While( condition, Block( [ Print(i), Expression(i = i + 1) ] ) )
        if (initializer!=null) {
            body = new Stmt.Block(Arrays.asList(initializer,body)); //body = Block( [ Var(i, 0), While( ... ) ] )

        }
        return body;
    }
    //if,else if,else 문, 꼼수로 else 안에 if문,else를 넣는 식으로 else if를 구현함.
    private Stmt ifStatement() {
        consume(LEFT_PAREN, "Expect '(' after 'if'.");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Expect ')' after 'if' condition.");
        Stmt thenBranch = statement();
        Stmt elseBranch = null;
        if (match(ELSE)) {
            elseBranch = statement();
        }
        return new Stmt.If(condition,thenBranch,elseBranch);
    }
    //print문
    private Stmt printStatement() {
        Expr value = expression();
        consume(SEMICOLON, "Expect ';' after value.");
        return new Stmt.Print(value);
    }
    //변수 선언 및 에러 처리
    private Stmt varDeclaration() {
        Token name = consume(IDENTIFIER, "Expect variable name");
        Expr initializer = null;
        if (match(EQUAL)) {
            initializer = expression();
        }
        consume(SEMICOLON, "Expect ';' after variable declaration");
        return new Stmt.Var(name, initializer);
    }
    //while문
    private Stmt whileStatement() {
        consume(LEFT_PAREN, "Expect '(' after 'while'.");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Expect ')' after condition.");
        Stmt body = statement();
        return new Stmt.While(condition, body);
    }
    //표현식문
    private Stmt expressionStatement() {
        Expr expr = expression();
        consume(SEMICOLON, "Expect ';' after value.");
        return new Stmt.Expression(expr);
    }
    private Stmt.Function function(String kind) {
        Token name = consume(IDENTIFIER, "Expect" + kind + " name.");
        consume(LEFT_PAREN, "Expect '(' after 'while'.");
        List<Token> parameters = new ArrayList<>();
        if (!check(RIGHT_PAREN)) {
            do {
                if (parameters.size()>=255) {
                    error(peek(),"Can't have more than 255 parameters.");
                }
                parameters.add(consume(IDENTIFIER, "Expect parameter name"));
            } while (match(COMMA));
        }
        consume(RIGHT_PAREN, "Expect ')' after condition.");
        consume(LEFT_BRACE,"Expect '{' before " + kind + " body.");
        List<Stmt> body = block();
        return new Stmt.Function(name, parameters, body);
    }
    //스코프 {} 추가문
    private List<Stmt> block() {
        List<Stmt> statements = new ArrayList<>();
        while (!check(RIGHT_BRACE) && !isAtEnd()) {
            statements.add(declaration());
        }
        consume(RIGHT_BRACE,"Expect '}' after block.");
        return statements;
    }
    private Expr equality() {
        Expr expr = comparison();
        while (match(BANG_EQUAL,EQUAL_EQUAL)) {
            Token operator = previous();
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }
    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }
    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().type==type;
    }
    private Token advance() {
        if (!isAtEnd()) ++current;
        return previous();
    }
    private boolean isAtEnd() {
        return peek().type==EOF;
    }
    private Token peek() {
        return tokens.get(current);
    }
    private Token previous() {
        return tokens.get(current-1);
    }
    private Expr comparison() {
        Expr expr = term();
        while (match(GREATER,GREATER_EQUAL,LESS,LESS_EQUAL)) {
            Token operator = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }
    private Expr term() {
        Expr expr = factor();
        while (match(MINUS,PLUS)) {
            Token operator = previous();
            Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }
    private Expr factor() {
        Expr expr = unary();
        while (match(SLASH,STAR)) {
            Token operator = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }
    private Expr unary() {
        if (match(BANG,MINUS)) {
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator,right);
        }
        return call();
    }
    private Expr call() {
        Expr expr = primary();
        while (true) {
            if (match(LEFT_PAREN)) {
                expr = finishCall(expr);
            } else {
                break;
            }
        }
        return expr;
    }
    private Expr finishCall(Expr callee) {
        List<Expr> arguments = new ArrayList<>();
        if (!check(RIGHT_PAREN)) {
            do {
                if (arguments.size()>=255)  error(peek(),"Can't have more than 255 arguments");
                arguments.add(expression());
            } while (match(COMMA));
        }
        Token paren = consume(RIGHT_PAREN, "Expect ')' after arguments.");
        return new Expr.Call(callee,paren,arguments);
    }
    private Expr primary() {
        if (match(FALSE)) return new Expr.Literal(false);
        if (match(TRUE)) return new Expr.Literal(true);
        if (match(NIL)) return new Expr.Literal(null);
        if (match(NUMBER,STRING)) {
            return new Expr.Literal(previous().literal);
        }
        if (match(IDENTIFIER)) {
            return new Expr.Variable(previous());
        }
        if (match(LEFT_PAREN)) {
            Expr expr = expression();
            consume(RIGHT_PAREN,"Expect ')' after expression. ");
            return new Expr.Grouping(expr);
        }
        throw error(peek(),"Expect expression. ");
    }
    private Token consume(TokenType type, String message) {
        if (check(type)) return advance();
        throw error(peek(),message);
    }
    private ParseError error(Token token, String message) {
        Lox.error(token,message);
        return new ParseError();
    }
    /*
    static void error(Token token, String message) {
        if (token.type==TokenType.EOF) {
            report(token.line," at end",message);
        } else {
            report(token.line," at '" + token.lexeme + "'",message);
        }
    }
    */
    private void synchronize() {
        advance();
        while (!isAtEnd()) {
            if (previous().type==SEMICOLON) return;
            switch (peek().type) {
                case CLASS:
                case FUN:
                case VAR:
                case FOR:
                case IF:
                case WHILE:
                case PRINT:
                case RETURN:
                return;
            }
            advance();
        }
    }
}