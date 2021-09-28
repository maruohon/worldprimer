package fi.dy.masa.worldprimer.command.parser.operator;

import fi.dy.masa.worldprimer.command.parser.TokenType;
import fi.dy.masa.worldprimer.command.parser.token.Token;
import fi.dy.masa.worldprimer.command.parser.token.ValueToken;
import fi.dy.masa.worldprimer.command.parser.value.BooleanValue;
import fi.dy.masa.worldprimer.command.parser.value.DoubleValue;
import fi.dy.masa.worldprimer.command.parser.value.IntValue;
import fi.dy.masa.worldprimer.command.parser.value.ValueCategory;

public enum NumericRelationOperator
{
    GREATER             ((v1, v2) -> v1 > v2, (v1, v2) -> v1 > v2),
    LESS                ((v1, v2) -> v1 < v2, (v1, v2) -> v1 < v2),
    GREATER_OR_EQUAL    ((v1, v2) -> v1 >= v2, (v1, v2) -> v1 >= v2),
    LESS_OR_EQUAL       ((v1, v2) -> v1 <= v2, (v1, v2) -> v1 <= v2);

    protected final EqualityOperator.IntOp intOp;
    protected final EqualityOperator.DoubleOp doubleOp;

    NumericRelationOperator(EqualityOperator.IntOp intOp, EqualityOperator.DoubleOp doubleOp)
    {
        this.intOp = intOp;
        this.doubleOp = doubleOp;
    }

    public ValueToken reduceTerms(Token valueToken1, Token valueToken2)
    {
        TokenType valueTokenType1 = valueToken1.getType();
        TokenType valueTokenType2 = valueToken2.getType();
        ValueCategory vc1 = valueTokenType1.getValueCategory();
        ValueCategory vc2 = valueTokenType2.getValueCategory();
        boolean newValue;

        if (vc1 == ValueCategory.NUMBER && vc2 == ValueCategory.NUMBER)
        {
            if (valueTokenType1 == TokenType.CONST_INT &&
                valueTokenType2 == TokenType.CONST_INT)
            {
                int val1 = ((IntValue) valueToken1.getValue()).getValue();
                int val2 = ((IntValue) valueToken2.getValue()).getValue();
                newValue = this.intOp.apply(val1, val2);
            }
            else
            {
                double val1 = valueTokenType1 == TokenType.CONST_INT ? ((IntValue) valueToken1.getValue()).getValue() : ((DoubleValue) valueToken1.getValue()).getValue();
                double val2 = valueTokenType2 == TokenType.CONST_INT ? ((IntValue) valueToken2.getValue()).getValue() : ((DoubleValue) valueToken2.getValue()).getValue();
                newValue = this.doubleOp.apply(val1, val2);
            }

            return new ValueToken(TokenType.CONST_BOOLEAN, String.valueOf(newValue), new BooleanValue(newValue));
        }

        String msg = String.format("The NumericRelationOperator '%s' can't be applied to the arguments '%s' and '%s'",
                                   this, valueTokenType1, valueTokenType2);
        throw new IllegalArgumentException(msg);
    }
}
