package fi.dy.masa.worldprimer.command.parser.token;

import fi.dy.masa.worldprimer.command.parser.TokenType;
import fi.dy.masa.worldprimer.command.parser.operator.EqualityOperator;

public class EqualityOperatorToken extends Token
{
    protected final EqualityOperator operator;

    public EqualityOperatorToken(TokenType type, String originalString, EqualityOperator operator)
    {
        super(type, originalString);

        this.operator = operator;
    }

    public EqualityOperator getOperator()
    {
        return this.operator;
    }
}
