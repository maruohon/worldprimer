package fi.dy.masa.worldprimer.command.substitutions;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import fi.dy.masa.worldprimer.util.Coordinate;
import fi.dy.masa.worldprimer.util.WorldUtils;

public class SubstitutionPlayerPosition extends SubstitutionBase
{
    protected final Type type;
    protected final Coordinate coordinate;

    public SubstitutionPlayerPosition(Type type, Coordinate coordinate)
    {
        super(true, false);

        this.type = type;
        this.coordinate = coordinate;
    }

    @Override
    public String getString(CommandContext context, String original)
    {
        EntityPlayer player = context.getPlayer();

        if (player != null)
        {
            if (this.type == Type.EXACT_POSITION)
            {
                return String.valueOf(this.coordinate.getDoublePos(player.getPositionVector()));
            }
            else if (this.type == Type.BED_SPAWN_POSITION)
            {
                BlockPos bedSpawnPos = WorldUtils.getPlayerBedSpawnLocation(player);

                if (bedSpawnPos != null)
                {
                    return String.valueOf(this.coordinate.getIntPos(bedSpawnPos));
                }
            }
            else if (this.type == Type.BED_POSITION)
            {
                BlockPos bedPos = player.getBedLocation(player.dimension);

                if (bedPos != null)
                {
                    return String.valueOf(this.coordinate.getIntPos(bedPos));
                }
            }
            else if (this.type == Type.BLOCK_POSITION)
            {
                return String.valueOf(this.coordinate.getIntPos(player.getPosition()));
            }
        }

        return original;
    }

    public enum Type
    {
        BLOCK_POSITION,
        EXACT_POSITION,
        BED_POSITION,
        BED_SPAWN_POSITION;
    }
}
