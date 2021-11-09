package fi.dy.masa.worldprimer.command.substitution;

import java.text.SimpleDateFormat;
import java.util.Date;
import javax.annotation.Nullable;

public class RealTimeSubstitution extends BaseSubstitution
{
    protected static final Date DATE = new Date();

    public RealTimeSubstitution()
    {
        super("TIME_IRL", false, true);
    }

    @Override
    public String getString(CommandContext context)
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
        protected final SimpleDateFormat format;

        public RealTimeSubstitutionArgs(String formatStr)
        {
            super("TIME_IRL", formatStr, false, false);

            this.format = new SimpleDateFormat(formatStr);
        }

        @Override
        public String getString(CommandContext context)
        {
            DATE.setTime(System.currentTimeMillis());
            return this.format.format(DATE);
        }
    }
}
