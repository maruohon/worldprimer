package fi.dy.masa.worldprimer.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import org.apache.commons.lang3.tuple.Pair;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import fi.dy.masa.worldprimer.WorldPrimer;

public class CommandSubstitutions
{
    private static final Random RAND = new Random();
    private static final Pattern PATTERN_RAND_DOUBLE  = Pattern.compile(".*(\\{RAND:(?<min>-?(?:[0-9]+\\.)[0-9]+),(?<max>-?(?:[0-9]+\\.)[0-9]+)\\}).*");
    private static final Pattern PATTERN_RAND_INT     = Pattern.compile(".*(\\{RAND:(?<min>-?[0-9]+),(?<max>-?[0-9]+)\\}).*");
    private static final Pattern PATTERN_TOP_Y        = Pattern.compile(".*(\\{TOP_Y:(?<x>-?[0-9]+),(?<z>-?[0-9]+)\\}).*");
    private static final Pattern PATTERN_TOP_Y_RAND   = Pattern.compile(".*(\\{TOP_Y_RAND:(?<x>-?[0-9]+),(?<z>-?[0-9]+);(?<rx>[0-9]+),(?<rz>[0-9]+)\\}).*");
    private static final Pattern PATTERN_NUMBER_START = Pattern.compile("([0-9]+)(.*)");
    private static final Map<Pair<Integer, BlockPos>, Integer> TOP_Y_CACHE = new HashMap<>();

    public static String doCommandSubstitutions(@Nullable EntityPlayer player, @Nullable World world, String originalCommand)
    {
        if (world == null)
        {
            WorldPrimer.logger.error("World was null when trying to do command substitutions, so didn't do any!");
            return originalCommand;
        }

        String[] parts = originalCommand.split(" ");
        parts = substituteParts(player, world, parts);

        return String.join(" ", parts);
    }

    private static String[] substituteParts(@Nullable EntityPlayer player, World world, String[] parts)
    {
        for (int index = 0; index < parts.length; index++)
        {
            parts[index] = substituteIn(player, world, parts[index]);
        }

        return parts;
    }

    private static String substituteIn(@Nullable EntityPlayer player, World world, String argument)
    {
        final int start = getFirstPlaceholderPosition(argument);

        if (start != -1)
        {
            String substituted = substitutePlaceholder(player, world, argument.substring(start, argument.length()));

            if (start > 0)
            {
                return argument.substring(0, start) + substituted;
            }
            else
            {
                return substituted;
            }
        }

        return argument;
    }

    private static int getFirstPlaceholderPosition(String argument)
    {
        int pos = argument.indexOf("{");

        if (pos > 0)
        {
            final int len = argument.length();

            // Find the next "{" that is not escaped by a backslash
            while (pos != -1 && pos < (len - 1) && argument.charAt(pos - 1) == '\\')
            {
                pos = argument.indexOf("{", pos + 1);
            }
        }

        return pos;
    }

    private static String substitutePlaceholder(@Nullable EntityPlayer player, World world, String argument)
    {
        // Need to temporarily wrap the arg in an array to emulate C pointers,
        // because the return value is a boolean to be able to return early.
        final String[] wrapper = new String[] { argument };

        final int dim = world.provider.getDimension();
        final BlockPos spawn = WorldUtils.getWorldSpawn(world);

        if (substituteNumber(player, world, wrapper, "{DIMENSION}", dim))           { return wrapper[0]; }
        if (substituteNumber(player, world, wrapper, "{SPAWN_X}", spawn.getX()))    { return wrapper[0]; }
        if (substituteNumber(player, world, wrapper, "{SPAWN_Y}", spawn.getY()))    { return wrapper[0]; }
        if (substituteNumber(player, world, wrapper, "{SPAWN_Z}", spawn.getZ()))    { return wrapper[0]; }
        if (substituteRandom(player, world, wrapper))                               { return wrapper[0]; }
        if (substituteTopBlockY(player, world, wrapper))                            { return wrapper[0]; }
        if (substituteTopBlockYRand(world, wrapper))                                { return wrapper[0]; }

        if (player != null)
        {
            if (substituteNumber(player, world, wrapper, "{PLAYER_X}", player.posX)) { return wrapper[0]; }
            if (substituteNumber(player, world, wrapper, "{PLAYER_Y}", player.posY)) { return wrapper[0]; }
            if (substituteNumber(player, world, wrapper, "{PLAYER_Z}", player.posZ)) { return wrapper[0]; }
            if (substituteString(player, world, wrapper, "{PLAYER_NAME}", player.getName())) { return wrapper[0]; }
        }

        return argument;
    }

    private static boolean substituteString(@Nullable EntityPlayer player, World world, String[] wrapper, String placeHolder, String value)
    {
        final String argument = wrapper[0];

        if (argument.equals(placeHolder))
        {
            wrapper[0] = value;
            return true;
        }
        else if (argument.startsWith(placeHolder))
        {
            String tail = argument.substring(placeHolder.length(), argument.length());
            wrapper[0] = value + tail;
            return true;
        }

        return false;
    }

