package fi.dy.masa.worldprimer.command.parser.value;

import fi.dy.masa.worldprimer.command.substitutions.IStringProvider;

public class SubstitutionValue extends Value
{
    protected final IStringProvider stringProvider;

    public SubstitutionValue(IStringProvider stringProvider)
    {
        this.stringProvider = stringProvider;
    }

    @Override
    public String toString()
    {
        return "?";
    }
}
