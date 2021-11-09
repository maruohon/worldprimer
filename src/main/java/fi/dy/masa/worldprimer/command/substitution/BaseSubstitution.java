package fi.dy.masa.worldprimer.command.substitution;

import javax.annotation.Nullable;

public abstract class BaseSubstitution implements StringSubstitution
{
    private final String substitutionName;
    protected final boolean isNumeric;
    protected final boolean hasArguments;

    protected BaseSubstitution(String substitutionName, boolean isNumeric, boolean hasArguments)
    {
        this.substitutionName = substitutionName;
        this.isNumeric = isNumeric;
        this.hasArguments = hasArguments;
    }

    public String getSubstitutionName()
    {
        return this.substitutionName;
    }

    public String getOriginalFullSubstitutionString()
    {
        return "{" + this.getSubstitutionName() + "}";
    }

    public final boolean isNumeric()
    {
        return this.isNumeric;
    }

    public final boolean hasArguments()
    {
        return this.hasArguments;
    }

    public boolean isArgumentValid(String argumentString)
    {
        return true;
    }

    /**
     * Builds the final substitution for a given substitution string.
     * This is meant for substitutions that take in arguments,
     * to allow them to parse the arguments and pre-build their final state.
     * @param argumentString the original argument string
     */
    @Nullable
    public BaseSubstitution buildSubstitution(String argumentString)
    {
        return this;
    }

    /*
    public int getIntValue(CommandContext context, String original)
    {
        if (this.isNumeric)
        {
            try
            {
                return Integer.parseInt(this.getString(context, original));
            }
            catch (NumberFormatException ignore) {}
        }

        return -1;
    }

    public double getDoubleValue(CommandContext context, String original)
    {
        if (this.isNumeric)
        {
            try
            {
                return Double.parseDouble(this.getString(context, original));
            }
            catch (NumberFormatException ignore) {}
        }

        return Double.NaN;
    }
    */
}
