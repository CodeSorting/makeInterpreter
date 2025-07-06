package com.craftinginterpreters.lox;

import static com.craftinginterpreters.lox.TokenType.*;
import java.util.List;
import java.util.ArrayList;
//추상 구문 트리에서 표현식 Stmt,Expr을 받아서 해당 표현식의 타입에 맞는 비지터 메서드를 호출함.
class Interpreter implements Expr.Visitor<Object>,Stmt.Visitor<Void> {
    // 전역 환경(네이티브 함수 등)을 저장
    final Environment globals = new Environment(); //네이티브 함수 정의를 위해 열어둠.
    // 현재 환경(스코프)
    private Environment environment = globals;
    // 인터프리터 생성자, 전역에 clock 네이티브 함수 등록
    Interpreter() {
        globals.define("clock",new LoxCallable() {
            @Override
            public int arity() { return 0; }

            @Override
            public Object call(Interpreter interpreter,List<Object> arguments) {
                return (double)System.currentTimeMillis() / 1000.0;
            }

            @Override
            public String toString() { return "<native fn>"; }
        });
    }
    // 프로그램(문장 리스트) 실행
    void interpret(List<Stmt> statements) {
        try {
            for (Stmt statement : statements) {
                execute(statement);
            }
        } catch (RuntimeError error) {
            Lox.runtimeError(error);
        }
    }
    // 단일 문장 실행
    private void execute(Stmt stmt) {
        stmt.accept(this);
    }
    // 블록({ ... }) 문장 실행 (새 환경 생성)
    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        // 블록({ ... }) 내부의 문장들을 새로운 환경(Environment)에서 실행한다.
        // 블록이 끝나면 이전 환경으로 되돌린다.
        executeBlock(stmt.statements, new Environment(environment));
        return null;
    }
    // 블록 내부 문장들을 주어진 환경에서 실행
    void executeBlock(List<Stmt> statements,Environment environment) {
        // 주어진 statements(문장 리스트)를 전달받은 environment(환경)에서 실행한다.
        // 블록 실행 전 현재 환경을 저장하고, 블록 실행 후 원래 환경으로 복구한다.
        Environment previous = this.environment;
        try {
            this.environment = environment; //현재 환경 저장
            for (Stmt statement : statements) { //실행
                execute(statement);
            }
        } finally {
            this.environment = previous; //원래 환경으로 복구
        }
    }
    // 리터럴(숫자, 문자열, true, false, nil) 평가
    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }
    // 논리 연산자(or, and) 평가
    @Override
    public Object visitLogicalExpr(Expr.Logical expr) {
        Object left = evaluate(expr.left);
        if (expr.operator.type == OR) {
            if (isTruthy(left)) return left;
        } else {
            if (!isTruthy(left)) return left;
        }
        return evaluate(expr.right);
    }
    // 괄호식 평가
    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        return evaluate(expr.expression);
    }
    // 표현식 평가 (accept 호출)
    private Object evaluate(Expr expr) { 
        return expr.accept(this);
    }
    // 표현식 문장 실행
    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        evaluate(stmt.expression); //표현식
        return null;
    }
    // 함수 선언 실행 (환경에 함수 등록)
    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {
        LoxFunction function = new LoxFunction(stmt,environment);
        environment.define(stmt.name.lexeme, function);
        return null;
    }
    // if문 실행
    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        if (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.thenBranch);
        }
        else if (stmt.elseBranch!=null) {
            execute(stmt.elseBranch);
        }
        return null;
    }
    // print문 실행
    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        Object value = evaluate(stmt.expression);
        System.out.println(stringify(value)); //출력
        return null;
    }
    // return문 실행 (Return 예외 발생)
    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        Object value = null;
        if (stmt.value != null) value = evaluate(stmt.value);
        throw new Return(value);
    }
    // 단항 연산자 평가 (!, -)
    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        Object right = evaluate(expr.right);
        switch(expr.operator.type) {
            case BANG:
                return !isTruthy(right);
            case MINUS:
                checkNumberOperand(expr.operator,right); //에러 체크
                return -(double)right;
        }
        return null; //실행되지 않는 코드
    }
    //단항 연산자 에러 체크
    private void checkNumberOperand(Token operator,Object operand) {
        if (operand instanceof Double) return;
        throw new RuntimeError(operator,"Operand must be a number.");
    }
    //이항 연산자 에러 체크
    private void checkNumberOperand(Token operator,Object left,Object right) {
        if (left instanceof Double && right instanceof Double) return;
        throw new RuntimeError(operator, "Operands must be numbers.");
    }
    // 참/거짓 판별
    private boolean isTruthy(Object object) {
        if (object==null) return false;
        if (object instanceof Boolean) return (boolean) object;
        return true;
    }
    // 동등성 비교
    private boolean isEqual(Object a,Object b) {
        if (a==null && b==null) return true;
        if (a==null) return false;
        return a.equals(b);
    }
    // 값 문자열화
    private String stringify(Object object) {
        if (object==null) return "nil";
        if (object instanceof Double) { //소수값이면 소수점 없앰.
            String text = object.toString();
            if (text.endsWith(".0")) {
                text = text.substring(0,text.length()-2);
            }
            return text;
        }
        return object.toString();
    }
    // 이항 연산자 평가 (+, -, *, /, 비교, ==, !=)
    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);
        switch(expr.operator.type) {
            case GREATER:
                checkNumberOperand(expr.operator,left,right);
                return (double)left > (double)right;
            case GREATER_EQUAL:
                checkNumberOperand(expr.operator,left,right);
                return (double)left >= (double)right;
            case LESS:
                checkNumberOperand(expr.operator,left,right);
                return (double)left < (double)right;
            case LESS_EQUAL:
                checkNumberOperand(expr.operator,left,right);
                return (double)left <= (double)right;
            case BANG_EQUAL:
                return !isEqual(left,right);
            case EQUAL_EQUAL:
                return isEqual(left,right);

            case MINUS:
                return (double)left - (double)right;       
            case PLUS:
                if (left instanceof Double && right instanceof Double) {
                    return (double)left + (double)right;
                }
                if (left instanceof String && right instanceof String) {
                    return (String)left + (String)right;
                }
                //문자열과 숫자 연산 시 문자열로 바꿔서 계산한다.
                if (left instanceof Double && right instanceof String) {
                    return String.valueOf(left) + (String)right;
                }
                if (left instanceof String && right instanceof Double) {
                    return (String)left + String.valueOf(right);
                }
                throw new RuntimeError(expr.operator, "Operands must be two numbers or two strings.");
            case SLASH:
                checkNumberOperand(expr.operator,left,right);
                if ((double)right == 0) { //0으로 나누면 에러
                    throw new RuntimeError(expr.operator, "Division by zero.");
                }
                return (double)left / (double)right;  
            case STAR:
                checkNumberOperand(expr.operator,left,right);
                return (double)left * (double)right;
        }
        return null; //실행되지 않는 코드
    }
    // 함수 호출 평가
    @Override
    public Object visitCallExpr(Expr.Call expr) {
        Object callee = evaluate(expr.callee);
        List<Object> arguments = new ArrayList<>();
        for (Expr argument : expr.arguments) {
            arguments.add(evaluate(argument));
        }

        if (!(callee instanceof LoxCallable)) {
            throw new RuntimeError(expr.paren, "Can only call functions and classes.");
        }

        LoxCallable function = (LoxCallable)callee;
        if (arguments.size() != function.arity()) {
            throw new RuntimeError(expr.paren, "Expected " + function.arity() + " arguments but got " + arguments.size() + ".");
        }
        return function.call(this,arguments);
    }
    // 변수 선언문 실행
    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        Object value = null;
        if (stmt.initializer != null) {
            value = evaluate(stmt.initializer);
        }
        environment.define(stmt.name.lexeme,value);
        return null;
    }
    // while문 실행
    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        while (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.body);
        }
        return null;
    }
    // 변수 할당 평가
    @Override
    public Object visitAssignExpr(Expr.Assign expr) {
        Object value = evaluate(expr.value);
        environment.assign(expr.name,value);
        return value;
    }
    // 변수 참조 평가
    @Override
    public Object visitVariableExpr(Expr.Variable expr) {
        return environment.get(expr.name);
    }
}
