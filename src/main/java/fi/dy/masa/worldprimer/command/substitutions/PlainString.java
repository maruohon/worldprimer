package fi.dy.masa.worldprimer.command.substitutions;

public class PlainString implements IStringProvider
{
    protected final String string;

    public PlainString(String string)
    {
        this.string = string;
    }

    @Override
    public String getString(CommandContext context, String original)
    {
        return this.string;
    }
}
