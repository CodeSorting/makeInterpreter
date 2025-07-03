package com.craftinginterpreters.lox;

import static com.craftinginterpreters.lox.TokenType.*;
import java.util.List;
//추상 구문 트리에서 표현식 Stmt,Expr을 받아서 해당 표현식의 타입에 맞는 비지터 메서드를 호출함.
class Interpreter implements Expr.Visitor<Object>,Stmt.Visitor<Void> {
    private Environment environment = new Environment();
    void interpret(List<Stmt> statements) {
        try {
            for (Stmt statement : statements) {
                execute(statement);
            }
        } catch (RuntimeError error) {
            Lox.runtimeError(error);
        }
    }
    private void execute(Stmt stmt) {
        stmt.accept(this);
    }
    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }
    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        return evaluate(expr.expression);
    }
    private Object evaluate(Expr expr) { 
        return expr.accept(this);
    }
    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        evaluate(stmt.expression); //표현식
        return null;
    }
    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        Object value = evaluate(stmt.expression);
        System.out.println(stringify(value)); //출력!
        return null;
    }
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
    private boolean isTruthy(Object object) {
        if (object==null) return false;
        if (object instanceof Boolean) return (boolean) object;
        return true;
    }
    private boolean isEqual(Object a,Object b) {
        if (a==null && b==null) return true;
        if (a==null) return false;
        return a.equals(b);
    }
    //문자열화 시킴.
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
                if ((double)right == 0) { //0으로 나누면 에러러
                    throw new RuntimeError(expr.operator, "Division by zero.");
                }
                return (double)left / (double)right;  
            case STAR:
                checkNumberOperand(expr.operator,left,right);
                return (double)left * (double)right;
        }
        return null; //실행되지 않는 코드
    }

    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        Object value = null;
        if (stmt.initializer != null) {
            value = evaluate(stmt.initializer);
        }
        environment.define(stmt.name.lexeme,value);
        return null;
    }
    @Override
    public Object visitAssignExpr(Expr.Assign expr) {
        Object value = evaluate(expr.value);
        environment.assign(expr.name,value);
        return value;
    }
    @Override
    public Object visitVariableExpr(Expr.Variable expr) {
        return environment.get(expr.name);
    }
}
