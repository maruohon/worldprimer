package fi.dy.masa.worldprimer.command.substitution;

import java.util.function.Function;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import fi.dy.masa.worldprimer.util.Coordinate;

public class SpawnPointSubstitution extends BaseSubstitution
{
    protected final Function<World, BlockPos> positionFetcher;
    protected final Coordinate coordinate;

    public SpawnPointSubstitution(String substitutionName,
                                  Function<World, BlockPos> positionFetcher,
                                  Coordinate coordinate)
    {
        super(substitutionName, true, false);

        this.positionFetcher = positionFetcher;
        this.coordinate = coordinate;
    }

    @Override
    public String getString(CommandContext ctx)
    {
        World world = ctx.getWorld();

        if (world != null)
        {
            BlockPos spawn = this.positionFetcher.apply(world);

            if (spawn != null)
            {
                return this.coordinate.getCoordinateAsIntString(spawn);
            }
        }

        return this.getOriginalFullSubstitutionString();
    }
}
