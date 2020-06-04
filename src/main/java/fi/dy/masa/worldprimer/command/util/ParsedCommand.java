package fi.dy.masa.worldprimer.command.util;

import java.util.List;
import fi.dy.masa.worldprimer.command.substitutions.CommandContext;
import fi.dy.masa.worldprimer.command.substitutions.IStringProvider;

public class ParsedCommand
{
    protected final List<IStringProvider> commandParts;

    public ParsedCommand(List<IStringProvider> commandParts)
    {
        this.commandParts = commandParts;
    }

    public String getCommand(CommandContext context, String original)
    {
        StringBuilder sb = new StringBuilder();

        for (IStringProvider provider : this.commandParts)
        {
            sb.append(provider.getString(context, original));
        }

        return sb.toString();
    }
}
