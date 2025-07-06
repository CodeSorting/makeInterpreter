# Lox 인터프리터 실행 가이드

## 프로젝트 전체 구조 및 파일별 역할

```
java/
└── com/
    └── craftinginterpreters/
        ├── lox/
        │   ├── AstPrinter.java      # 추상 구문 트리(Expr)를 사람이 읽기 쉽게 출력하는 클래스
        │   ├── Environment.java     # 변수 스코프(환경)를 구현, 중첩 환경 지원
        │   ├── Expr.java            # 표현식(Expr) 추상 구문 트리 및 비지터 패턴 정의
        │   ├── Interpreter.java     # AST(Stmt, Expr)를 실행하는 인터프리터, 스코프 관리
        │   ├── Lox.java             # 메인 클래스, REPL 및 파일 실행 진입점
        │   ├── Parser.java          # 파서(구문 분석기), 토큰 리스트를 AST로 변환
        │   ├── RuntimeError.java    # 런타임 에러 처리 클래스
        │   ├── Scanner.java         # 소스코드를 토큰 리스트로 변환하는 스캐너(어휘 분석기)
        │   ├── Stmt.java            # 문장(Stmt) 추상 구문 트리 및 비지터 패턴 정의
        │   ├── Token.java           # 토큰 객체, 타입/이름/리터럴/라인 정보 포함
        │   └── TokenType.java       # 토큰 타입 열거형(키워드, 연산자, 리터럴 등)
        └── tool/
            └── GenerateAst.java     # AST 클래스(Expr, Stmt) 자동 생성 도구
```

### 주요 파일/클래스별 설명 (주석 포함)

- **Lox.java**
  - 프로그램의 진입점. REPL(대화형) 또는 파일 실행을 지원.
  - `run()`에서 Scanner → Parser → Interpreter 순으로 실행.
  - 에러 처리 및 메시지 출력 담당.

- **Scanner.java**
  - 소스코드를 문자 단위로 읽어 토큰(Token) 리스트로 변환.
  - 주석, 문자열, 숫자, 식별자, 키워드, 연산자 등 다양한 토큰을 인식.
  - 내부적으로 `scanToken()`, `number()`, `identifier()` 등 메서드로 세분화.

- **Token.java / TokenType.java**
  - Token: 토큰의 타입, 원본 문자열, 리터럴 값, 라인 정보를 저장.
  - TokenType: 모든 토큰 종류(키워드, 연산자, 리터럴 등)를 열거형으로 정의.

- **Parser.java**
  - 토큰 리스트를 받아 AST(Stmt, Expr)로 변환.
  - Lox의 문법을 재귀 하강 파싱 방식으로 구현.
  - 변수 선언, 블록, 표현식, print문 등 다양한 구문을 처리.

- **Expr.java / Stmt.java**
  - Expr: 표현식(이항, 단항, 그룹, 변수, 리터럴, 할당 등) 추상 클래스와 내부 클래스들.
  - Stmt: 문장(블록, 변수 선언, print, 표현식) 추상 클래스와 내부 클래스들.
  - **비지터 패턴(Visitor Pattern)**을 통해 Interpreter, AstPrinter 등에서 타입별 처리 가능.

- **Interpreter.java**
  - AST(Stmt, Expr)를 실제로 실행(해석)하는 클래스.
  - 변수 스코프(블록) 처리를 위해 Environment를 사용.
  - 각 visit 메서드에 주석으로 역할 설명:
    - `visitBlockStmt`: 블록 내부 문장들을 새로운 환경(Environment)에서 실행, 블록 종료 시 이전 환경으로 복구
    - `executeBlock`: 실제 환경 전환 및 문장 실행 로직
    - `visitPrintStmt`: print문 실행 및 출력
    - `visitBinaryExpr`: 이항 연산자 평가 등

- **Environment.java**
  - 변수 이름과 값을 저장하는 맵(Map)과, 바깥(Environment) 참조(enclosing)를 가짐.
  - 변수 정의/조회/할당 시, 현재 환경에 없으면 바깥 환경을 따라가며 찾음(스코프 체인).

- **AstPrinter.java**
  - Expr(추상 구문 트리)을 사람이 읽기 쉬운 문자열로 변환.
  - 각 Expr 타입별로 visit 메서드 오버라이드.

- **RuntimeError.java**
  - 런타임 에러 발생 시 예외 객체로 사용.

- **tool/GenerateAst.java**
  - Expr, Stmt 등 AST 클래스/비지터 인터페이스를 자동 생성하는 도구.

---

## 비지터 패턴(Visitor Pattern) 설명 및 본 프로젝트에서의 활용

### 비지터 패턴이란?
- **비지터 패턴**은 객체 구조(트리 등)를 변경하지 않고, 각 타입별로 다양한 연산(동작)을 분리해서 구현할 수 있게 해주는 디자인 패턴입니다.
- 즉, 데이터 구조(예: AST)는 그대로 두고, 그 위에서 동작하는 로직(예: 해석, 출력 등)을 별도의 클래스로 분리할 수 있습니다.
- 새로운 연산을 추가할 때 데이터 구조를 수정하지 않고, 비지터(Visitor)만 추가하면 되므로 확장성이 좋습니다.

