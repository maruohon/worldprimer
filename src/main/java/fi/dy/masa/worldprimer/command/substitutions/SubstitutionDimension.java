package fi.dy.masa.worldprimer.command.substitutions;

import net.minecraft.world.World;

public class SubstitutionDimension extends SubstitutionBase
{
    public SubstitutionDimension()
    {
        super(true, false);
    }

    @Override
    public String getString(CommandContext context, String original)
    {
        World world = this.getWorldFromContext(context);
        return world != null ? String.valueOf(world.provider.getDimension()) : original;
    }
}
