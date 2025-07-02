package com.craftinginterpreters.lox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import com.craftinginterpreters.lox.Scanner;
import com.craftinginterpreters.lox.AstPrinter;

public class Lox {
    //소스코드를 직접 읽어 실행하는 스크립트 언어이다.
    static boolean hadError = false;
    public static void main(String[] args) {
        if (args.length>1) {
            System.out.println("Usage: jlox [script]");
            System.exit(64);
        } else if (args.length==1) {
            try {
                runFile(args[0]);
            } catch (IOException e) {
                System.err.println("Error reading file: " + e.getMessage());
                System.exit(65);
            }
        } else { //args.length=0
            try {
                runPrompt();
            } catch (IOException e) {
                System.err.println("Error reading input: " + e.getMessage());
                System.exit(65);
            }
        }
    }
    // ex) jlox myscript.lox -> jlox는 args에 안 들어간다. (C와 다름.)
    private static void runFile(String path) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(path)); //파일의 모든 바이트 스트림 읽어오기
        run(new String(bytes, Charset.defaultCharset())); //실행
        //종료 코드로 에러를 식별한다.
        if (hadError) System.exit(65);
    }

    // ex) jlox -> 실시간 한줄씩 대화형 방식 REPL (Read-Eval-Print Loop)이라고 한다. ctrl D로 종료
    private static void runPrompt() throws IOException {
        InputStreamReader input = new InputStreamReader(System.in);
        BufferedReader reader = new BufferedReader(input);

        for (;;) { //한번에 한 줄 씩 코드를 읽어 실행한다.
            System.out.println("> ");
            String line = reader.readLine();
            if (line==null) break; //ctrl D로 취소
            run(line);
            hadError = false;
        }
    }
    //한줄씩 실행
    private static void run(String source) {
        Scanner scanner = new Scanner(source);
        List<Token> tokens = scanner.scanTokens(); //토큰으로 쪼갬. Scanner.java에 있다.

        /* 어휘 분석 용
        for (Token token : tokens) {
            System.out.println(token); //일단 지금은 그냥 출력
        }
        */
        Parser parser = new Parser(tokens);
        Expr expression = parser.parse();
        if (hadError) return; //구문 에러 발생 시 멈춘다.
        System.out.println(new AstPrinter().print(expression));
    }
    
    static void error(int line,String message) {
        report(line, "", message);
    }

    private static void report(int line,String where,String message) {
        System.err.println("[line " + line + "] Error" + where + ": " + message);
        hadError = true;
    }
    static void error(Token token, String message) {
        if (token.type == TokenType.EOF) {
            report(token.line, " at end", message);
        } else {
            report(token.line, " at '" + token.lexeme + "'", message);
        }
    }
    /*
    static void runtimeError(RuntimeError error) {
        System.err.println(error.getMessage() + "\n[line " + error.token.line + "]");
        hadRuntimeError = true;
    }
    */
}
