package com.craftinginterpreters.lox;

// Expr(추상 구문 트리)를 사람이 읽기 쉽게 출력하는 클래스 : 실제로는 필요 없다.
public class AstPrinter implements Expr.Visitor<String> {
    public String print(Expr expr) {
        return expr.accept(this);
    }

    @Override
    public String visitBinaryExpr(Expr.Binary expr) {
        return parenthesize(expr.operator.lexeme, expr.left, expr.right);
    }

    @Override
    public String visitGroupingExpr(Expr.Grouping expr) {
        return parenthesize("group", expr.expression);
    }

    @Override
    public String visitLiteralExpr(Expr.Literal expr) {
        if (expr.value == null) return "nil";
        return expr.value.toString();
    }

    @Override
    public String visitUnaryExpr(Expr.Unary expr) {
        return parenthesize(expr.operator.lexeme, expr.right);
    }
    
    @Override
    public String visitVariableExpr(Expr.Variable expr) {
        return expr.name.lexeme;
    }

    @Override
    public String visitAssignExpr(Expr.Assign expr) {
        // 예시: (assign x 123)
        return parenthesize("assign " + expr.name.lexeme, expr.value);
    }

    @Override
    public String visitLogicalExpr(Expr.Logical expr) {
        return parenthesize(expr.operator.lexeme, expr.left, expr.right);
    }

    @Override
    public String visitCallExpr(Expr.Call expr) {
        // 예시: (call function (arg1 arg2))
        StringBuilder builder = new StringBuilder();
        builder.append("(call ");
        builder.append(expr.callee.accept(this));
        for (Expr argument : expr.arguments) {
            builder.append(" ");
            builder.append(argument.accept(this));
        }
        builder.append(")");
        return builder.toString();
    }
    //1 + 2 * 3 -> (+ 1 ( * 2 3) )
    private String parenthesize(String name, Expr... exprs) {
        StringBuilder builder = new StringBuilder();
        builder.append("(").append(name);
        for (Expr expr : exprs) {
            builder.append(" ");
            builder.append(expr.accept(this));
        }
        builder.append(")");
        return builder.toString();
    }
        
} 