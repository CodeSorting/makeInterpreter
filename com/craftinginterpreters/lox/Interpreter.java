package com.craftinginterpreters.lox;

import static com.craftinginterpreters.lox.TokenType.*;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.Map;
import java.util.HashMap;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.StringTokenizer;

//추상 구문 트리에서 표현식 Stmt,Expr을 받아서 해당 표현식의 타입에 맞는 비지터 메서드를 호출함.
class Interpreter implements Expr.Visitor<Object>,Stmt.Visitor<Void> {
    public static final BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(System.out));
    // 전역 환경(네이티브 함수 등)을 저장
    final Environment globals = new Environment(); //네이티브 함수 정의를 위해 열어둠.
    // 현재 환경(스코프)
    private Environment environment = globals;
    //scanNum,scanString 때문에 그럼.
    Scanner sin = new Scanner(System.in);
    //리졸브 위한 변수
    private final Map<Expr,Integer> locals = new HashMap<>();
    // 빠른 입력용 버퍼 및 토크나이저
    private static final BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
    private static StringTokenizer st = null;

    // 빠른 문자열 입력
    private static String fastReadString() {
        while (st == null || !st.hasMoreTokens()) {
            try {
                String line = br.readLine();
                if (line == null) return null;
                st = new StringTokenizer(line);
            } catch (IOException e) {
                throw new RuntimeException("입력 오류: " + e.getMessage());
            }
        }
        return st.nextToken();
    }

    // 빠른 숫자 입력
    private static double fastReadDouble() {
        return Double.parseDouble(fastReadString());
    }
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
        globals.define("scanText", new LoxCallable() {
            @Override
            public int arity() { return 0; }
            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                return fastReadString();
            }
            @Override
            public String toString() { return "<native fn>"; }
        });
        globals.define("scanNum", new LoxCallable() {
            @Override
            public int arity() { return 0; }
            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                return fastReadDouble();
            }
            @Override
            public String toString() { return "<native fn>"; }
        });
        globals.define("문자열입력", new LoxCallable() {
            @Override
            public int arity() { return 0; }
            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                return fastReadString();
            }
            @Override
            public String toString() { return "<native fn>"; }
        });
        globals.define("숫자입력", new LoxCallable() {
            @Override
            public int arity() { return 0; }
            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                return fastReadDouble();
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
    void resolve(Expr expr, int depth) {
        locals.put(expr,depth);
    }
    @Override
    public Void visitClassStmt(Stmt.Class stmt) {
        environment.define(stmt.name.lexeme, null);
        Map<String,LoxFunction> methods = new HashMap<>();
        for (Stmt.Function method : stmt.methods) {
            LoxFunction function = new LoxFunction(method, environment,method.name.lexeme.equals("init"));
            methods.put(method.name.lexeme, function);
        }
        LoxClass klass = new LoxClass(stmt.name.lexeme,methods);
        environment.assign(stmt.name, klass); //맵에 클래스 이름 : LoxClass() 객체 하나 넣기
        return null;
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
        LoxFunction function = new LoxFunction(stmt,environment,false);
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
        try {
            bw.write(stringify(value));
        } catch (IOException e) {
            throw new RuntimeException("출력 오류: " + e.getMessage());
        }
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
        throw new RuntimeError(operator,"피연산자는 숫자여야 합니다.");
    }
    //이항 연산자 에러 체크
    private void checkNumberOperand(Token operator,Object left,Object right) {
        if (left instanceof Double && right instanceof Double) return;
        throw new RuntimeError(operator, "모든 피연산자는 숫자여야 합니다.");
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
                throw new RuntimeError(expr.operator, "피연산자는 두 숫자 또는 두 문자열이어야 합니다.");
            case SLASH:
                checkNumberOperand(expr.operator,left,right);
                if ((double)right == 0) { //0으로 나누면 에러
                    throw new RuntimeError(expr.operator, "0으로 나눌 수 없습니다.");
                }
                return (double)left / (double)right;  
            case STAR:
                checkNumberOperand(expr.operator,left,right);
                return (double)left * (double)right;
            case MOD:
                checkNumberOperand(expr.operator,left,right);
                if ((double)right == 0) {
                    throw new RuntimeError(expr.operator, "0으로 나눌 수 없습니다.");
                }
                return (double)left % (double)right;
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
        // 배열 내장 메서드 호출 처리
        if (callee instanceof ArrayMethodWrapper) {
            ArrayMethodWrapper wrapper = (ArrayMethodWrapper)callee;
            if (arguments.size() != wrapper.arity()) {
                throw new RuntimeError(expr.paren, "" + wrapper.arity() + "개의 인자를 기대했지만, 실제로는 " + arguments.size() + "개를 받았습니다.");
            }
            return wrapper.call(this, arguments);
        }
        if (!(callee instanceof LoxCallable)) {
            throw new RuntimeError(expr.paren, "함수나 클래스로만 호출할 수 있습니다.");
        }

        LoxCallable function = (LoxCallable)callee;
        if (arguments.size() != function.arity()) {
            throw new RuntimeError(expr.paren, "" + function.arity() + "개의 인자를 기대했지만, 실제로는 " + arguments.size() + "개를 받았습니다.");
        }
        return function.call(this,arguments);
    }
    @Override
    public Object visitGetExpr(Expr.Get expr) {
        Object object = evaluate(expr.object);
        // 배열(List) 타입의 내장 속성/메서드 처리
        if (object instanceof List) {
            String name = expr.name.lexeme;
            if (name.equals("길이") || name.equals("length")) {
                return (double)((List<?>)object).size();
            }
            if (
                name.equals("붙이기") || name.equals("append") ||
                name.equals("뒤에서빼기") || name.equals("pop_back") ||
                name.equals("앞에서빼기") || name.equals("pop_front") ||
                name.equals("앞에넣기") || name.equals("push_front")
            ) {
                return new ArrayMethodWrapper((List<Object>)object, name);
            }
            throw new RuntimeError(expr.name, "지원하지 않는 배열 속성/메서드입니다.");
        }
        // 인스턴스 필드/메서드 처리
        if (object instanceof LoxInstance) {
            return ((LoxInstance)object).get(expr.name);
        }
        throw new RuntimeError(expr.name, "오직 인스턴스와 배열만 프로퍼티를 가질 수 있습니다.");
    }
    @Override
    public Object visitSetExpr(Expr.Set expr) {
        Object object = evaluate(expr.object);
        if (!(object instanceof LoxInstance)) {
            throw new RuntimeError(expr.name, "인스턴스만 필드를 가집니다.");
        }
        Object value = evaluate(expr.value);
        ((LoxInstance)object).set(expr.name,value);
        return value;
    }
    @Override
    public Object visitThisExpr(Expr.This expr) {
        return lookUpVariable(expr.keyword, expr);
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
    // break/continue 제어용 예외
    private static class BreakException extends RuntimeException {}
    private static class ContinueException extends RuntimeException {}
    // while문 실행
    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        try {
            while (isTruthy(evaluate(stmt.condition))) {
                try {
                    execute(stmt.body);
                } catch (ContinueException ce) {
                    // 다음 반복으로 continue
                    continue;
                }
            }
        } catch (BreakException be) {
            // break로 루프 탈출
        }
        return null;
    }
    // 변수 할당 평가
    @Override
    public Object visitAssignExpr(Expr.Assign expr) {
        Object value = evaluate(expr.value);
        Integer distance = locals.get(expr);
        if (distance!=null) {
            environment.assignAt(distance,expr.name,value);
        } else {
            globals.assign(expr.name,value);
        }
        return value;
    }
    // 변수 참조 평가
    @Override
    public Object visitVariableExpr(Expr.Variable expr) {
        return lookUpVariable(expr.name,expr);
    }
    private Object lookUpVariable(Token name,Expr expr) {
        Integer distance = locals.get(expr);
        if (distance!=null) {
            return environment.getAt(distance,name.lexeme);
        } else {
            return globals.get(name);
        }
    }
    @Override
    public Void visitBreakStmt(Stmt.Break stmt) {
        throw new BreakException();
    }
    @Override
    public Void visitContinueStmt(Stmt.Continue stmt) {
        throw new ContinueException();
    }
    // 배열 리터럴 평가
    @Override
    public Object visitArrayExpr(Expr.Array expr) {
        List<Object> result = new ArrayList<>();
        for (Expr element : expr.elements) {
            result.add(evaluate(element));
        }
        return result;
    }
    // 배열 인덱싱 평가
    @Override
    public Object visitIndexGetExpr(Expr.IndexGet expr) {
        Object object = evaluate(expr.object);
        Object index = evaluate(expr.index);
        
        if (index instanceof Double) {
            if (!(object instanceof List)) {
                throw new RuntimeError(new Token(TokenType.IDENTIFIER, "array", null, 0), "배열이 아닌 객체에서 인덱싱을 시도했습니다.");
            }
            List<Object> list = (List<Object>)object;
            int idx = (int)(double)index;
            if (idx < 0 || idx >= list.size()) {
                throw new RuntimeError(new Token(TokenType.IDENTIFIER, "index", null, 0), "배열 인덱스가 범위를 벗어났습니다.");
            }
            return list.get(idx);
        } else if (index instanceof String) {
            // length/길이 지원
            if (object instanceof List && (index.equals("length") || index.equals("길이"))) {
                return (double)((List<?>)object).size();
            }
            // append/붙이기/pop_back/뒤에서빼기 지원 (메서드 접근)
            if (object instanceof List && (index.equals("append") || index.equals("붙이기") || index.equals("pop_back") || index.equals("뒤에서빼기"))) {
                List<Object> prop = new ArrayList<>();
                prop.add(object);
                prop.add(index);
                return prop;
            }
        } else {
            throw new RuntimeError(new Token(TokenType.IDENTIFIER, "index", null, 0), "배열 인덱스는 숫자 또는 문자열(프로퍼티)이여야 합니다.");
        }
        return null;
    }
    // 배열 요소 할당 평가
    @Override
    public Object visitIndexSetExpr(Expr.IndexSet expr) {
        Object object = evaluate(expr.object);
        Object index = evaluate(expr.index);
        Object value = evaluate(expr.value);
        
        if (!(object instanceof List)) {
            throw new RuntimeError(new Token(TokenType.IDENTIFIER, "array", null, 0), "배열이 아닌 객체에서 할당을 시도했습니다.");
        }
        
        if (!(index instanceof Double)) {
            throw new RuntimeError(new Token(TokenType.IDENTIFIER, "index", null, 0), "배열 인덱스는 숫자여야 합니다.");
        }
        
        List<Object> list = (List<Object>)object;
        int idx = (int)(double)index;
        
        if (idx < 0 || idx >= list.size()) {
            throw new RuntimeError(new Token(TokenType.IDENTIFIER, "index", null, 0), "배열 인덱스가 범위를 벗어났습니다.");
        }
        
        list.set(idx, value);
        return value;
    }
}

// 배열 내장 메서드 래퍼 클래스
class ArrayMethodWrapper implements LoxCallable {
    private final List<Object> array;
    private final String method;
    public ArrayMethodWrapper(List<Object> array, String method) {
        this.array = array;
        this.method = method;
    }
    @Override
    public int arity() {
        if (method.equals("붙이기") || method.equals("append") || method.equals("앞에넣기") || method.equals("push_front")) return 1;
        if (method.equals("뒤에서빼기") || method.equals("pop_back") || method.equals("앞에서빼기") || method.equals("pop_front")) return 0;
        return 0;
    }
    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        if (method.equals("붙이기") || method.equals("append")) {
            array.add(arguments.get(0));
            return null;
        }
        if (method.equals("앞에넣기") || method.equals("push_front")) {
            array.add(0, arguments.get(0));
            return null;
        }
        if (method.equals("뒤에서빼기") || method.equals("pop_back")) {
            if (array.size() == 0) return null;
            return array.remove(array.size() - 1);
        }
        if (method.equals("앞에서빼기") || method.equals("pop_front")) {
            if (array.size() == 0) return null;
            return array.remove(0);
        }
        throw new RuntimeError(null, "지원하지 않는 배열 메서드입니다.");
    }
    @Override
    public String toString() {
        return "<array method " + method + ">";
    }
}
