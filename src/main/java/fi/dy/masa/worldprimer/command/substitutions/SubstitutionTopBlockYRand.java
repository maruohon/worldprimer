package fi.dy.masa.worldprimer.command.substitutions;

import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import net.minecraft.world.World;

public class SubstitutionTopBlockYRand extends SubstitutionBase
{
    private static final Random RAND = new Random();
    private static final Pattern PATTERN_TOP_Y_RAND = Pattern.compile("TOP_Y_RAND:(?<x>-?[0-9]+),(?<z>-?[0-9]+);(?<rx>[0-9]+),(?<rz>[0-9]+)");

    protected final int centerX;
    protected final int centerZ;
    protected final int rangeX;
    protected final int rangeZ;

    public SubstitutionTopBlockYRand()
    {
        this(0, 0, 0, 0);
    }

    protected SubstitutionTopBlockYRand(int centerX, int centerZ, int rangeX, int rangeZ)
    {
        super(true, true);

        this.centerX = centerX;
        this.centerZ = centerZ;
        this.rangeX = rangeX;
        this.rangeZ = rangeZ;
    }

    @Override
    public boolean isValid(String originalSubstitutionString)
    {
        return PATTERN_TOP_Y_RAND.matcher(originalSubstitutionString).matches();
    }

    @Override
    @Nullable
    public SubstitutionBase buildSubstitution(String originalSubstitutionString)
    {
        Matcher matcher = PATTERN_TOP_Y_RAND.matcher(originalSubstitutionString);

        if (matcher.matches())
        {
            final int x = Integer.parseInt(matcher.group("x"));
            final int z = Integer.parseInt(matcher.group("z"));
            final int rx = Integer.parseInt(matcher.group("rx"));
            final int rz = Integer.parseInt(matcher.group("rz"));

            return new SubstitutionTopBlockYRand(x, z, rx, rz);
        }

        return null;
    }

    @Override
    public String getString(CommandContext context, String original)
    {
        World world = this.getWorldFromContext(context);

        if (world != null)
        {
            int rx = this.rangeX + 1;
            int rz = this.rangeZ + 1;
            int x = this.centerX - RAND.nextInt(rx) + RAND.nextInt(rx);
            int z = this.centerZ - RAND.nextInt(rz) + RAND.nextInt(rz);

            return String.valueOf(SubstitutionTopBlockY.getTopYAt(world, x, z));
        }

        return original;
    }
}
