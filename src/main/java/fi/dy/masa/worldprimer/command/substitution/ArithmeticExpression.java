package fi.dy.masa.worldprimer.command.substitution;

import fi.dy.masa.worldprimer.WorldPrimer;
import fi.dy.masa.worldprimer.command.handler.Expression;
import fi.dy.masa.worldprimer.command.parser.ExpressionParser;

public class ArithmeticExpression implements StringSubstitution
{
    protected final ExpressionParser expressionParser;
    protected final Expression expression;
    protected final String originalString;

    public ArithmeticExpression(Expression expression, ExpressionParser expressionParser)
    {
        this.expression = expression;
        this.originalString = expression.getOriginalString();
        this.expressionParser = expressionParser;
    }

    @Override
    public String getString(CommandContext ctx)
    {
        try
        {
            return this.expression.evaluateAsString(ctx, this.expressionParser);
        }
        catch (Exception e)
        {
            WorldPrimer.LOGGER.warn("Failed to evaluate expression '{}'", this.originalString);
        }

        return this.originalString;
    }
}
