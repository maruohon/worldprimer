package fi.dy.masa.worldprimer.command.substitutions;

import java.util.function.BiFunction;
import javax.annotation.Nullable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import fi.dy.masa.worldprimer.util.Coordinate;
import fi.dy.masa.worldprimer.util.WorldUtils;

public class SubstitutionPlayerPosition extends SubstitutionBase
{
    protected final PositionType positionType;
    protected final Coordinate coordinate;

    public SubstitutionPlayerPosition(PositionType positionType, Coordinate coordinate)
    {
        super(true, false);

        this.positionType = positionType;
        this.coordinate = coordinate;
    }

    @Override
    public String getString(CommandContext context, String original)
    {
        EntityPlayer player = context.getPlayer();

        if (player == null)
        {
            return original;
        }

        String substituted = this.positionType.function.apply(this.coordinate, player);
        return substituted != null ? substituted : original;
    }

    @Nullable
    public static String getBedLocationCoordinate(Coordinate coordinate, EntityPlayer player)
    {
        BlockPos bedPos = player.getBedLocation(player.dimension);

        if (bedPos != null)
        {
            return coordinate.getCoordinateAsIntString(bedPos);
        }

        return null;
    }

    @Nullable
    public static String getBedSpawnLocationCoordinate(Coordinate coordinate, EntityPlayer player)
    {
        BlockPos bedSpawnPos = WorldUtils.getPlayerBedSpawnLocation(player);

        if (bedSpawnPos != null)
        {
            return coordinate.getCoordinateAsIntString(bedSpawnPos);
        }

        return null;
    }

    public enum PositionType
    {
        BLOCK_POSITION      ((c, p) -> c.getCoordinateAsIntString(p.getPosition())),
        EXACT_POSITION      ((c, p) -> c.getCoordinateAsDoubleString(p.getPositionVector())),
        BED_POSITION        (SubstitutionPlayerPosition::getBedLocationCoordinate),
        BED_SPAWN_POSITION  (SubstitutionPlayerPosition::getBedSpawnLocationCoordinate);

        public final BiFunction<Coordinate, EntityPlayer, String> function;

        PositionType(BiFunction<Coordinate, EntityPlayer, String> function)
        {
            this.function = function;
        }
    }
}
