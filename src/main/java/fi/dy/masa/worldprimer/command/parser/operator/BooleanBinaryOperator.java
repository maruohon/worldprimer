package fi.dy.masa.worldprimer.command.parser.operator;

import fi.dy.masa.worldprimer.command.parser.TokenType;
import fi.dy.masa.worldprimer.command.parser.token.Token;
import fi.dy.masa.worldprimer.command.parser.token.ValueToken;
import fi.dy.masa.worldprimer.command.parser.value.BooleanValue;
import fi.dy.masa.worldprimer.command.parser.value.ValueCategory;

public enum BooleanBinaryOperator
{
    AND  ((v1, v2) -> v1 && v2),
    OR   ((v1, v2) -> v1 || v2);

    private final EqualityOperator.BooleanOp operator;

    BooleanBinaryOperator(EqualityOperator.BooleanOp operator)
    {
        this.operator = operator;
    }

    public ValueToken reduceTerms(Token valueToken1, Token valueToken2)
    {
        TokenType valueTokenType1 = valueToken1.getType();
        TokenType valueTokenType2 = valueToken2.getType();
        ValueCategory vc1 = valueTokenType1.getValueCategory();
        ValueCategory vc2 = valueTokenType2.getValueCategory();

        if (vc1 == ValueCategory.BOOLEAN && vc2 == ValueCategory.BOOLEAN)
        {
            boolean val1 = ((BooleanValue) valueToken1.getValue()).getValue();
            boolean val2 = ((BooleanValue) valueToken2.getValue()).getValue();
            boolean newValue = this.operator.apply(val1, val2);
            return new ValueToken(TokenType.CONST_BOOLEAN, String.valueOf(newValue), new BooleanValue(newValue));
        }

        throw new IllegalArgumentException("The BooleanBinaryOperator '" + this +
                                           "' can't be applied to the arguments '" + valueTokenType1 + "'" +
                                           " and '" + valueTokenType2 + "'");
    }
}
