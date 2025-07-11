package com.craftinginterpreters.tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
// java com.craftinginterpreters.tool.GenerateAst com/craftinginterpreters/lox 로 실행.
//보일러 플레이트 코드다. defineAst 안에 정의할 내용을 쓰면 알아서 추상 클래스, 전역 클래스 및 생성자,필드를 정의한다.
public class GenerateAst {
    public static void main(String[] args) throws IOException{
        if (args.length!=1) {
            System.err.println("Usage: generate_ast <output directory>");
            System.exit(64);
        }
        String outputDir = args[0];
        defineAst(outputDir,"Expr",Arrays.asList(
          "Binary   : Expr left, Token operator, Expr right",
             "Call : Expr callee, Token paren, List<Expr> arguments",
             "Get : Expr object, Token name",
             "Set : Expr object, Token name, Expr value",
             "This : Token keyword",
             "IndexGet   : Expr object, Expr index",
             "IndexSet   : Expr object, Expr index, Expr value",
             "Grouping : Expr expression",
             "Literal  : Object value", 
             "Logical  : Expr left, Token operator, Expr right",
             "Unary    : Token operator, Expr right",
             "Variable : Token name",
             "Assign   : Token name, Expr value",
             "Array    : List<Expr> elements"
        ));
        defineAst(outputDir, "Stmt", Arrays.asList(
          "Block : List<Stmt> statements",
            "Class : Token name, List<Stmt.Function> methods",
            "Expression : Expr expression",
            "Function : Token name, List<Token> params, List<Stmt> body",
            "If : Expr condition, Stmt thenBranch," + " Stmt elseBranch",
                "Print : Expr expression",
                "Return : Token keyword, Expr value",
                "Var : Token name, Expr initializer",
                "While : Expr condition, Stmt body",
                "Break      : ",
                "Continue   : "
        ));
    }
    private static void defineAst(
        String outputDir, String baseName, List<String> types
    ) throws IOException {
        String path = outputDir + "/" + baseName + ".java";
        PrintWriter writer = new PrintWriter(path,"UTF-8");

        writer.println("package com.craftinginterpreters.lox;");
        writer.println();
        writer.println("import java.util.List;");
        writer.println();
        writer.println("abstract class " + baseName + "{");
        
        defineVisitor(writer,baseName,types);

        //AST 클래스
        for (String type : types) {
            String[] parts = type.split(":");
            String className = parts[0].trim();
            String fields = parts.length > 1 ? parts[1].trim() : "";
            defineType(writer,baseName,className,fields);
        }
        //베이스 accept() 메서드
        writer.println();
        writer.println("  abstract <R> R accept(Visitor<R> visitor);");
        
        writer.println("}");
        writer.close();
    }
    private static void defineVisitor(PrintWriter writer,String baseName, List<String> types) {
        writer.println("  interface Visitor<R> {");
        for (String type : types) {
            String typeName = type.split(":")[0].trim();
            writer.println("    R visit" + typeName + baseName + "(" + typeName + " " + baseName.toLowerCase() + ");");
        }
        writer.println("  }");
    }
    private static void defineType(
        PrintWriter writer, String baseName, String className, String fieldList) {
        writer.println("  static class " + className + " extends " + baseName + " {");

        // 생성자
        writer.print("    " + className + "(");
        if (!fieldList.isEmpty()) {
            String[] fields = fieldList.split(", ");
            for (int i = 0; i < fields.length; i++) {
                writer.print(fields[i]);
                if (i != fields.length - 1) writer.print(", ");
            }
        }
        writer.println(") {");
        if (!fieldList.isEmpty()) {
            String[] fields = fieldList.split(", ");
            for (String field : fields) {
                String name = field.split(" ")[1];
                writer.println("      this." + name + " = " + name + ";");
            }
        }
        writer.println("    }");

        // 비지터 패턴 accept 메서드 (항상 생성)
        writer.println();
        writer.println("    @Override");
        writer.println("    <R> R accept(Visitor<R> visitor) {");
        writer.println("      return visitor.visit" + className + baseName + "(this);");
        writer.println("    }");

        // 필드 선언
        if (!fieldList.isEmpty()) {
            String[] fields = fieldList.split(", ");
            for (String field : fields) {
                writer.println("    final " + field + ";");
            }
        }
        writer.println("  }");
    }
}
/*
package com.craftinginterpreters.lox;

import java.util.List;

abstract class Expr {
  static class Binary extends Expr {
    Binary(Expr left, Token operator, Expr right) {
      this.left = left;
      this.operator = operator;
      this.right = right;
    }

    final Expr left;
    final Token operator;
    final Expr right;
  }

  static class Grouping extends Expr {
    Grouping(Expr expression) {
      this.expression = expression;
    }

    final Expr expression;
  }

  static class Literal extends Expr {
    Literal(Object value) {
      this.value = value;
    }

    final Object value;
  }

  static class Unary extends Expr {
    Unary(Token operator, Expr right) {
      this.operator = operator;
      this.right = right;
    }

    final Token operator;
    final Expr right;
  }
}

 */