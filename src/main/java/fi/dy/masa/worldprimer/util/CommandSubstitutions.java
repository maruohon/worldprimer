package fi.dy.masa.worldprimer.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import org.apache.commons.lang3.tuple.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import fi.dy.masa.worldprimer.WorldPrimer;

public class CommandSubstitutions
{
    private static final Random RAND = new Random();
    private static final Pattern PATTERN_RAND_DOUBLE  = Pattern.compile(".*(\\{RAND:(?<min>[0-9]+\\.[0-9]+),(?<max>[0-9]+\\.[0-9]+)\\}).*");
    private static final Pattern PATTERN_RAND_INT     = Pattern.compile(".*(\\{RAND:(?<min>[0-9]+),(?<max>[0-9]+)\\}).*");
    private static final Pattern PATTERN_TOP_Y        = Pattern.compile(".*(\\{TOP_Y:(?<x>[0-9]+),(?<z>[0-9]+)\\}).*");
    private static final Pattern PATTERN_NUMBER_START = Pattern.compile("([0-9]+)(.*)");
    private static final Map<Pair<Integer, BlockPos>, Integer> TOP_Y_CACHE = new HashMap<>();

    public static String doCommandSubstitutions(@Nullable World world, String originalCommand)
    {
        if (world == null)
        {
            WorldPrimer.logger.error("World was null when trying to do command substitutions, so didn't do any!");
            return originalCommand;
        }

        String[] parts = originalCommand.split(" ");
        parts = substituteParts(world, parts);

        return String.join(" ", parts);
    }

    private static String[] substituteParts(World world, String[] parts)
    {
        for (int index = 0; index < parts.length; index++)
        {
            parts[index] = substituteIn(world, parts[index]);
        }

        return parts;
    }

    private static String substituteIn(World world, String argument)
    {
        final int start = getFirstPlaceholderPosition(argument);

        if (start != -1)
        {
            String substituted = substitutePlaceholder(world, argument.substring(start, argument.length()));

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

    private static String substitutePlaceholder(World world, String argument)
    {
        // Need to temporarily wrap the arg in an array to emulate C pointers,
        // because the return value is a boolean to be able to return early.
        final String[] wrapper = new String[] { argument };

        final int dim = world.provider.getDimension();
        final BlockPos spawn = WorldUtils.getWorldSpawn(world);

        if (substituteNumber(world, wrapper, "{DIMENSION}", dim))
            return wrapper[0];

        if (substituteNumber(world, wrapper, "{SPAWNX}", spawn.getX()))
            return wrapper[0];

        if (substituteNumber(world, wrapper, "{SPAWNY}", spawn.getY()))
            return wrapper[0];

        if (substituteNumber(world, wrapper, "{SPAWNZ}", spawn.getZ()))
            return wrapper[0];

        if (substituteRandom(world, wrapper))
            return wrapper[0];

        if (substituteTopBlockY(world, wrapper))
            return wrapper[0];

        return argument;
    }

    private static boolean substituteNumber(World world, String[] wrapper, String placeHolder, double value)
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
                    tail = substituteIn(world, tail.substring(1, tail.length()));

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
                    tail = substituteIn(world, tail);
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

    private static boolean substituteRandom(World world, String[] wrapper)
    {
        final String argument = wrapper[0];
        Matcher matcher = PATTERN_RAND_DOUBLE.matcher(argument);

        if (matcher.matches())
        {
            try
            {
                final double min = Double.parseDouble(matcher.group("min"));
                final double max = Double.parseDouble(matcher.group("max"));
                final double value = min + RAND.nextDouble() * (max - min);
                substituteNumber(world, wrapper, matcher.group(1), value);
            }
            catch (NumberFormatException e)
            {
                WorldPrimer.logger.warn("Failed to parse random min or max value for argument: {}", argument);
            }

            return true;
        }

        matcher = PATTERN_RAND_INT.matcher(argument);

        if (matcher.matches())
        {
            try
            {
                final int min = Integer.parseInt(matcher.group("min"));
                final int max = Integer.parseInt(matcher.group("max"));
                final int value = min + RAND.nextInt(max - min);
                substituteNumber(world, wrapper, matcher.group(1), value);
            }
            catch (NumberFormatException e)
            {
                WorldPrimer.logger.warn("Failed to parse random min or max value for argument: {}", argument);
            }

            return true;
        }

        return false;
    }

    private static boolean substituteTopBlockY(World world, String[] wrapper)
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

                substituteNumber(world, wrapper, matcher.group(1), value);
            }
            catch (NumberFormatException e)
            {
                WorldPrimer.logger.warn("Failed to parse block x or z coordinate for TOP_Y argument: {}", argument);
            }

            return true;
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

        final int top = world.getTopSolidOrLiquidBlock(new BlockPos(x, 0, z)).getY();
        TOP_Y_CACHE.put(pair, Integer.valueOf(top));

        WorldUtils.unloadLoadedChunks(world);

        return top;
    }

    public static void clearTopYCache()
    {
        TOP_Y_CACHE.clear();
    }
}
