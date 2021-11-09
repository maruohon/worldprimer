package fi.dy.masa.worldprimer.command.parser.value;

import fi.dy.masa.worldprimer.command.substitution.StringSubstitution;

public class SubstitutionValue extends Value
{
    protected final StringSubstitution stringProvider;

    public SubstitutionValue(StringSubstitution stringProvider)
    {
        this.stringProvider = stringProvider;
    }

    public StringSubstitution getSubstitution()
    {
        return this.stringProvider;
    }

    @Override
    public String toString()
    {
        return "?";
    }
}
