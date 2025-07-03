package com.craftinginterpreters.lox;

import java.util.HashMap;
import java.util.Map;

class Environment {
    private final Map<String, Object> values = new HashMap<>();

    //새 변수 정의
    void define(String name,Object value) {
        values.put(name,value);
    }
    //원소 값 얻기 (정의 안되어 있으면 실패)
    Object get(Token name) {
        if (values.containsKey(name.lexeme)) {
            return values.get(name.lexeme);
        }
        throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
    }
    void assign(Token name, Object value) {
        if (values.containsKey(name.lexeme)) {
            values.put(name.lexeme,value);
            return;
        }
        throw new RuntimeError(name,"Undefined variable '" + name.lexeme + "'.");
    }
}
