package fi.dy.masa.worldprimer.command.substitution;

public class PlainString implements StringSubstitution
{
    protected final String string;

    public PlainString(String string)
    {
        this.string = string;
    }

    @Override
    public String evaluate(CommandContext context)
    {
        return this.string;
    }
}
