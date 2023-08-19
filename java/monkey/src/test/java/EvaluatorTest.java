import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import root.evaluation.Evaluator;
import root.evaluation.objects.MonkeyObject;
import root.evaluation.objects.impl.MonkeyBoolean;
import root.evaluation.objects.impl.MonkeyInteger;
import root.evaluation.objects.impl.MonkeyNull;
import root.lexer.Lexer;
import root.parser.ParseProgramException;
import root.parser.Parser;

import java.util.List;

public class EvaluatorTest {

    private record IntegerTest(long expected, String input) {
    }

    @Test
    void testIntegerExpression() {
        var tests = List.of(
                new IntegerTest(5, "5"),
                new IntegerTest(10, "10"),
                new IntegerTest(0, "0"),
                new IntegerTest(-5, "-5"),
                new IntegerTest(-10, "-10"),
                new IntegerTest(100, "--100"),
                new IntegerTest(10, "5 + 5 + 5 + 5 - 10"),
                new IntegerTest(32, "2 * 2 * 2 * 2 * 2"),
                new IntegerTest(0, "-50 + 100 + -50"),
                new IntegerTest(20, "5 * 2 + 10"),
                new IntegerTest(25, "5 + 2 * 10"),
                new IntegerTest(0, "20 + 2 * -10"),
                new IntegerTest(60, "50 / 2 * 2 + 10"),
                new IntegerTest(30, "2 * (5 + 10)"),
                new IntegerTest(37, "3 * 3 * 3 + 10"),
                new IntegerTest(37, "3 * (3 * 3) + 10"),
                new IntegerTest(50, "(5 + 10 * 2 + 15 / 3) * 2 + -10")
        );

        for (IntegerTest(long expected, String input) : tests) {
            testIntegerObject(expected, testEval(input));
        }
    }

    private record BooleanTest(boolean expected, String input) {
    }

    @Test
    void testBooleanExpression() {
        var tests = List.of(
                new BooleanTest(true, "true"),
                new BooleanTest(false, "false"),
                new BooleanTest(true, "1 < 2"),
                new BooleanTest(false, "1 > 2"),
                new BooleanTest(false, "1 < 1"),
                new BooleanTest(false, "1 > 1"),
                new BooleanTest(true, "1 == 1"),
                new BooleanTest(false, "1 != 1"),
                new BooleanTest(false, "1 == 2"),
                new BooleanTest(true, "1 != 2"),
                new BooleanTest(true, "true == true"),
                new BooleanTest(true, "false == false"),
                new BooleanTest(false, "true == false"),
                new BooleanTest(true, "true != false"),
                new BooleanTest(true, "false != true"),
                new BooleanTest(true, "(1 < 2) == true"),
                new BooleanTest(false, "(1 < 2) == false"),
                new BooleanTest(false, "(1 > 2) == true"),
                new BooleanTest(true, "(1 > 2) == false")
        );

        for (BooleanTest(boolean expected, String input) : tests) {
            testBooleanObject(expected, testEval(input));
        }
    }

    @Test
    void testMinusOperator() {
        var tests = List.of(
                new BooleanTest(false, "!true"),
                new BooleanTest(true, "!false"),
                new BooleanTest(false, "!5"),
                new BooleanTest(true, "!!5"),
                new BooleanTest(true, "!!5")
        );

        for (BooleanTest(boolean expected, String input) : tests) {
            testBooleanObject(expected, testEval(input));
        }
    }

    private record ExpressionTest(Object expected, String input) {
    }

    @Test
    void testIfElseExpression() {
        var tests = List.of(
                new ExpressionTest(10l, "if (true) { 10 }"),
                new ExpressionTest(null, "if (false) { 10 }"),
                new ExpressionTest(10l, "if (1) { 10 }"),
                new ExpressionTest(10l, "if (1 < 2) { 10 }"),
                new ExpressionTest(null, "if (1 > 2) { 10 }"),
                new ExpressionTest(20l, "if (1 > 2) { 10 } else { 20 }"),
                new ExpressionTest(10l, "if (1 < 2) { 10 } else { 20 }")
        );

        for (ExpressionTest(Object expected, String input) : tests) {
            MonkeyObject<?> evaluated = testEval(input);

            switch (expected) {
                case Long l -> testIntegerObject(l, evaluated);
                case Boolean b -> testBooleanObject(b, evaluated);
                case null -> testNullObject(evaluated);
                default -> throw new IllegalStateException("Unexpected value: " + expected);
            }
        }
    }

    @Test
    void testReturnStatement() {
        var tests = List.of(
                new IntegerTest(10, "return 10;"),
                new IntegerTest(10, "return 10; 9;"),
                new IntegerTest(10, "return 2 * 5; 9;"),
                new IntegerTest(10, "9; return 2 * 5; 9;"),
                new IntegerTest(10, """
                        if (10 > 1) {
                            if (10 > 1) {
                                return 10;
                            }
                        
                            return 1;
                        }""")
        );

        for (IntegerTest(long expected, String input) : tests) {
            MonkeyObject<?> evaluated = testEval(input);
            testIntegerObject(expected, evaluated);
        }
    }

    private MonkeyObject<?> testEval(String input) {
        var l = new Lexer(input);
        var p = new Parser(l);

        try {
            return Evaluator.eval(p.parseProgram());
        } catch (ParseProgramException e) {
            throw new RuntimeException(e);
        }
    }

    private void testIntegerObject(long expected, MonkeyObject<?> object) {
        switch (object) {
            case MonkeyInteger integer -> Assertions.assertEquals(expected, integer.getValue());
            default ->
                    throw new AssertionError("Object is not MonkeyInteger: " + object);
        }
    }

    private void testBooleanObject(boolean expected, MonkeyObject<?> object) {
        switch (object) {
            case MonkeyBoolean bool -> Assertions.assertEquals(expected, bool.getValue());
            default ->
                    throw new AssertionError("Object is not MonkeyBoolean: " + object);
        }
    }

    private void testNullObject(MonkeyObject<?> object) {
        if (object != MonkeyNull.INSTANCE) {
            throw new AssertionError("Object is not MonkeyNull: " + object);
        }
    }
}
