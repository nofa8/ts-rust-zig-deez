package root;

import root.ast.Program;
import root.lexer.Lexer;
import root.parser.ParseProgramException;
import root.parser.Parser;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.Scanner;

public class REPL {

    private static final String PROMPT = ">> ";

    private final InputStream inputStream;
    private final PrintStream printStream;
    private final boolean shouldPrintPrompt;

    public REPL(InputStream inputStream, PrintStream printStream, boolean shouldPrintPrompt) {
        this.inputStream = inputStream;
        this.printStream = printStream;
        this.shouldPrintPrompt = shouldPrintPrompt;
    }

    public void start() {
        var scanner = new Scanner(inputStream);

        printPrompt();
        while (scanner.hasNextLine()) {
            var line = scanner.nextLine();
            var lexer = new Lexer(line);
            var parser = new Parser(lexer);

            try {
                Program program = parser.parseProgram();

                printStream.println(program);
            } catch (ParseProgramException e) {
                printStream.println(e.getMessage());
            }

            printStream.println();
            printPrompt();
        }
    }

    private void printPrompt() {
        if (shouldPrintPrompt) {
            printStream.print(PROMPT);
        }
    }

    public void printToken(Token token) {
        if (token.type() == TokenType.ILLEGAL) {
            throw new IllegalArgumentException("Illegal Token Provided: " + token.literal());
        }
        printStream.println(token);
    }
}
