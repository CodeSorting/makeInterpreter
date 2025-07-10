package com.craftinginterpreters.lox;

import java.util.HashMap;
import java.util.Map;

class LoxInstance {
    private LoxClass klass;
    private final Map<String,Object> fields = new HashMap<>();

    LoxInstance(LoxClass klass) {
        this.klass = klass;
    }
    @Override
    public String toString() {
        return klass.name + " instance";
    }
    Object get(Token name) {
        if (fields.containsKey(name.lexeme)) {
            return fields.get(name.lexeme);
        }
        LoxFunction method = klass.findMethod(name.lexeme);
        if (method!=null) return method;

        throw new RuntimeError(name, "정의되지 않은 프로퍼티 '" + name.lexeme + "'.");
    }
    
    void set(Token name,Object value) {
        fields.put(name.lexeme, value);
    }
}
