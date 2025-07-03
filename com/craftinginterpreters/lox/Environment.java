package com.craftinginterpreters.lox;

import java.util.HashMap;
import java.util.Map;

class Environment {
    private final Map<String, Object> values = new HashMap<>();
    final Environment enclosing; //전역,지역 변수 체이닝 하기 위해 만듬.
    
    Environment() {
        enclosing = null;
    }
    Environment(Environment enclosing) {
        this.enclosing = enclosing;
    }

    //새 변수 정의
    void define(String name,Object value) {
        values.put(name,value);
    }
    //원소 값 얻기 (정의 안되어 있으면 실패)
    Object get(Token name) {
        if (values.containsKey(name.lexeme)) {
            return values.get(name.lexeme);
        }
        if (enclosing!=null) return enclosing.get(name); //재귀로 계속 들어가서 찾음.
        throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
    }
    void assign(Token name, Object value) {
        if (values.containsKey(name.lexeme)) {
            values.put(name.lexeme,value);
            return;
        }
        if (enclosing!=null) { //재귀로 계속 들어가서 찾음.
            enclosing.assign(name,value);
            return;
        }
        throw new RuntimeError(name,"Undefined variable '" + name.lexeme + "'.");
    }
}
