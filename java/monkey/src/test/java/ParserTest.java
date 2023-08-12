import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import root.TokenType;
import root.ast.Program;
import root.ast.expressions.*;
import root.ast.statements.ExpressionStatement;
import root.ast.statements.LetStatement;
import root.ast.statements.ReturnStatement;
import root.ast.statements.Statement;
import root.lexer.Lexer;
import root.parser.Parser;

import java.util.List;

public class ParserTest {

    @Test
    void testLetStatements() {
        var input = """
                let x = 5;
                let y = 10;
                let foobar = 838383;""";

        var program = buildProgram(input);

        Assertions.assertEquals(3, program.getStatements().size());

        var expectedIdentifiers = List.of("x", "y", "foobar");

        var index = 0;
        for (var statement : program.getStatements()) {
            testLetStatement(statement, expectedIdentifiers.get(index++));
        }
    }

    @Test
    void testReturnStatements() {
        var input = """
                return 5;
                return 10;
                return 993322;""";

        var program = buildProgram(input);

        Assertions.assertEquals(3, program.getStatements().size());

        for (var statement : program.getStatements()) {
            Assertions.assertTrue(statement instanceof ReturnStatement);
            Assertions.assertEquals(TokenType.RETURN.token().literal(), statement.tokenLiteral());
        }
    }

    @Test
    void testString() {
        var program = new Program() {
            {
                getStatements().add(
                        new LetStatement(TokenType.LET.token()) {
                            {
                                setName(new IdentifierExpression(TokenType.IDENT.createToken("myVar"), "myVar"));
                                setValue(new IdentifierExpression(TokenType.IDENT.createToken("anotherVar"), "anotherVar"));
                            }
                        }
                );
            }
        };

        Assertions.assertEquals("let myVar = anotherVar;", program.toString());
    }

    @Test
    void TestIdentifierExpression() {
        var input = "foobar;";

        var program = buildProgram(input);

        Assertions.assertEquals(1, program.getStatements().size());

        Assertions.assertInstanceOf(ExpressionStatement.class, program.getStatements().get(0));
        var statement = (ExpressionStatement) program.getStatements().get(0);

        testIdentifier(statement.getExpression(), "foobar");
    }

    @Test
    void testIntegerLiteralExpression() {
        var input = "5;";

        var program = buildProgram(input);

        Assertions.assertEquals(1, program.getStatements().size());

        Assertions.assertInstanceOf(ExpressionStatement.class, program.getStatements().get(0));
        var statement = (ExpressionStatement) program.getStatements().get(0);

        testIntegerLiteral(statement.getExpression(), 5L);
    }

    @Test
    void testBooleanLiteralExpression() {
        var input = "true; false;";

        var program = buildProgram(input);

        Assertions.assertEquals(2, program.getStatements().size());

        Assertions.assertInstanceOf(ExpressionStatement.class, program.getStatements().get(0));
        Assertions.assertInstanceOf(ExpressionStatement.class, program.getStatements().get(1));

        testBooleanLiteral(((ExpressionStatement) program.getStatements().get(0)).getExpression(), true);
        testBooleanLiteral(((ExpressionStatement) program.getStatements().get(1)).getExpression(), false);
    }

    private record PrefixTestRecord(String input, String operator, Object right) {
    }

    @Test
    void testParsingPrefixExpressions() {
        var prefixTests = List.of(
                new PrefixTestRecord("!5", "!", 5),
                new PrefixTestRecord("- 15;", "-", 15),
                new PrefixTestRecord("!true", "!", true),
                new PrefixTestRecord("!false", "!", false)
        );

        for (PrefixTestRecord(String input, String operator, Object right) : prefixTests) {
            Program program = buildProgram(input);

            Assertions.assertEquals(1, program.getStatements().size());

            Assertions.assertInstanceOf(ExpressionStatement.class, program.getStatements().get(0));
            var statement = (ExpressionStatement) program.getStatements().get(0);

            testPrefixExpression(statement.getExpression(), operator, right);
        }
    }

