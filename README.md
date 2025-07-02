# Lox 인터프리터 실행 가이드

## 프로젝트 구조
```
java/
└── com/
    └── craftinginterpreters/
        └── lox/
            ├── Lox.java          # 메인 클래스
            ├── Scanner.java      # 토큰 스캐너
            ├── Token.java        # 토큰 클래스
            └── TokenType.java    # 토큰 타입 열거형
```

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