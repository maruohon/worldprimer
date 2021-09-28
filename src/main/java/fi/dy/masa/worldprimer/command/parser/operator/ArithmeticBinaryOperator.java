package fi.dy.masa.worldprimer.command.parser.operator;

import java.util.function.DoubleBinaryOperator;
import java.util.function.IntBinaryOperator;
import fi.dy.masa.worldprimer.command.parser.token.TokenType;
import fi.dy.masa.worldprimer.command.parser.token.Token;
import fi.dy.masa.worldprimer.command.parser.token.ValueToken;
import fi.dy.masa.worldprimer.command.parser.value.DoubleValue;
import fi.dy.masa.worldprimer.command.parser.value.IntValue;
import fi.dy.masa.worldprimer.command.parser.value.ValueCategory;

public enum ArithmeticBinaryOperator
{
    PLUS        ((v1, v2) -> v1 + v2, (v1, v2) -> v1 + v2),
    MINUS       ((v1, v2) -> v1 - v2, (v1, v2) -> v1 - v2),
    MULTIPLY    ((v1, v2) -> v1 * v2, (v1, v2) -> v1 * v2),
    DIVIDE      ((v1, v2) -> v1 / v2, (v1, v2) -> v1 / v2),
    MODULO      ((v1, v2) -> v1 % v2, (v1, v2) -> v1 % v2);

    private final DoubleBinaryOperator doubleOperator;
    private final IntBinaryOperator intOperator;

    ArithmeticBinaryOperator(IntBinaryOperator intOperator,
                             DoubleBinaryOperator doubleOperator)
    {
        this.intOperator = intOperator;
        this.doubleOperator = doubleOperator;
    }

    public ValueToken reduceTerms(Token valueToken1, Token valueToken2)
    {
        TokenType valueTokenType1 = valueToken1.getType();
        TokenType valueTokenType2 = valueToken2.getType();
        ValueCategory vc1 = valueTokenType1.getValueCategory();
        ValueCategory vc2 = valueTokenType2.getValueCategory();

        if (vc1 == ValueCategory.NUMBER && vc2 == ValueCategory.NUMBER)
        {
            if (valueTokenType1 == TokenType.CONST_INT &&
                valueTokenType2 == TokenType.CONST_INT)
            {
                int val1 = ((IntValue) valueToken1.getValue()).getValue();
                int val2 = ((IntValue) valueToken2.getValue()).getValue();
                int newValue = this.intOperator.applyAsInt(val1, val2);
                return new ValueToken(TokenType.CONST_INT, String.valueOf(newValue), new IntValue(newValue));
            }
            else
            {
                double val1 = valueTokenType1 == TokenType.CONST_INT ? ((IntValue) valueToken1.getValue()).getValue() : ((DoubleValue) valueToken1.getValue()).getValue();
                double val2 = valueTokenType2 == TokenType.CONST_INT ? ((IntValue) valueToken2.getValue()).getValue() : ((DoubleValue) valueToken2.getValue()).getValue();
                double newValue = this.doubleOperator.applyAsDouble(val1, val2);
                return new ValueToken(TokenType.CONST_DOUBLE, String.valueOf(newValue), new DoubleValue(newValue));
            }
        }

        String msg = String.format("The ArithmeticBinaryOperator '%s' can't be applied to the arguments '%s' and '%s'",
                                   this, valueTokenType1, valueTokenType2);
        throw new IllegalArgumentException(msg);
    }
}
