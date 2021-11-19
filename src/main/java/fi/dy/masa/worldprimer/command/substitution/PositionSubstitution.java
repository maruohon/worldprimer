package fi.dy.masa.worldprimer.command.substitution;

import java.util.function.Function;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import fi.dy.masa.worldprimer.util.Coordinate;

public class PositionSubstitution extends BaseSubstitution
{
    protected final Function<World, BlockPos> positionFetcher;
    protected final Coordinate coordinate;

    public PositionSubstitution(String substitutionName,
                                Function<World, BlockPos> positionFetcher,
                                Coordinate coordinate)
    {
        super(substitutionName, false);

        this.positionFetcher = positionFetcher;
        this.coordinate = coordinate;
    }

    @Override
    public String evaluate(CommandContext ctx)
    {
        World world = ctx.getWorld();

        if (world != null)
        {
            BlockPos pos = this.positionFetcher.apply(world);

            if (pos != null)
            {
                return this.coordinate.getCoordinateAsIntString(pos);
            }
        }

        return this.getOriginalFullSubstitutionString();
    }
}