### Lox 인터프리터에서의 비지터 패턴 적용
- **Expr.java**와 **Stmt.java**에서 각각 `Visitor` 인터페이스를 정의하고, 각 표현식/문장 타입(이항, 단항, 그룹, 변수, 블록 등) 내부에 `accept(Visitor)` 메서드를 구현합니다.
- **Interpreter.java**와 **AstPrinter.java** 등은 이 Visitor 인터페이스를 구현하여, 각 타입별 동작(실행, 출력 등)을 visit 메서드로 분리합니다.
- 예시:
  - `Expr.Binary`(이항 연산식)은 `accept(Visitor)`를 통해 Interpreter의 `visitBinaryExpr` 또는 AstPrinter의 `visitBinaryExpr`를 호출합니다.
  - `Stmt.Print`(print문)은 `accept(Visitor)`를 통해 Interpreter의 `visitPrintStmt`를 호출합니다.
- 이렇게 하면 AST 구조(Expr, Stmt)는 변경하지 않고도, 다양한 동작(실행, 출력, 타입 검사 등)을 Visitor 클래스를 추가하여 확장할 수 있습니다.
- **장점:**
  - AST 구조와 동작(해석, 출력 등)이 분리되어 코드가 명확해짐
  - 새로운 동작(Visitor) 추가가 쉬움 (예: 타입 검사기, 최적화기 등)
  - 각 타입별 visit 메서드에 주석을 달아 역할을 명확히 설명할 수 있음

---

## 컴파일 및 실행 방법

### 1. 디렉토리 이동
```bash
cd C:\java
```

### 2. 컴파일
```bash
javac com/craftinginterpreters/lox/*.java
```

### 3. 실행 방법

#### 대화형 모드 (REPL)
```bash
java com.craftinginterpreters.lox.Lox
```
- `>` 프롬프트가 나타나면 Lox 코드를 직접 입력
- 각 줄을 입력하면 토큰으로 분석되어 출력
- 종료하려면 `Ctrl+C` 또는 빈 줄에서 `Enter`

#### 파일 실행
```bash
java com.craftinginterpreters.lox.Lox [파일명.lox]
```

## 사용 예시

### 대화형 모드에서 테스트
```
> print "Hello, World!";
> var x = 42;
> var name = "Lox";
> var result = x + 10;
```

### 토큰 분석 결과
입력한 코드는 다음과 같이 토큰으로 분석됩니다:
- `PRINT` - 출력 키워드
- `STRING` - 문자열 리터럴
- `VAR` - 변수 선언 키워드
- `IDENTIFIER` - 변수명
- `NUMBER` - 숫자 리터럴
- `SEMICOLON` - 세미콜론

## 주의사항
- Java 8 이상이 필요합니다
- 컴파일 후 `.class` 파일이 생성됩니다
- 대화형 모드에서는 각 줄이 즉시 토큰으로 분석됩니다 

---

## 파일 구조 이해를 위한 핵심 용어 요약

- **토큰(Token)**: 소스 코드를 의미 있는 최소 단위로 나눈 조각. (예: `x`, `=`, `3`, `+`, `5`)
- **어휘 분석(Lexical Analysis)**: 소스 코드를 토큰으로 분리하는 과정. 담당: 스캐너(Scanner)
- **구문 분석(Parsing)**: 토큰이 문법적으로 맞는지 확인하고 트리로 만드는 과정. 담당: 파서(Parser)
- **표현식(Expression)**: 값을 만들어내는 코드 조각. (예: `3 + 5`, `x`)
- **문장(Statement)**: 동작을 수행하는 코드 단위. (예: `x = 3 + 5`, `print(x)`)
- **추상 구문 트리(AST)**: 코드의 구조를 트리 형태로 표현한 것. (예: `3 + 5`의 AST)
- **평가(Evaluation)**: AST나 표현식을 실제로 계산하여 값을 만드는 단계. 담당: 인터프리터(Interpreter)
- **환경(Environment)**: 변수와 값이 저장되는 공간. (예: `{ x: 5, y: 7 }`)
- **인터프리터(Interpreter)**: 코드를 한 줄씩 읽고 즉시 실행하는 프로그램.
- **비지터 패턴(Visitor Pattern)**: AST(Expr, Stmt) 각 타입별 동작을 분리해 처리하는 디자인 패턴. (예: Interpreter, AstPrinter)
- **스코프(Scope)**: 변수의 유효 범위. 블록({ ... })마다 새로운 환경(Environment)이 생성되어 변수 격리.
- **런타임 에러(Runtime Error)**: 실행 중 발생하는 오류. (예: 0으로 나누기, 정의되지 않은 변수 접근)
- **REPL(Read-Eval-Print Loop)**: 한 줄씩 코드를 입력하고 바로 결과를 확인하는 대화형 실행 환경.
- **자동 코드 생성 도구**: AST(Expr, Stmt) 클래스와 비지터 인터페이스를 자동으로 만들어주는 도구. (예: tool/GenerateAst.java)

---
