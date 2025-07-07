package com.craftinginterpreters.lox;

import java.lang.management.PlatformLoggingMXBean;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static com.craftinginterpreters.lox.TokenType.*;

/**
 * Lox 언어의 스캐너(Scanner) 클래스
 * 소스 코드를 읽어서 토큰(Token)으로 분해하는 역할을 담당
 */
public class Scanner {
    private final String source;        // 스캔할 소스 코드
    private final List<Token> tokens = new ArrayList<>();  // 생성된 토큰들을 저장하는 리스트
    private int start = 0;              // 현재 토큰의 시작 위치
    private int current = 0;            // 현재 읽고 있는 위치
    private int line = 1;               // 현재 라인 번호
    
    // Lox 언어의 키워드들을 저장하는 맵
    private static final Map<String,TokenType> keywords;
    static {
        keywords = new HashMap<>();
        keywords.put("and", AND);
        keywords.put("class", CLASS);
        keywords.put("else", ELSE);
        keywords.put("false", FALSE);
        keywords.put("for", FOR);
        keywords.put("fun", FUN);
        keywords.put("if", IF);
        keywords.put("nil", NIL);
        keywords.put("or", OR);
        keywords.put("print", PRINT);
        keywords.put("return", RETURN);
        keywords.put("super", SUPER);
        keywords.put("this", THIS);
        keywords.put("true", TRUE);
        keywords.put("var", VAR);
        keywords.put("while", WHILE);
        // 한글 키워드 추가
        keywords.put("변수", VAR);
        keywords.put("출력", PRINT);
        keywords.put("범위반복", FOR);
        keywords.put("함수", FUN);
        keywords.put("만약", IF);
        keywords.put("아니면", ELSE);
        keywords.put("조건반복", WHILE);
        keywords.put("반환", RETURN);
        keywords.put("참", TRUE);
        keywords.put("거짓", FALSE);
        keywords.put("또는", OR);
        keywords.put("그리고", AND);
        keywords.put("널", NIL);
        keywords.put("break", BREAK);
        keywords.put("continue", CONTINUE);
        keywords.put("중단", BREAK);
        keywords.put("계속", CONTINUE);
    }
    
    /**
     * Scanner 생성자
     * @param source 스캔할 소스 코드 문자열
     */
    Scanner(String source) {
        this.source = source;
    }
    
    /**
     * 소스 코드를 스캔하여 토큰 리스트를 생성하는 메인 메서드
     * @return 생성된 토큰들의 리스트
     */
    List<Token> scanTokens() {
        while (!isAtEnd()) {
            // 다음 토큰의 시작 위치를 기록
            start = current;
            scanToken();
        }
        // 파일 끝을 나타내는 EOF 토큰 추가
        tokens.add(new Token(EOF,"",null,line));
        return tokens;
    }
    
    /**
     * 소스 코드의 끝에 도달했는지 확인
     * @return 끝에 도달했으면 true, 아니면 false
     */
    private boolean isAtEnd() { 
        return current >= source.length(); 
    }
    
    /**
     * 현재 위치에서 하나의 토큰을 스캔하는 메서드
     * 문자를 읽어서 해당하는 토큰 타입을 결정하고 토큰을 생성
     */
    private void scanToken() {
        char c = advance();
        switch (c) {
            // 단일 문자 토큰들
            case '(': addToken(LEFT_PAREN); break;
            case ')': addToken(RIGHT_PAREN); break;
            case '{': addToken(LEFT_BRACE); break;
            case '}': addToken(RIGHT_BRACE); break;
            case '[': addToken(LEFT_BRACKET); break;
            case ']': addToken(RIGHT_BRACKET); break;
            case ',': addToken(COMMA); break;
            case '.': addToken(DOT); break;
            case '-': addToken(MINUS); break;
            case '+': addToken(PLUS); break;
            case ';': addToken(SEMICOLON); break;
            case '*': addToken(STAR); break;
            case '%': addToken(MOD); break;
            
            // 2글자 연산자들 (!=, ==, <=, >=)
            case '!': addToken(match('=') ? BANG_EQUAL : BANG); break;
            case '=': addToken(match('=') ? EQUAL_EQUAL : EQUAL); break;
            case '<': addToken(match('=') ? LESS_EQUAL : LESS); break;
            case '>': addToken(match('=') ? GREATER_EQUAL : GREATER); break;
            
            // 주석 처리 (//로 시작하는 한 줄 주석)
            case '/':
            if (match('/')) {
                // 주석의 끝까지 건너뛰기
                while (peek()!='\n' && !isAtEnd()) advance();
            } else {
                addToken(SLASH);
            }
            break;
            
            // 공백 문자들은 무시
            case ' ':
            case '\r':
            case '\t':
            break;

            // 줄바꿈 문자는 라인 번호 증가
            case '\n':
            line++;
            break;

            // 문자열 리터럴 처리
            case '"': string(); break;

            default: 
            // 숫자나 식별자 처리
            if (isDigit(c)) {
                number();
            }else if (isAlpha(c)) {
                identifier();
            }else {
                Lox.error(line,"예상치 못한 문자입니다.");
            }
            break;
        }
    }
    
