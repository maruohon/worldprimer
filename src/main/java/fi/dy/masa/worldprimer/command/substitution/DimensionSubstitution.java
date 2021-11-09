package fi.dy.masa.worldprimer.command.substitution;

import java.util.OptionalInt;

public class DimensionSubstitution extends BaseSubstitution
{
    public DimensionSubstitution()
    {
        super("DIMENSION", true, false);
    }

    @Override
    public String getString(CommandContext ctx)
    {
        OptionalInt dimension = ctx.getEventDimension();
        return dimension.isPresent() ? String.valueOf(dimension.getAsInt()) : this.getOriginalFullSubstitutionString();
    }
}
