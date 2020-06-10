package fi.dy.masa.worldprimer.command.substitutions;

import java.text.SimpleDateFormat;
import java.util.Date;

public class SubstitutionRealTime extends SubstitutionBase
{
    protected static final Date DATE = new Date();

    protected final SimpleDateFormat format;

    public SubstitutionRealTime(String formatStr)
    {
        super(true, false);

        this.format = new SimpleDateFormat(formatStr);
    }

    @Override
    public String getString(CommandContext context, String original)
    {
        DATE.setTime(System.currentTimeMillis());
        return this.format.format(DATE);
    }
}
