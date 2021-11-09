package fi.dy.masa.worldprimer.command.handler;

import fi.dy.masa.worldprimer.WorldPrimer;
import fi.dy.masa.worldprimer.command.parser.ExpressionParser;
import fi.dy.masa.worldprimer.command.substitution.CommandContext;

public class ConditionalCommand extends ParsedCommand
{
    protected final Expression conditionExpression;

    public ConditionalCommand(ParsedCommand baseCommand,
                              Expression conditionExpression)
    {
        super(baseCommand.commandParts, baseCommand.originalString);

        this.conditionExpression = conditionExpression;
    }

    @Override
    protected void logExecutionDebugMessage(String commandStr, String originalStr)
    {
        WorldPrimer.logInfo("ConditionalCommand: condition: '{}' passed",
                            this.conditionExpression.getOriginalString());
        super.logExecutionDebugMessage(commandStr, originalStr);
    }

    @Override
    public void execute(CommandContext ctx, ExpressionParser parser)
    {
        try
        {
            if (this.conditionExpression.evaluateAsBoolean(ctx, parser))
            {
                super.execute(ctx, parser);
            }
        }
        catch (Exception e)
        {
            WorldPrimer.LOGGER.warn("Failed to evaluate command condition '{}' for command '{}'",
                                    this.conditionExpression.getOriginalString(),
                                    this.getOriginalString());
        }
    }
}
