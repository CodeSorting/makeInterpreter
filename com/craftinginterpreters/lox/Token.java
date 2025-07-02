package com.craftinginterpreters.lox;
//각 토큰 : 토큰 타입, 이름, 리터럴 값, 몇번째 줄인지
class Token {
    final TokenType type;
    final String lexeme;
    final Object literal;
    final int line;

    Token(TokenType type,String lexeme,Object literal,int line) {
        this.type = type;
        this.lexeme = lexeme;
        this.literal = literal;
        this.line = line;
    }

    public String toString() {
        return type + " " + lexeme + " " + literal;
    }
}