    /**
     * 다음 문자가 예상한 문자와 일치하는지 확인하고 일치하면 현재 위치를 이동
     * @param expected 예상하는 문자
     * @return 일치하면 true, 아니면 false
     */
    private boolean match(char expected) {
        if (isAtEnd()) return false;
        if (source.charAt(current)!=expected) return false;

        ++current;
        return true;
    }
    
    /**
     * 현재 위치의 문자를 읽되 위치는 이동하지 않음 (lookahead)
     * @return 현재 위치의 문자, 끝에 도달했으면 '\0'
     */
    private char peek() {
        if (isAtEnd()) return '\0';
        return source.charAt(current);
    }
    
    /**
     * 식별자(변수명, 함수명 등)를 처리하는 메서드
     * 알파벳이나 숫자로 구성된 식별자를 읽어서 키워드인지 확인
     */
    private void identifier() {
        while (isAlphaNumeric(peek())) advance();

        String text = source.substring(start,current);
        TokenType type = keywords.get(text);
        if (type==null) type = IDENTIFIER;
        addToken(type);
    }
    
    /**
     * 문자가 알파벳인지 확인 (a-z, A-Z, _, 한글)
     * @param c 확인할 문자
     * @return 알파벳 또는 한글이면 true, 아니면 false
     */
    private boolean isAlpha(char c) {
        return (c>='a' && c<='z') || (c>='A' && c<='Z') || c=='_' || (c >= '\uAC00' && c <= '\uD7A3');
    }
    
    /**
     * 문자가 알파벳이나 숫자인지 확인 (한글 포함)
     * @param c 확인할 문자
     * @return 알파벳, 한글, 숫자이면 true, 아니면 false
     */
    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);    
    }
    
    /**
     * 문자가 숫자인지 확인 (0-9)
     * @param c 확인할 문자
     * @return 숫자이면 true, 아니면 false
     */
    private boolean isDigit(char c) {
        return c>='0' && c<='9';
    }
    
    /**
     * 숫자 리터럴을 처리하는 메서드
     * 정수와 실수 모두 지원 (예: 123, 123.45)
     */
    private void number() {
        while (isDigit(peek())) advance();

        // 소수점 처리
        if (peek()=='.' && isDigit(peekNext())) {
            advance(); // .을 소비한다.

            while (isDigit(peek())) advance();
        }
        addToken(NUMBER,Double.parseDouble(source.substring(start,current)));
    }
    
    /**
     * 현재 위치의 다음 문자를 읽되 위치는 이동하지 않음
     * @return 다음 위치의 문자, 끝에 도달했으면 '\0'
     */
    private char peekNext() {
        if (current+1>=source.length()) return '\0';
        return source.charAt(current+1);
    }
    
    /**
     * 소스 파일의 다음 문자를 읽어서 반환하고 현재 위치를 이동
     * @return 읽은 문자
     */
    private char advance() {
        return source.charAt(current++);
    }
    
    /**
     * 문자열 리터럴을 처리하는 메서드
     * 큰따옴표로 둘러싸인 문자열을 읽어서 토큰으로 생성
     */
    private void string() {
        while (peek()!='"' && !isAtEnd()) {
            if (peek()=='\n') line++;
            advance();
        }
        if (isAtEnd()) {
            Lox.error(line, "문자열이 끝나지 않았습니다.");
            return;
        }
        // 닫는 큰따옴표 소비
        advance();

        // 앞뒤 큰따옴표 제거하여 실제 문자열 값 추출
        String value = source.substring(start+1,current-1);
        addToken(STRING,value);
    }

    /**
     * 리터럴 값이 없는 토큰을 추가하는 헬퍼 메서드
     * @param type 토큰 타입
     */
    private void addToken(TokenType type) {
        addToken(type,null);
    }
    
    /**
     * 토큰을 리스트에 추가하는 메서드
     * @param type 토큰 타입
     * @param literal 토큰의 리터럴 값 (숫자, 문자열 등)
     */
    private void addToken(TokenType type,Object literal) {
        String text = source.substring(start,current);
        tokens.add(new Token(type,text,literal,line));
    }
}
