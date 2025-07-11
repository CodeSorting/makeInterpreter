package com.craftinginterpreters.lox;

import java.util.List;

class LoxFunction implements LoxCallable {
    private final Stmt.Function declaration;
    private final Environment closure;
    private final boolean isInitializer;

    LoxFunction(Stmt.Function declaration,Environment closure,boolean isInitializer) {
        this.declaration = declaration;
        this.closure = closure;
        this.isInitializer = isInitializer;
    }
    @Override
    public Object call(Interpreter interpreter,List<Object> arguments) {
        Environment environment = new Environment(closure);
        for (int i=0;i<declaration.params.size();++i) {
            environment.define(declaration.params.get(i).lexeme, arguments.get(i));
        }
        //executeBlock으로 visitWhileStmt,visitBlockStmt,... visitReturnStmt가 되면 throw new Return(value)로 value를 던질 때 리턴한다.
        try {
            interpreter.executeBlock(declaration.body, environment);
        } catch (Return returnValue) {
            if (isInitializer) return closure.getAt(0,"this");
            return returnValue.value;
        }
        if (isInitializer) return closure.getAt(0, "this");
        return null;
    }
    @Override
    public int arity() {
        return declaration.params.size();
    }
    @Override
    public String toString() {
        return "<fn " + declaration.name.lexeme + ">";
    }
    
    LoxFunction bind(LoxInstance instance) {
        Environment environment = new Environment(closure);
        environment.define("this", instance);
        return new LoxFunction(declaration, environment,isInitializer);
    }
}
