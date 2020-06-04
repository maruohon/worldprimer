package fi.dy.masa.worldprimer.command.substitutions;

import java.util.function.Function;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import fi.dy.masa.worldprimer.util.Coordinate;

public class SubstitutionSpawnPoint extends SubstitutionBase
{
    protected final Coordinate coordinate;
    protected final Function<World, BlockPos> positionFetcher;

    public SubstitutionSpawnPoint(Coordinate coordinate, Function<World, BlockPos> positionFetcher)
    {
        super(true, false);

        this.coordinate = coordinate;
        this.positionFetcher = positionFetcher;
    }

    @Override
    public String getString(CommandContext context, String original)
    {
        World world = context.getWorld();

        if (world == null && context.getPlayer() != null)
        {
            world = context.getPlayer().getEntityWorld();
        }

        if (world != null)
        {
            BlockPos spawn = this.positionFetcher.apply(world);

            if (spawn != null)
            {
                return String.valueOf(this.coordinate.getIntPos(spawn));
            }
        }

        return original;
    }
}
