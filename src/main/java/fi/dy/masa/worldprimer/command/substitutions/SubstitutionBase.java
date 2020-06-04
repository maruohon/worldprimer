package fi.dy.masa.worldprimer.command.substitutions;

public abstract class SubstitutionBase implements IStringProvider
{
    protected final boolean isNumeric;
    protected final boolean hasArguments;

    protected SubstitutionBase(boolean isNumeric, boolean hasArguments)
    {
        this.isNumeric = isNumeric;
        this.hasArguments = hasArguments;
    }

    public final boolean isNumeric()
    {
        return this.isNumeric;
    }

    public final boolean hasArguments()
    {
        return this.hasArguments;
    }

    public double getNumericValue(CommandContext context, String original)
    {
        try
        {
            return Double.parseDouble(this.getString(context, original));
        }
        catch (NumberFormatException e)
        {
            return Double.NaN;
        }
    }
}
