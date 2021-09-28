package fi.dy.masa.worldprimer.command.parser.token;

import fi.dy.masa.worldprimer.command.parser.operator.NumericRelationOperator;

public class NumericRelationOperatorToken extends Token
{
    protected final NumericRelationOperator operator;

    public NumericRelationOperatorToken(TokenType type, String originalString, NumericRelationOperator operator)
    {
        super(type, originalString);

        this.operator = operator;
    }

    public NumericRelationOperator getOperator()
    {
        return this.operator;
    }
}
