package fi.dy.masa.worldprimer.command.substitutions;

import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;

public class SubstitutionRandomNumber extends SubstitutionBase
{
    protected static final Pattern PATTERN_RAND_DOUBLE = Pattern.compile("RAND:(?<min>-?(?:[0-9]+\\.)?[0-9]+),(?<max>-?(?:[0-9]+\\.)?[0-9]+)");
    protected static final Pattern PATTERN_RAND_INT    = Pattern.compile("RAND:(?<min>-?[0-9]+),(?<max>-?[0-9]+)");

    public SubstitutionRandomNumber()
    {
        super(true, true);
    }

    @Override
    public boolean isValid(String originalSubstitutionString)
    {
        return PATTERN_RAND_DOUBLE.matcher(originalSubstitutionString).matches();
    }

    @Override
    @Nullable
    public SubstitutionBase buildSubstitution(String originalSubstitutionString)
    {
        Matcher matcher = PATTERN_RAND_INT.matcher(originalSubstitutionString);

        if (matcher.matches())
        {
            int min = Integer.parseInt(matcher.group("min"));
            int max = Integer.parseInt(matcher.group("max"));
            return new SubstitutionRandomInt(min, max);
        }

        matcher = PATTERN_RAND_DOUBLE.matcher(originalSubstitutionString);

        if (matcher.matches())
        {
            double min = Double.parseDouble(matcher.group("min"));
            double max = Double.parseDouble(matcher.group("max"));
            return new SubstitutionRandomDouble(min, max);
        }

        return null;
    }

    @Override
    public String getString(CommandContext context, String original)
    {
        return original;
    }

    public static class SubstitutionRandomInt extends SubstitutionBase
    {
        protected static final Random RAND = new Random();

        protected final int minValue;
        protected final int range;

        public SubstitutionRandomInt(int min, int max)
        {
            super(true, true);

            this.minValue = min;
            this.range = max - min + 1;
        }

        @Override
        public String getString(CommandContext context, String original)
        {
            return String.valueOf(this.minValue + RAND.nextInt(this.range));
        }
    }

    public static class SubstitutionRandomDouble extends SubstitutionBase
    {
        protected static final Random RAND = new Random();

        protected final double minValue;
        protected final double range;

        public SubstitutionRandomDouble(double min, double max)
        {
            super(true, true);

            this.minValue = min;
            this.range = max - min + 1.0;
        }

        @Override
        public String getString(CommandContext context, String original)
        {
            return String.valueOf(this.minValue + RAND.nextDouble() * this.range);
        }
    }
}
