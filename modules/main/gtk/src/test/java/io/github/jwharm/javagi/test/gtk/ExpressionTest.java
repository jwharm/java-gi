package io.github.jwharm.javagi.test.gtk;

import io.github.jwharm.javagi.gobject.JavaClosure;
import io.github.jwharm.javagi.gobject.types.Types;
import org.gnome.glib.Type;
import org.gnome.gobject.Closure;
import org.gnome.gobject.Value;
import org.gnome.gtk.ClosureExpression;
import org.gnome.gtk.ConstantExpression;
import org.gnome.gtk.Expression;
import org.junit.jupiter.api.Test;

import java.util.function.IntBinaryOperator;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Evaluate Expressions and check the result
 */
public class ExpressionTest {

    @Test
    public void testConstantExpression() {
        Value value = new Value();
        int input = 100;
        Expression expression = new ConstantExpression(Types.INT, input);
        expression.evaluate(null, value);
        int output = value.getInt();
        assertEquals(input, output);
    }

    @Test
    public void testClosureExpression() {
        Value value = new Value();
        Type integer = Types.INT;
        Closure closure = new JavaClosure((IntBinaryOperator) (a, b) -> a * b);
        Expression[] params = {
                new ConstantExpression(integer, 2),
                new ConstantExpression(integer, 3)
        };
        Expression expression = new ClosureExpression(integer, closure, params);
        expression.evaluate(null, value);
        int output = value.getInt();
        assertEquals(2 * 3, output);
    }
}
