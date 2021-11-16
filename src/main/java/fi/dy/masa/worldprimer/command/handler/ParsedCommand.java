package fi.dy.masa.worldprimer.command.handler;

import java.util.Objects;
import com.google.common.collect.ImmutableList;
import fi.dy.masa.worldprimer.WorldPrimer;
import fi.dy.masa.worldprimer.command.parser.ExpressionParser;
import fi.dy.masa.worldprimer.command.substitution.CommandContext;
import fi.dy.masa.worldprimer.command.substitution.StringSubstitution;
import fi.dy.masa.worldprimer.command.util.WorldPrimerCommandSender;

public class ParsedCommand
{
    protected final ImmutableList<StringSubstitution> commandParts;
    protected final String originalString;

    public ParsedCommand(ImmutableList<StringSubstitution> commandParts, String originalString)
    {
        this.commandParts = commandParts;
        this.originalString = originalString;
    }

    public String getOriginalString()
    {
        return this.originalString;
    }

    public String getCommand(CommandContext ctx)
    {
        if (this.commandParts.size() == 1)
        {
            return this.commandParts.get(0).evaluate(ctx);
        }

        StringBuilder sb = new StringBuilder();

        for (StringSubstitution provider : this.commandParts)
        {
            sb.append(provider.evaluate(ctx));
        }

        return sb.toString();
    }

    protected String getExecutionDebugMessage(String commandStr, String originalStr)
    {
        if (Objects.equals(commandStr, originalStr))
        {
            return String.format("Executing command '%s'", commandStr);
        }
        else
        {
            return String.format("Executing substituted command '%s' (original: '%s')",
                                 commandStr, originalStr);
        }
    }

    protected void logExecutionDebugMessage(String commandStr, String originalStr)
    {
        WorldPrimer.logInfo(this.getExecutionDebugMessage(commandStr, originalStr));
    }

    public void execute(CommandContext ctx, ExpressionParser parser)
    {
        String commandStr = this.getCommand(ctx);
        this.logExecutionDebugMessage(commandStr, this.getOriginalString());
        WorldPrimerCommandSender.INSTANCE.executeCommand(commandStr, ctx.getWorld());
    }
}
