package fi.dy.masa.worldprimer.command.substitution;

import java.util.HashMap;
import javax.annotation.Nullable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import fi.dy.masa.worldprimer.command.substitution.PlayerPositionSubstitution.PlayerPositionType;
import fi.dy.masa.worldprimer.util.Coordinate;
import fi.dy.masa.worldprimer.util.WorldUtils;

public class SubstitutionRegistry
{
    public static final SubstitutionRegistry INSTANCE = new SubstitutionRegistry();

    private final HashMap<String, BaseSubstitution> substitutions = new HashMap<>();

    public SubstitutionRegistry()
    {
        this.init();
    }

    public void init()
    {
        this.substitutions.clear();

        this.register(new IntegerSubstitution("COUNT", CommandContext::getCount));
        this.register(new IntegerSubstitution("DIMENSION", CommandContext::getEventDimension));

        this.register(new PositionSubstitution("SPAWN_X", WorldUtils::getWorldSpawn, Coordinate.X));
        this.register(new PositionSubstitution("SPAWN_Y", WorldUtils::getWorldSpawn, Coordinate.Y));
        this.register(new PositionSubstitution("SPAWN_Z", WorldUtils::getWorldSpawn, Coordinate.Z));

        this.register(new PositionSubstitution("SPAWN_POINT_X", World::getSpawnPoint, Coordinate.X));
        this.register(new PositionSubstitution("SPAWN_POINT_Y", World::getSpawnPoint, Coordinate.Y));
        this.register(new PositionSubstitution("SPAWN_POINT_Z", World::getSpawnPoint, Coordinate.Z));

        this.register(new WorldTimeSubstitution("TIME_TICK", World::getTotalWorldTime));
        this.register(new WorldTimeSubstitution("TIME_TICK_DAY", World::getWorldTime));

        this.register(new RandomNumberSubstitution());
        this.register(new RealTimeSubstitution());
        this.register(new TopBlockYRandSubstitution());
        this.register(new TopBlockYSubstitution());

        this.register(new PlayerAttributeSubstitution("PLAYER_NAME", EntityPlayer::getName));
        this.register(new PlayerAttributeSubstitution("PLAYER_UUID", (p) -> p.getUniqueID().toString()));

        this.register(new PlayerPositionSubstitution("PLAYER_BED_X", PlayerPositionType.BED_POSITION, Coordinate.X));
        this.register(new PlayerPositionSubstitution("PLAYER_BED_Y", PlayerPositionType.BED_POSITION, Coordinate.Y));
        this.register(new PlayerPositionSubstitution("PLAYER_BED_Z", PlayerPositionType.BED_POSITION, Coordinate.Z));

        this.register(new PlayerPositionSubstitution("PLAYER_BED_SPAWN_X", PlayerPositionType.BED_SPAWN_POSITION, Coordinate.X));
        this.register(new PlayerPositionSubstitution("PLAYER_BED_SPAWN_Y", PlayerPositionType.BED_SPAWN_POSITION, Coordinate.Y));
        this.register(new PlayerPositionSubstitution("PLAYER_BED_SPAWN_Z", PlayerPositionType.BED_SPAWN_POSITION, Coordinate.Z));

        this.register(new PlayerPositionSubstitution("PLAYER_BLOCK_X", PlayerPositionType.BLOCK_POSITION, Coordinate.X));
        this.register(new PlayerPositionSubstitution("PLAYER_BLOCK_Y", PlayerPositionType.BLOCK_POSITION, Coordinate.Y));
        this.register(new PlayerPositionSubstitution("PLAYER_BLOCK_Z", PlayerPositionType.BLOCK_POSITION, Coordinate.Z));

        this.register(new PlayerPositionSubstitution("PLAYER_X", PlayerPositionType.EXACT_POSITION, Coordinate.X));
        this.register(new PlayerPositionSubstitution("PLAYER_Y", PlayerPositionType.EXACT_POSITION, Coordinate.Y));
        this.register(new PlayerPositionSubstitution("PLAYER_Z", PlayerPositionType.EXACT_POSITION, Coordinate.Z));
    }

    protected void register(BaseSubstitution substitution)
    {
        this.substitutions.put(substitution.getSubstitutionName(), substitution);
    }

    @Nullable
    public BaseSubstitution getSubstitution(String name)
    {
        return this.substitutions.get(name);
    }
}
