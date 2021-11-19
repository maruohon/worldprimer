package fi.dy.masa.worldprimer.command.substitution;

import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;

public class RandomNumberSubstitution extends BaseSubstitution
{
    protected static final Pattern PATTERN_RAND_DOUBLE = Pattern.compile("(?<min>-?(?:[0-9]+\\.)?[0-9]+),(?<max>-?(?:[0-9]+\\.)?[0-9]+)");
    protected static final Pattern PATTERN_RAND_INT    = Pattern.compile("(?<min>-?[0-9]+),(?<max>-?[0-9]+)");

    public RandomNumberSubstitution(String substitutionName)
    {
        super(substitutionName, true);
    }

    @Override
    @Nullable
    public BaseSubstitution buildSubstitution(String argumentString)
    {
        Matcher matcher = PATTERN_RAND_INT.matcher(argumentString);

        if (matcher.matches())
        {
            int min = Integer.parseInt(matcher.group("min"));
            int max = Integer.parseInt(matcher.group("max"));
            return new RandomIntSubstitution(argumentString, min, max);
        }

        matcher = PATTERN_RAND_DOUBLE.matcher(argumentString);

        if (matcher.matches())
        {
            double min = Double.parseDouble(matcher.group("min"));
            double max = Double.parseDouble(matcher.group("max"));
            return new RandomDoubleSubstitution(argumentString, min, max);
        }

        return null;
    }

    @Override
    public String evaluate(CommandContext context)
    {
        return this.getOriginalFullSubstitutionString();
    }

    public static class RandomIntSubstitution extends ArgumentSubstitution
    {
        protected static final Random RAND = new Random();

        protected final int minValue;
        protected final int range;

        public RandomIntSubstitution(String argumentString, int min, int max)
        {
            super("RAND", argumentString, false);

            this.minValue = min;
            this.range = max - min + 1;
        }

        @Override
        public String evaluate(CommandContext context)
        {
            return String.valueOf(this.minValue + RAND.nextInt(this.range));
        }
    }

    public static class RandomDoubleSubstitution extends ArgumentSubstitution
    {
        protected static final Random RAND = new Random();

        protected final double minValue;
        protected final double range;

        public RandomDoubleSubstitution(String argumentString, double min, double max)
        {
            super("RAND", argumentString, false);

            this.minValue = min;
            this.range = max - min;
        }

        @Override
        public String evaluate(CommandContext context)
        {
            return String.valueOf(this.minValue + RAND.nextDouble() * this.range);
        }
    }
}