    private record InfixTestRecord(String input, Object leftValue, String operator, Object rightValue) {
    }

    @Test
    void testParsingInfixExpressions() {
        var infixTests = List.of(
                new InfixTestRecord("5 + 5;", 5, "+", 5),
                new InfixTestRecord("3 + 10", 3, "+", 10),
                new InfixTestRecord("5 - 5;", 5, "-", 5),
                new InfixTestRecord("5 * 5;", 5, "*", 5),
                new InfixTestRecord("5 / 5;", 5, "/", 5),
                new InfixTestRecord("5 > 5;", 5, ">", 5),
                new InfixTestRecord("5 < 5;", 5, "<", 5),
                new InfixTestRecord("0 < 83", 0, "<", 83),
                new InfixTestRecord("5 == 5;", 5, "==", 5),
                new InfixTestRecord("5 != 5;", 5, "!=", 5),
                new InfixTestRecord("true == true", true, "==", true),
                new InfixTestRecord("true != false;", true, "!=", false),
                new InfixTestRecord("false == false", false, "==", false)
        );

        for (InfixTestRecord(String input, Object leftValue, String operator, Object rightValue) : infixTests) {
            var program = buildProgram(input);

            Assertions.assertEquals(1, program.getStatements().size());

            Assertions.assertInstanceOf(ExpressionStatement.class, program.getStatements().get(0));
            var statement = (ExpressionStatement) program.getStatements().get(0);

            testInfixExpression(statement.getExpression(), leftValue, operator, rightValue);
        }
    }

    @Test
    void testOperatorPrecedenceParsing() {
        var tests = List.of(
                List.of(
                        "-a * b",
                        "((-a) * b)"
                ),
                List.of(
                        "!-a",
                        "(!(-a))"
                ),
                List.of(
                        "a + b + c",
                        "((a + b) + c)"
                ),
                List.of(
                        "a + b - c",
                        "((a + b) - c)"
                ),
                List.of(
                        "a * b * c",
                        "((a * b) * c)"
                ),
                List.of(
                        "a * b / c",
                        "((a * b) / c)"
                ),
                List.of(
                        "a + b / c",
                        "(a + (b / c))"
                ),
                List.of(
                        "a + b * c + d / e - f",
                        "(((a + (b * c)) + (d / e)) - f)"
                ),
                List.of(
                        "3 + 4; -5 * 5",
                        "(3 + 4)\n((-5) * 5)"
                ),
                List.of(
                        "5 > 4 == 3 < 4",
                        "((5 > 4) == (3 < 4))"
                ),
                List.of(
                        "5 < 4 != 3 > 4",
                        "((5 < 4) != (3 > 4))"
                ),
                List.of(
                        "3 + 4 * 5 == 3 * 1 + 4 * 5",
                        "((3 + (4 * 5)) == ((3 * 1) + (4 * 5)))"
                ),
                List.of(
                        "3 + 4 * 5 == 3 * 1 + 4 * 5",
                        "((3 + (4 * 5)) == ((3 * 1) + (4 * 5)))"
                ),
                List.of(
                        "true",
                        "true"
                ),
                List.of(
                        "false",
                        "false"
                ),
                List.of(
                        "3 > 5 == false",
                        "((3 > 5) == false)"
                ),
                List.of(
                        "3 < 5 == true",
                        "((3 < 5) == true)"
                ),
                List.of(
                        "1 + (2 + 3) + 4",
                        "((1 + (2 + 3)) + 4)"
                ),
                List.of(
                        "(5 + 5) * 2",
                        "((5 + 5) * 2)"
                ),
                List.of(
                        "2 / (5 + 5)",
                        "(2 / (5 + 5))"
                ),
                List.of(
                        "-(5 + 5)",
                        "(-(5 + 5))"
                ),
                List.of(
                        "!(true == true)",
                        "(!(true == true))"
                )
        );

        for (List<String> test : tests) {
            var program = buildProgram(test.get(0));

            Assertions.assertEquals(test.get(1), program.toString());
        }
    }

