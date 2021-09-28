package fi.dy.masa.worldprimer.command.parser.operator;

import fi.dy.masa.worldprimer.command.parser.token.TokenType;
import fi.dy.masa.worldprimer.command.parser.token.Token;
import fi.dy.masa.worldprimer.command.parser.token.ValueToken;
import fi.dy.masa.worldprimer.command.parser.value.BooleanValue;
import fi.dy.masa.worldprimer.command.parser.value.DoubleValue;
import fi.dy.masa.worldprimer.command.parser.value.IntValue;
import fi.dy.masa.worldprimer.command.parser.value.StringValue;
import fi.dy.masa.worldprimer.command.parser.value.ValueCategory;

public enum EqualityOperator
{
    EQUAL       ((v1, v2) -> v1 == v2, (v1, v2) -> v1 == v2, (v1, v2) -> v1 == v2, String::equals),
    NOT_EQUAL   ((v1, v2) -> v1 != v2, (v1, v2) -> v1 != v2, (v1, v2) -> v1 != v2, (v1, v2) -> ! v1.equals(v2));

    protected final BooleanOp booleanOp;
    protected final IntOp intOp;
    protected final DoubleOp doubleOp;
    protected final StringOp stringOp;

    EqualityOperator(BooleanOp booleanOp, IntOp intOp, DoubleOp doubleOp, StringOp stringOp)
    {
        this.booleanOp = booleanOp;
        this.intOp = intOp;
        this.doubleOp = doubleOp;
        this.stringOp = stringOp;
    }

    public ValueToken reduceTerms(Token valueToken1, Token valueToken2)
    {
        TokenType valueTokenType1 = valueToken1.getType();
        TokenType valueTokenType2 = valueToken2.getType();
        ValueCategory vc1 = valueTokenType1.getValueCategory();
        ValueCategory vc2 = valueTokenType2.getValueCategory();
        boolean newValue;

        if (vc1 == ValueCategory.BOOLEAN && vc2 == ValueCategory.BOOLEAN)
        {
            boolean val1 = ((BooleanValue) valueToken1.getValue()).getValue();
            boolean val2 = ((BooleanValue) valueToken2.getValue()).getValue();
            newValue = this.booleanOp.apply(val1, val2);
        }
        else if (vc1 == ValueCategory.NUMBER && vc2 == ValueCategory.NUMBER)
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
        }
        else if (vc1 == ValueCategory.STRING && vc2 == ValueCategory.STRING)
        {
            String val1 = ((StringValue) valueToken1.getValue()).getValue();
            String val2 = ((StringValue) valueToken2.getValue()).getValue();
            newValue = this.stringOp.apply(val1, val2);
        }
        else
        {
            String msg = String.format("The EqualityOperator '%s' can't be applied to the arguments '%s' and '%s'",
                                       this, valueTokenType1, valueTokenType2);
            throw new IllegalArgumentException(msg);
        }

        return new ValueToken(TokenType.CONST_BOOLEAN, String.valueOf(newValue), new BooleanValue(newValue));
    }

    protected interface BooleanOp
    {
        boolean apply(boolean val1, boolean val2);
    }

    protected interface IntOp
    {
        boolean apply(int val1, int val2);
    }

    protected interface DoubleOp
    {
        boolean apply(double val1, double val2);
    }

    protected interface StringOp
    {
        boolean apply(String val1, String val2);
    }
}
