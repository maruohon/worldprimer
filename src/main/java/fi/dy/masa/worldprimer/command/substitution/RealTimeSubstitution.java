package fi.dy.masa.worldprimer.command.substitution;

import java.text.SimpleDateFormat;
import java.util.Date;
import javax.annotation.Nullable;

public class RealTimeSubstitution extends BaseSubstitution
{
    public RealTimeSubstitution(String substitutionName)
    {
        super(substitutionName, true);
    }

    @Override
    public String evaluate(CommandContext context)
    {
        return this.getOriginalFullSubstitutionString();
    }

    @Nullable
    @Override
    public BaseSubstitution buildSubstitution(String argumentString)
    {
        return new RealTimeSubstitutionArgs(argumentString);
    }

    protected static class RealTimeSubstitutionArgs extends ArgumentSubstitution
    {
        protected final Date date;
        protected final SimpleDateFormat format;

        public RealTimeSubstitutionArgs(String formatStr)
        {
            super("TIME_IRL", formatStr, false);

            this.date = new Date();
            this.format = new SimpleDateFormat(formatStr);
        }

        @Override
        public String evaluate(CommandContext context)
        {
            this.date.setTime(System.currentTimeMillis());
            return this.format.format(this.date);
        }
    }
}
