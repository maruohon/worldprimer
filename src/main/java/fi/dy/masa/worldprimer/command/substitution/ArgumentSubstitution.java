package fi.dy.masa.worldprimer.command.substitution;

public abstract class ArgumentSubstitution extends BaseSubstitution
{
    private final String argumentsString;

    protected ArgumentSubstitution(String substitutionName,
                                   String argumentsString,
                                   boolean isNumeric,
                                   boolean hasArguments)
    {
        super(substitutionName, isNumeric, hasArguments);

        this.argumentsString = argumentsString;
    }

    @Override
    public String getOriginalFullSubstitutionString()
    {
        return String.format("{%s:%s}", this.getSubstitutionName(), this.argumentsString);
    }
}
