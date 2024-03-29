package fi.dy.masa.worldprimer.command.substitution;

import javax.annotation.Nullable;

public abstract class BaseSubstitution implements StringSubstitution
{
    private final String substitutionName;
    protected final boolean hasArguments;

    protected BaseSubstitution(String substitutionName, boolean hasArguments)
    {
        this.substitutionName = substitutionName;
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

    public final boolean hasArguments()
    {
        return this.hasArguments;
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
}
