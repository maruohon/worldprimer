package fi.dy.masa.worldprimer.command.parser.token;

import fi.dy.masa.worldprimer.command.parser.operator.BooleanBinaryOperator;
import fi.dy.masa.worldprimer.command.parser.TokenType;

public class BooleanBinaryOperatorToken extends Token
{
    protected final BooleanBinaryOperator operator;

    public BooleanBinaryOperatorToken(TokenType type, String originalString, BooleanBinaryOperator operator)
    {
        super(type, originalString);

        this.operator = operator;
    }

    public BooleanBinaryOperator getOperator()
    {
        return this.operator;
    }
}