    @Test
    void testIfExpression() {
        var input = "if (x < y) { x }";
        var program = buildProgram(input);

        Assertions.assertEquals(1, program.getStatements().size());
        Assertions.assertInstanceOf(ExpressionStatement.class, program.getStatements().get(0));
        ExpressionStatement statement = (ExpressionStatement) program.getStatements().get(0);

        Assertions.assertInstanceOf(IfExpression.class, statement.getExpression());
        IfExpression expression = (IfExpression) statement.getExpression();
        testInfixExpression(expression.getCondition(), "x", "<", "y");

        Assertions.assertEquals(1, expression.getConsequence().getStatements().size());
        Assertions.assertInstanceOf(ExpressionStatement.class, expression.getConsequence().getStatements().get(0));
        ExpressionStatement consequence = (ExpressionStatement) expression.getConsequence().getStatements().get(0);
        testIdentifier(consequence.getExpression(), "x");

        Assertions.assertNull(expression.getAlternative());

        Assertions.assertEquals("if (x < y) {\nx\n}", expression.toString());

        input = "if (y > x) { x } else { y }";
        program = buildProgram(input);

        Assertions.assertEquals(1, program.getStatements().size());
        Assertions.assertInstanceOf(ExpressionStatement.class, program.getStatements().get(0));
        statement = (ExpressionStatement) program.getStatements().get(0);

        Assertions.assertInstanceOf(IfExpression.class, statement.getExpression());
        expression = (IfExpression) statement.getExpression();
        testInfixExpression(expression.getCondition(), "y", ">", "x");

        Assertions.assertEquals(1, expression.getConsequence().getStatements().size());
        Assertions.assertInstanceOf(ExpressionStatement.class, expression.getConsequence().getStatements().get(0));
        consequence = (ExpressionStatement) expression.getConsequence().getStatements().get(0);
        testIdentifier(consequence.getExpression(), "x");

        Assertions.assertNotNull(expression.getAlternative());
        Assertions.assertEquals(1, expression.getAlternative().getStatements().size());
        Assertions.assertInstanceOf(ExpressionStatement.class, expression.getAlternative().getStatements().get(0));
        ExpressionStatement alternative = (ExpressionStatement) expression.getAlternative().getStatements().get(0);
        testIdentifier(alternative.getExpression(), "y");

        Assertions.assertEquals("if (y > x) {\nx\n} else {\ny\n}", expression.toString());
    }

    @Test
    void testFunctionLiteralParsing() {
        var input = "fn(x, y) { x + y; }";
        var program = buildProgram(input);

        Assertions.assertEquals(1, program.getStatements().size());
        Assertions.assertInstanceOf(ExpressionStatement.class, program.getStatements().get(0));

        ExpressionStatement expressionStatement = (ExpressionStatement) program.getStatements().get(0);

        Assertions.assertInstanceOf(FunctionLiteralExpression.class, expressionStatement.getExpression());

        FunctionLiteralExpression function = (FunctionLiteralExpression) expressionStatement.getExpression();

        Assertions.assertEquals(2, function.getParameters().size());
        testLiteralExpression(function.getParameters().get(0), "x");
        testLiteralExpression(function.getParameters().get(1), "y");

        Assertions.assertEquals(1, function.getBody().getStatements().size());

        Assertions.assertInstanceOf(ExpressionStatement.class, function.getBody().getStatements().get(0));

        ExpressionStatement expression = (ExpressionStatement) function.getBody().getStatements().get(0);

        testInfixExpression(expression.getExpression(),"x", "+", "y");

        Assertions.assertEquals("fn(x, y) {\n(x + y)\n}", function.toString());
    }

    private static record FunctionParameterTest(String input, List<String> expectedParams) {}

