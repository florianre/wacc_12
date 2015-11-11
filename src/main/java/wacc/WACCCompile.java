package wacc;

import antlr.WACCLexer;
import antlr.WACCParser;
import bindings.Binding;
import bindings.NewScope;
import bindings.Type;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import wacc.error.DeclarationError;

public class WACCCompile {
  public static void main(String[] args) throws Exception {
    System.out.println("Welcome to WACC Compile...");
    // create a CharStream that reads from standard input
    ANTLRInputStream input = new ANTLRInputStream(System.in);

    // create a lexer that feeds off of input CharStream
    WACCLexer lexer = new WACCLexer(input);

    // create a buffer of tokens pulled from the lexer
    CommonTokenStream tokens = new CommonTokenStream(lexer);

    // create a parser that feeds off the tokens buffer
    WACCParser parser = new WACCParser(tokens);

    ParseTree tree = parser.prog(); // begin parsing at prog rule

    //System.out.println(tree.toStringTree(parser)); // print LISP-style tree

    System.out.println("====");
    System.out.println("Visiting...");
    SymbolTable<String, Binding> top = createTopSymbolTable();
    WACCBuildSTVisitor buildSTVisitor = new WACCBuildSTVisitor(top);
    buildSTVisitor.visit(tree);
    System.out.println("Symbol Tables: ");
    System.out.println(top);
    System.out.println(((NewScope) top.get("prog")).getSymbolTable());
    WACCTypeChecker typeChecker = new WACCTypeChecker(top);
    DeclarationError error = new DeclarationError();
    error.print();
    System.out.println("====");
  }

  private static SymbolTable<String, Binding> createTopSymbolTable() {
    SymbolTable<String, Binding> top = new SymbolTable<>();
    top.put("int", new Type("INT_T", Integer.MIN_VALUE, Integer.MAX_VALUE));
    top.put("bool", new Type("BOOL_T", 0, 1));
    top.put("char", new Type("CHAR_T", 0, 255));
    top.put("string", new Type("STRING_T"));
    top.put("pair", new Type("PAIR"));
    return top;
  }

}