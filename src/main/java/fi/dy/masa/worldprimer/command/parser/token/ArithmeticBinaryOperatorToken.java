package fi.dy.masa.worldprimer.command.parser.token;

import fi.dy.masa.worldprimer.command.parser.operator.ArithmeticBinaryOperator;

public class ArithmeticBinaryOperatorToken extends Token
{
    protected final ArithmeticBinaryOperator operator;

    public ArithmeticBinaryOperatorToken(TokenType type, String originalString, ArithmeticBinaryOperator operator)
    {
        super(type, originalString);
        this.operator = operator;
    }

    public ArithmeticBinaryOperator getOperator()
    {
        return this.operator;
    }
}