    private static boolean substituteNumber(@Nullable EntityPlayer player, World world, String[] wrapper, String placeHolder, double value)
    {
        final String argument = wrapper[0];

        // No arithmetic operations following the substituted string
        if (argument.equals(placeHolder))
        {
            wrapper[0] = getStringForValue(value);
            return true;
        }
        else if (argument.startsWith(placeHolder))
        {
            String tail = argument.substring(placeHolder.length(), argument.length());
            final String supportedOperations = "+-*/";

            if (tail.length() > 1)
            {
                final char op = tail.charAt(0);

                // Some arithmetic operation follows
                if (supportedOperations.indexOf(op) != -1)
                {
                    // Do any remaining substitutions to the rest of the argument recursively
                    tail = substituteIn(player, world, tail.substring(1, tail.length()));

                    Matcher matcher = PATTERN_NUMBER_START.matcher(tail);

                    if (matcher.matches())
                    {
                        final String operandValueStr = matcher.group(1);

                        try
                        {
                            double operandValue = Double.parseDouble(operandValueStr);

                            switch (op)
                            {
                                case '+':
                                case '-':
                                    value += operandValue;
                                    break;

                                case '*':
                                    value *= operandValue;
                                    break;

                                case '/':
                                    value /= operandValue;
                                    break;
                            }

                            wrapper[0] = getStringForValue(value) + matcher.group(2);

                            return true;
                        }
                        catch (NumberFormatException e)
                        {
                            WorldPrimer.logger.warn("Failed to parse relative argument '{}'", argument, e);
                        }
                    }
                }
                // An escaped arithmetic operator, remove the escape character
                else if (op == '\\' && supportedOperations.indexOf(tail.charAt(1)) != -1)
                {
                    tail = tail.substring(1, tail.length());
                }
                else
                {
                    tail = substituteIn(player, world, tail);
                }
            }

            wrapper[0] = getStringForValue(value) + tail;

            return true;
        }

        return false;
    }

    private static String getStringForValue(double value)
    {
        return Math.floor(value) == value ? String.valueOf((long) value) : String.valueOf(value);
    }

    private static boolean substituteRandom(@Nullable EntityPlayer player, World world, String[] wrapper)
    {
        final String argument = wrapper[0];
        Matcher matcher;

        matcher = PATTERN_RAND_INT.matcher(argument);

        if (matcher.matches())
        {
            try
            {
                final int min = Integer.parseInt(matcher.group("min"));
                final int max = Integer.parseInt(matcher.group("max"));
                final int value = min + RAND.nextInt(max - min);

                substituteNumber(player, world, wrapper, matcher.group(1), value);

                return true;
            }
            catch (NumberFormatException e)
            {
                WorldPrimer.logger.warn("Failed to parse random min or max value for argument: {}", argument);
            }
        }

        matcher = PATTERN_RAND_DOUBLE.matcher(argument);

        if (matcher.matches())
        {
            try
            {
                final double min = Double.parseDouble(matcher.group("min"));
                final double max = Double.parseDouble(matcher.group("max"));
                final double value = min + RAND.nextDouble() * (max - min);

                substituteNumber(player, world, wrapper, matcher.group(1), value);

                return true;
            }
            catch (NumberFormatException e)
            {
                WorldPrimer.logger.warn("Failed to parse random min or max value for argument: {}", argument);
            }
        }

        return false;
    }

    private static boolean substituteTopBlockY(@Nullable EntityPlayer player, World world, String[] wrapper)
    {
        final String argument = wrapper[0];
        final Matcher matcher = PATTERN_TOP_Y.matcher(argument);

        if (matcher.matches())
        {
            try
            {
                final int x = Integer.parseInt(matcher.group("x"));
                final int z = Integer.parseInt(matcher.group("z"));
                final int value = getTopYAt(world, x, z);

                substituteNumber(player, world, wrapper, matcher.group(1), value);

                return true;
            }
            catch (NumberFormatException e)
            {
                WorldPrimer.logger.warn("Failed to parse arguments for TOP_Y substitution '{}'", argument);
            }
        }

        return false;
    }

    private static boolean substituteTopBlockYRand(World world, String[] wrapper)
    {
        final String argument = wrapper[0];
        final Matcher matcher = PATTERN_TOP_Y_RAND.matcher(argument);

        if (matcher.matches())
        {
            try
            {
                final int rx = Integer.parseInt(matcher.group("rx"));
                final int rz = Integer.parseInt(matcher.group("rz"));
                final int x = Integer.parseInt(matcher.group("x")) + - rx + RAND.nextInt(rx * 2);
                final int z = Integer.parseInt(matcher.group("z")) + - rz + RAND.nextInt(rz * 2);
                final int y = getTopYAt(world, x, z);
                String placeHolder = matcher.group(1);
                String result = String.format("%d %d %d", x, y, z);

                if (argument.equals(placeHolder))
                {
                    wrapper[0] = result;
                    return true;
                }
                else if (argument.startsWith(placeHolder))
                {
                    String tail = argument.substring(placeHolder.length(), argument.length());
                    wrapper[0] = result + tail;
                    return true;
                }
            }
            catch (NumberFormatException e)
            {
                WorldPrimer.logger.warn("Failed to parse arguments for TOP_Y_RAND substitution '{}'", argument);
            }
        }

        return false;
    }

    private static int getTopYAt(World world, int x, int z)
    {
        final Pair<Integer, BlockPos> pair = Pair.of(world.provider.getDimension(), new BlockPos(x, 0, z));
        final Integer topY = TOP_Y_CACHE.get(pair);

        if (topY != null)
        {
            return topY.intValue();
        }

        // Load an area of 3x3 chunks around the target location, to generate the world and structures
        WorldUtils.loadChunks(world, (x >> 4) - 1, (z >> 4) - 1, (x >> 4) + 1, (z >> 4) + 1);

        // world.getTopSolidOrLiquidBlock() will return -1 over void
        final int top = Math.max(0, world.getTopSolidOrLiquidBlock(new BlockPos(x, 0, z)).getY());
        TOP_Y_CACHE.put(pair, Integer.valueOf(top));

        WorldUtils.unloadLoadedChunks(world);

        return top;
    }

    public static void clearTopYCache()
    {
        TOP_Y_CACHE.clear();
    }
}
