package fi.dy.masa.worldprimer.command.substitution;

import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import net.minecraft.world.World;
import fi.dy.masa.worldprimer.util.WorldUtils;

public class TopBlockYRandSubstitution extends BaseSubstitution
{
    private static final Random RAND = new Random();
    private static final Pattern PATTERN_TOP_Y_RAND = Pattern.compile("(?<x>-?[0-9]+),(?<z>-?[0-9]+);(?<rx>[0-9]+),(?<rz>[0-9]+)");

    public TopBlockYRandSubstitution(String substitutionName)
    {
        super(substitutionName, true);
    }

    @Override
    public String evaluate(CommandContext context)
    {
        return this.getOriginalFullSubstitutionString();
    }

    @Override
    @Nullable
    public BaseSubstitution buildSubstitution(String argumentString)
    {
        Matcher matcher = PATTERN_TOP_Y_RAND.matcher(argumentString);

        if (matcher.matches())
        {
            final int x = Integer.parseInt(matcher.group("x"));
            final int z = Integer.parseInt(matcher.group("z"));
            final int rx = Integer.parseInt(matcher.group("rx"));
            final int rz = Integer.parseInt(matcher.group("rz"));

            return new TopBlockYRandSubstitutionArgs(argumentString, x, z, rx, rz);
        }

        return null;
    }

    protected static class TopBlockYRandSubstitutionArgs extends ArgumentSubstitution
    {
        protected final int centerX;
        protected final int centerZ;
        protected final int rangeX;
        protected final int rangeZ;

        protected TopBlockYRandSubstitutionArgs(String argumentString, int centerX, int centerZ, int rangeX, int rangeZ)
        {
            super("TOP_Y_RAND", argumentString, false);

            this.centerX = centerX;
            this.centerZ = centerZ;
            this.rangeX = rangeX;
            this.rangeZ = rangeZ;
        }

        @Override
        public String evaluate(CommandContext ctx)
        {
            World world = ctx.getWorld();
    
            if (world != null)
            {
                int rx = this.rangeX + 1;
                int rz = this.rangeZ + 1;
                int x = this.centerX - RAND.nextInt(rx) + RAND.nextInt(rx);
                int z = this.centerZ - RAND.nextInt(rz) + RAND.nextInt(rz);
    
                return String.valueOf(WorldUtils.getTopYAt(world, x, z));
            }
    
            return this.getOriginalFullSubstitutionString();
        }
    }
}
