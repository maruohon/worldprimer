package fi.dy.masa.worldprimer.command.substitution;

import java.util.function.BiFunction;
import javax.annotation.Nullable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import fi.dy.masa.worldprimer.util.Coordinate;
import fi.dy.masa.worldprimer.util.WorldUtils;

public class PlayerPositionSubstitution extends BaseSubstitution
{
    protected final PlayerPositionType playerPositionType;
    protected final Coordinate coordinate;

    public PlayerPositionSubstitution(String substitutionName,
                                      PlayerPositionType playerPositionType,
                                      Coordinate coordinate)
    {
        super(substitutionName, false);

        this.playerPositionType = playerPositionType;
        this.coordinate = coordinate;
    }

    @Override
    public String evaluate(CommandContext context)
    {
        EntityPlayer player = context.getPlayer();

        if (player != null)
        {
            String substituted = this.playerPositionType.function.apply(this.coordinate, player);
            return substituted != null ? substituted : this.getOriginalFullSubstitutionString();
        }

        return this.getOriginalFullSubstitutionString();
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

    public enum PlayerPositionType
    {
        BLOCK_POSITION      ((c, p) -> c.getCoordinateAsIntString(p.getPosition())),
        EXACT_POSITION      ((c, p) -> c.getCoordinateAsDoubleString(p.getPositionVector())),
        BED_POSITION        (PlayerPositionSubstitution::getBedLocationCoordinate),
        BED_SPAWN_POSITION  (PlayerPositionSubstitution::getBedSpawnLocationCoordinate);

        public final BiFunction<Coordinate, EntityPlayer, String> function;

        PlayerPositionType(BiFunction<Coordinate, EntityPlayer, String> function)
        {
            this.function = function;
        }
    }
}