    @Test
    void testFunctionParameterParsing() {
        var tests = List.of(
                new FunctionParameterTest("fn () {};", List.of()),
                new FunctionParameterTest("fn (x) {};", List.of("x")),
                new FunctionParameterTest("fn (x, y, z) {};", List.of("x", "y", "z"))
        );

        for (FunctionParameterTest(String input, List<String> expectedParams) : tests) {
            var program = buildProgram(input);
            var function = (FunctionLiteralExpression) ((ExpressionStatement) program.getStatements().get(0)).getExpression();

            Assertions.assertEquals(expectedParams.size(), function.getParameters().size());

            for (int i = 0; i < expectedParams.size(); i++) {
                testLiteralExpression(function.getParameters().get(i), expectedParams.get(i));
            }
        }
    }

    private void testIntegerLiteral(Expression expression, Long expectedValue) {
        if (expression instanceof IntegerLiteralExpression integerLiteralExpression) {
            Assertions.assertEquals(expectedValue, integerLiteralExpression.getValue());
            Assertions.assertEquals(expectedValue.toString(), integerLiteralExpression.tokenLiteral());
        } else {
            throw new AssertionError(expression.getClass().getSimpleName() + " is not instance of IntegerLiteralExpression");
        }
    }

    private void testIdentifier(Expression expression, String value) {
        if (expression instanceof IdentifierExpression identifierExpression) {
            Assertions.assertEquals(value, identifierExpression.getValue());
            Assertions.assertEquals(value, identifierExpression.tokenLiteral());
        } else {
            throw new AssertionError(expression.getClass().getSimpleName() + " is not instance of IdentifierExpression");
        }
    }

    private void testBooleanLiteral(Expression expression, Boolean value) {
        if (expression instanceof BooleanLiteralExpression booleanLiteralExpression) {
            Assertions.assertEquals(value, booleanLiteralExpression.getValue());
            Assertions.assertEquals(value.toString(), booleanLiteralExpression.tokenLiteral());
        } else {
            throw new AssertionError(expression.getClass().getSimpleName() + " is not instance of IdentifierExpression");
        }
    }

    private void testLiteralExpression(Expression expression, Object expected) {
        switch (expected) {
            case Integer i -> testIntegerLiteral(expression, i.longValue());
            case Long i -> testIntegerLiteral(expression, i);
            case String s -> testIdentifier(expression, s);
            case Boolean b -> testBooleanLiteral(expression, b);
            default -> throw new AssertionError("Type of exp not handled. got=" + expected.getClass().getSimpleName());
        }
    }

    private void testInfixExpression(Expression expression, Object left, String operator, Object right) {
        if (expression instanceof InfixExpression infixExpression) {
            testLiteralExpression(infixExpression.getLeft(), left);
            Assertions.assertEquals(operator, infixExpression.getOperator());
            testLiteralExpression(infixExpression.getRight(), right);
        } else {
            throw new AssertionError(expression.getClass().getSimpleName() + " is not instance of infixExpression");
        }
    }

    private void testPrefixExpression(Expression expression, String operator, Object right) {
        if (expression instanceof PrefixExpression prefixExpression) {
            Assertions.assertEquals(operator, prefixExpression.getOperator());
            testLiteralExpression(prefixExpression.getRight(), right);
        } else {
            throw new AssertionError(expression.getClass().getSimpleName() + " is not instance of prefixExpression");
        }
    }

    private Program buildProgram(String input) {
        var l = new Lexer(input);
        var p = new Parser(l);
        var program = p.parseProgram();

        checkParseErrors(p);

        return program;
    }

    private void testLetStatement(Statement statement, String name) {
        Assertions.assertEquals(TokenType.LET.token().literal(), statement.tokenLiteral());

        if (statement instanceof LetStatement letStatement) {
            Assertions.assertEquals(name, letStatement.getName().getValue());
            Assertions.assertEquals(name, letStatement.getName().tokenLiteral());
        } else {
            throw new AssertionError(statement.getClass().getSimpleName() + " is not instance of LetStatement");
        }
    }

    private void checkParseErrors(Parser p) {
        if (!p.errors.isEmpty()) {
            StringBuilder errorMessage = new StringBuilder("Parser encountered errors:\n");

            for (var error : p.errors) {
                errorMessage.append(error).append("\n");
            }

            throw new AssertionError(errorMessage.toString());
        }
    }
}
