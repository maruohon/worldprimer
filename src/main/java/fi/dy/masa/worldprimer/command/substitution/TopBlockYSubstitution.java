package fi.dy.masa.worldprimer.command.substitution;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import net.minecraft.world.World;
import fi.dy.masa.worldprimer.util.WorldUtils;

public class TopBlockYSubstitution extends BaseSubstitution
{
    private static final Pattern PATTERN_TOP_Y = Pattern.compile("(?<x>-?[0-9]+),(?<z>-?[0-9]+)");

    public TopBlockYSubstitution()
    {
        super("TOP_Y", true, true);
    }

    @Override
    public boolean isArgumentValid(String argumentString)
    {
        return PATTERN_TOP_Y.matcher(argumentString).matches();
    }

    @Override
    public String getString(CommandContext context)
    {
        return this.getOriginalFullSubstitutionString();
    }

    @Override
    @Nullable
    public BaseSubstitution buildSubstitution(String argumentString)
    {
        Matcher matcher = PATTERN_TOP_Y.matcher(argumentString);

        if (matcher.matches())
        {
            try
            {
                int x = Integer.parseInt(matcher.group("x"));
                int z = Integer.parseInt(matcher.group("z"));
                return new TopBlockYSubstitutionArgs(argumentString, x, z);
            }
            catch (Exception ignore) {}
        }

        return null;
    }

    protected static class TopBlockYSubstitutionArgs extends ArgumentSubstitution
    {
        protected final int x;
        protected final int z;

        protected TopBlockYSubstitutionArgs(String argumentString, int x, int z)
        {
            super("TOP_Y", argumentString, true, false);

            this.x = x;
            this.z = z;
        }

        @Override
        public String getString(CommandContext ctx)
        {
            World world = ctx.getWorld();
    
            if (world != null)
            {
                return String.valueOf(WorldUtils.getTopYAt(world, this.x, this.z));
            }
    
            return this.getOriginalFullSubstitutionString();
        }
    }
}
