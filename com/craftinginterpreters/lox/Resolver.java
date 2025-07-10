package com.craftinginterpreters.lox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

// 변수, 함수 등 이름(식별자)의 유효 범위(스코프)를 추적하고, 올바른 바인딩을 찾아주는 역할을 한다.
// 즉, 각 변수/함수가 어느 블록(스코프)에서 선언되고 사용되는지 분석하여, 중복 선언, 미정의 변수 사용 등 오류를 미리 잡아준다.
// 이 과정을 'resolve(해결)'라고 하며, 인터프리터가 실제 실행 전에 이름 바인딩을 명확히 할 수 있게 도와준다.
//
// 예시: 중첩된 블록에서 같은 이름의 변수가 선언될 때, 올바른 변수를 참조하도록 스코프를 관리한다.
class Resolver implements Expr.Visitor<Void>, Stmt.Visitor<Void> {
    private final Interpreter interpreter;
    private final Stack<Map<String,Boolean>> scopes = new Stack<>();
    private FunctionType currentFunction = FunctionType.NONE;

    Resolver(Interpreter interpreter) {
        this.interpreter = interpreter;
    }
    private enum FunctionType {
        NONE, FUNCTION, METHOD
    }
    @Override
    public Void visitClassStmt(Stmt.Class stmt) {
        declare(stmt.name);
        define(stmt.name);

        for (Stmt.Function method : stmt.methods) {
            FunctionType declaration = FunctionType.METHOD;
            resolveFunction(method, declaration);
        }
        return null;
    }
    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        // 새 블록(스코프) 시작
        beginScope();
        // 블록 내부의 모든 문장 resolve(해결)
        resolve(stmt.statements);
        // 블록(스코프) 끝
        endScope();
        return null;
    }
    void resolve(List<Stmt> statements) {
        for (Stmt statement : statements) {
            resolve(statement);
        }
    } 
    private void resolve(Stmt stmt) {
        stmt.accept(this);
    }
    private void resolve(Expr expr) {
        expr.accept(this);
    }
    private void beginScope() { //push
        scopes.push(new HashMap<String,Boolean>());
    }
    private void endScope() { //pop
        scopes.pop();
    }
    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        // 변수 선언(이름만 등록)
        declare(stmt.name);
        // 초기화 식이 있으면 resolve(해결) (예: 변수 a = b + 1)
        if (stmt.initializer!=null) {
            resolve(stmt.initializer);
        }
        // 변수 정의(초기화 완료 표시)
        define(stmt.name);
        return null;
    }
    private void declare(Token name) {
        // 스코프가 비어 있으면(전역) 아무것도 하지 않음
        if (scopes.isEmpty()) return;
        // 가장 안쪽(최근)의 스코프를 가져옴
        Map<String,Boolean> scope = scopes.peek();
        // 이미 이 스코프에 같은 이름의 변수가 있으면 에러
        if (scope.containsKey(name.lexeme)) {
            Lox.error(name, "이미 이 범위 안에 이 이름의 변수가 있습니다.");
        }
        // 변수 이름을 '아직 초기화되지 않음(false)' 상태로 등록
        scope.put(name.lexeme,false); //포인터처럼 실제 객체 가리킴.
    }
    private void define(Token name) {
        // 변수 선언이 끝나고, 이제 초기화가 완료되었음을 표시
        // (이제 이 이름을 참조해도 됨)
        if (scopes.isEmpty()) return;
        scopes.peek().put(name.lexeme, true);
    }
    @Override
    public Void visitVariableExpr(Expr.Variable expr) {
        if (!scopes.isEmpty() && scopes.peek().get(expr.name.lexeme)==Boolean.FALSE) {
            Lox.error(expr.name, "초기화 중인 지역 변수는 읽을 수 없습니다.");
        } 
        resolveLocal(expr,expr.name);
        return null;
    }
    private void resolveLocal(Expr expr, Token name) {
        for (int i=scopes.size()-1;i>=0;--i) {
            if (scopes.get(i).containsKey(name.lexeme)) {
                interpreter.resolve(expr,scopes.size()-1-i);
                return;
            }
        }
    }
    @Override
    public Void visitAssignExpr(Expr.Assign expr) {
        // 대입문의 오른쪽(값) 부분을 먼저 resolve(해결)
        resolve(expr.value);
        // 대입문의 왼쪽(변수 이름)이 현재 스코프에서 어디에 바인딩되는지 확인
        resolveLocal(expr, expr.name);
        return null;
    }
    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {
        // 함수 이름을 현재 스코프에 선언(이름만 등록)
        declare(stmt.name);
        // 함수 이름을 정의(초기화 완료 표시)
        define(stmt.name);
        // 함수 본문과 매개변수의 스코프를 resolve(해결)
        resolveFunction(stmt, FunctionType.FUNCTION);
        return null;
    }
    private void resolveFunction(Stmt.Function function, FunctionType type) {
        // 현재 함수 타입(중첩 함수 대비)을 저장
        FunctionType enclosingFunction = currentFunction;
        currentFunction = type;

        // 함수의 새로운 스코프 시작(매개변수, 지역변수용)
        beginScope();
        // 매개변수 각각을 스코프에 등록(이름만 등록 후 바로 정의)
        for (Token param : function.params) {
            declare(param);
            define(param);
        }
        // 함수 본문(여러 문장) resolve(해결)
        resolve(function.body);
        // 함수 스코프 끝(매개변수, 지역변수 소멸)
        endScope();
        // 함수 타입 복구(중첩 함수 대비)
        currentFunction = enclosingFunction;
    }
    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        resolve(stmt.expression);
        return null;
    }
    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        resolve(stmt.condition);
        resolve(stmt.thenBranch);
        if (stmt.elseBranch!=null) resolve(stmt.elseBranch);
        return null;
    }
    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        resolve(stmt.expression);
        return null;
    }
    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        if (currentFunction==FunctionType.NONE) Lox.error(stmt.keyword, "맨 위 코드에 리턴(반환)문을 못 쓴다.");
        if (stmt.value != null) resolve(stmt.value);
        return null;
    }
    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        resolve(stmt.condition);
        resolve(stmt.body);
        return null;
    }
    @Override
    public Void visitBinaryExpr(Expr.Binary expr) {
        resolve(expr.left);
        resolve(expr.right);
        return null;
    }
    @Override
    public Void visitCallExpr(Expr.Call expr) {
        resolve(expr.callee);
        for (Expr argument : expr.arguments) {
            resolve(argument);
        }
        return null;
    }
    @Override
    public Void visitGroupingExpr(Expr.Grouping expr) {
        resolve(expr.expression);
        return null;
    }
    @Override
    public Void visitLiteralExpr(Expr.Literal expr) {
        return null;
    }
    @Override
    public Void visitLogicalExpr(Expr.Logical expr) {
        resolve(expr.left);
        resolve(expr.right);
        return null;
    }
    @Override
    public Void visitUnaryExpr(Expr.Unary expr) {
        resolve(expr.right);
        return null;
    }
    @Override
    public Void visitBreakStmt(Stmt.Break stmt) {
        return null;
    }
    @Override
    public Void visitContinueStmt(Stmt.Continue stmt) {
        return null;
    }
    @Override
    public Void visitArrayExpr(Expr.Array expr) {
        for (Expr element : expr.elements) {
            resolve(element);
        }
        return null;
    }
    @Override
    public Void visitIndexGetExpr(Expr.IndexGet expr) {
        resolve(expr.object);
        resolve(expr.index);
        return null;
    }
    @Override
    public Void visitIndexSetExpr(Expr.IndexSet expr) {
        resolve(expr.object);
        resolve(expr.index);
        resolve(expr.value);
        return null;
    }
    @Override
    public Void visitGetExpr(Expr.Get expr) {
        resolve(expr.object);
        return null;
    }
    @Override
    public Void visitSetExpr(Expr.Set expr) {
        resolve(expr.value);
        resolve(expr.object);
        return null;
    }
}

