package fi.dy.masa.worldprimer.command.substitution;

import net.minecraft.world.World;

public class WorldTimeSubstitution extends BaseSubstitution
{
    protected final WorldTimeSource worldTimeSource;

    public WorldTimeSubstitution(String substitutionName,
                                 WorldTimeSource worldTimeSource)
    {
        super(substitutionName, true, false);

        this.worldTimeSource = worldTimeSource;
    }

    @Override
    public String getString(CommandContext ctx)
    {
        World world = ctx.getWorld();

        if (world != null)
        {
            return String.valueOf(this.worldTimeSource.getTime(world));
        }

        return this.getOriginalFullSubstitutionString();
    }

    public interface WorldTimeSource
    {
        long getTime(World world);
    }
}
