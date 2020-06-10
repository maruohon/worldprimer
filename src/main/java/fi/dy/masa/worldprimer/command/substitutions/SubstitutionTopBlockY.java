package fi.dy.masa.worldprimer.command.substitutions;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import fi.dy.masa.worldprimer.util.WorldUtils;

public class SubstitutionTopBlockY extends SubstitutionBase
{
    private static final Pattern PATTERN_TOP_Y = Pattern.compile("TOP_Y:(?<x>-?[0-9]+),(?<z>-?[0-9]+)");

    protected final int x;
    protected final int z;

    public SubstitutionTopBlockY()
    {
        this(0, 0);
    }

    protected SubstitutionTopBlockY(int x, int z)
    {
        super(true, true);

        this.x = x;
        this.z = z;
    }

    @Override
    public boolean isValid(String originalSubstitutionString)
    {
        return PATTERN_TOP_Y.matcher(originalSubstitutionString).matches();
    }

    @Override
    @Nullable
    public SubstitutionBase buildSubstitution(String originalSubstitutionString)
    {
        Matcher matcher = PATTERN_TOP_Y.matcher(originalSubstitutionString);

        if (matcher.matches())
        {
            int x = Integer.parseInt(matcher.group("x"));
            int z = Integer.parseInt(matcher.group("z"));
            return new SubstitutionTopBlockY(x, z);
        }

        return null;
    }

    @Override
    public String getString(CommandContext context, String original)
    {
        World world = this.getWorldFromContext(context);
        return world != null ? String.valueOf(getTopYAt(world, this.x, this.z)) : original;
    }

    public static int getTopYAt(World world, int x, int z)
    {
        // Load an area of 3x3 chunks around the target location, to generate the world and structures
        WorldUtils.loadChunks(world, (x >> 4) - 1, (z >> 4) - 1, (x >> 4) + 1, (z >> 4) + 1);

        // world.getTopSolidOrLiquidBlock() will return -1 over void
        final int top = Math.max(0, world.getTopSolidOrLiquidBlock(new BlockPos(x, 0, z)).getY());

        WorldUtils.unloadLoadedChunks(world);

        return top;
    }
}
