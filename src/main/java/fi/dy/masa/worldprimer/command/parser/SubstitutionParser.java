package fi.dy.masa.worldprimer.command.parser;

import java.util.HashMap;
import javax.annotation.Nullable;
import net.minecraft.world.World;
import fi.dy.masa.worldprimer.command.substitution.BaseSubstitution;
import fi.dy.masa.worldprimer.command.substitution.CountSubstitution;
import fi.dy.masa.worldprimer.command.substitution.DimensionSubstitution;
import fi.dy.masa.worldprimer.command.substitution.PlayerNameSubstitution;
import fi.dy.masa.worldprimer.command.substitution.PlayerPositionSubstitution;
import fi.dy.masa.worldprimer.command.substitution.PlayerPositionSubstitution.PlayerPositionType;
import fi.dy.masa.worldprimer.command.substitution.PlayerUuidSubstitution;
import fi.dy.masa.worldprimer.command.substitution.RandomNumberSubstitution;
import fi.dy.masa.worldprimer.command.substitution.RealTimeSubstitution;
import fi.dy.masa.worldprimer.command.substitution.SpawnPointSubstitution;
import fi.dy.masa.worldprimer.command.substitution.TopBlockYRandSubstitution;
import fi.dy.masa.worldprimer.command.substitution.TopBlockYSubstitution;
import fi.dy.masa.worldprimer.command.substitution.WorldTimeSubstitution;
import fi.dy.masa.worldprimer.util.Coordinate;
import fi.dy.masa.worldprimer.util.WorldUtils;

public class SubstitutionParser
{
    public static final SubstitutionParser INSTANCE = new SubstitutionParser();

    private final HashMap<String, BaseSubstitution> substitutions = new HashMap<>();

    public SubstitutionParser()
    {
        this.init();
    }

    public void init()
    {
        this.substitutions.clear();

        this.register(new CountSubstitution());
        this.register(new DimensionSubstitution());

        this.register(new SpawnPointSubstitution("SPAWN_X", WorldUtils::getWorldSpawn, Coordinate.X));
        this.register(new SpawnPointSubstitution("SPAWN_Y", WorldUtils::getWorldSpawn, Coordinate.Y));
        this.register(new SpawnPointSubstitution("SPAWN_Z", WorldUtils::getWorldSpawn, Coordinate.Z));

        this.register(new SpawnPointSubstitution("SPAWN_POINT_X", World::getSpawnPoint, Coordinate.X));
        this.register(new SpawnPointSubstitution("SPAWN_POINT_Y", World::getSpawnPoint, Coordinate.Y));
        this.register(new SpawnPointSubstitution("SPAWN_POINT_Z", World::getSpawnPoint, Coordinate.Z));

        this.register(new WorldTimeSubstitution("TIME_TICK", World::getTotalWorldTime));
        this.register(new WorldTimeSubstitution("TIME_TICK_DAY", World::getWorldTime));

        this.register(new RandomNumberSubstitution());
        this.register(new RealTimeSubstitution());
        this.register(new TopBlockYRandSubstitution());
        this.register(new TopBlockYSubstitution());

        this.register(new PlayerNameSubstitution());
        this.register(new PlayerUuidSubstitution());

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
    public Region getNextSubstitutionRegion(StringReader reader)
    {
        final int origPos = reader.getPos();
        int startPos = -1;
        int nesting = 0;
        Region region = null;

        while (reader.canRead())
        {
            char c = reader.peek();

            if (c == '{' && reader.peekPrevious() != '\\')
            {
                if (nesting == 0)
                {
                    startPos = reader.getPos();
                }

                ++nesting;
            }
            else if (c == '}' && nesting > 0 && --nesting == 0)
            {
                String nameAndArgs = reader.subString(startPos + 1, reader.getPos() - 1);

                // Check if the substitution is valid, and if not, discard it and continue the search
                // from the next position after the current start.
                if (this.isValidSubstitution(nameAndArgs, false))
                {
                    region = new Region(startPos, reader.getPos());
                    break;
                }
                else
                {
                    reader.setPos(startPos + 1);
                    region = null;
                    nesting = 0;
                    startPos = -1;
                    continue;
                }
            }

            reader.skip();
        }

        reader.setPos(origPos);

        return region;
    }

    @Nullable
    public Region getSubstitutionRegionStartingAt(StringReader reader, int startPos)
    {
        if (reader.peekAt(startPos) != '{')
        {
            return null;
        }

        Region region = this.getNextSubstitutionRegion(reader);
        return region != null && region.start == startPos ? region : null;
    }

    @Nullable
    public BaseSubstitution getSubstitutionStartingAt(StringReader reader, int startPos)
    {
        Region region = this.getSubstitutionRegionStartingAt(reader, startPos);
        return region != null ? this.getSubstitutionForFullRegion(reader, region, false) : null;
    }

    /*
    public boolean isValidFullSubstitution(StringReader reader, Region region)
    {
        return this.getSubstitutionForFullRegion(reader, region, false) != null;
    }
    */

    @Nullable
    public BaseSubstitution getSubstitutionForFullRegion(StringReader reader, Region region,
                                                         boolean buildFinalSubstitution)
    {
        String nameAndArgs = reader.subString(region.start + 1, region.end - 1);
        return this.getSubstitutionFor(nameAndArgs, buildFinalSubstitution);
    }

    public boolean isValidSubstitution(String nameAndArgs, boolean buildFinalSubstitution)
    {
        return this.getSubstitutionFor(nameAndArgs, buildFinalSubstitution) != null;
    }

    @Nullable
    public BaseSubstitution getSubstitutionFor(String nameAndArgs, boolean buildFinalSubstitution)
    {
        String name = nameAndArgs;
        final int colonIndex = name.indexOf(':');
        final boolean hasArgs = colonIndex != -1;

        if (hasArgs)
        {
            if (colonIndex == nameAndArgs.length() - 1)
            {
                return null;
            }

            name = name.substring(0, colonIndex);
        }

        BaseSubstitution substitution = this.substitutions.get(name);

        if (substitution != null &&
            substitution.hasArguments() == hasArgs)
        {
            if (buildFinalSubstitution)
            {
                String arg = nameAndArgs.substring(colonIndex + 1);

                if (substitution.isArgumentValid(arg))
                {
                    return substitution.buildSubstitution(arg);
                }
            }
            else
            {
                return substitution;
            }
        }

        return null;
    }
}
