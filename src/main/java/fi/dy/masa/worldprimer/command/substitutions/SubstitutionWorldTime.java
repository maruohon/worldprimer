package fi.dy.masa.worldprimer.command.substitutions;

import net.minecraft.world.World;

public class SubstitutionWorldTime extends SubstitutionBase
{
    protected final ITimeSource timeSource;

    public SubstitutionWorldTime(ITimeSource timeSource)
    {
        super(true, false);

        this.timeSource = timeSource;
    }

    @Override
    public String getString(CommandContext context, String original)
    {
        World world = this.getWorldFromContext(context);

        if (world != null)
        {
            return String.valueOf(this.timeSource.getTime(world));
        }

        return original;
    }

    public interface ITimeSource
    {
        long getTime(World world);
    }
}
