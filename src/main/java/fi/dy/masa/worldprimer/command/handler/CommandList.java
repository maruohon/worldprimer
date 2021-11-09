package fi.dy.masa.worldprimer.command.handler;

import com.google.common.collect.ImmutableList;
import fi.dy.masa.worldprimer.WorldPrimer;
import fi.dy.masa.worldprimer.command.parser.ExpressionParser;
import fi.dy.masa.worldprimer.command.substitution.CommandContext;

public class CommandList
{
    protected final String name;
    protected final ImmutableList<ParsedCommand> commands;

    public CommandList(String name, ImmutableList<ParsedCommand> commands)
    {
        this.name = name;
        this.commands = commands;
    }

    public void execute(CommandContext ctx, ExpressionParser parser)
    {
        WorldPrimer.logInfo("Executing the commands for '{}'", this.name);

        for (ParsedCommand cmd : this.commands)
        {
            cmd.execute(ctx, parser);
        }
    }
}
