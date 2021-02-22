package fi.dy.masa.worldprimer.command.substitutions;

import java.text.SimpleDateFormat;
import java.util.Date;

public class SubstitutionRealTime extends SubstitutionBase
{
    protected final SimpleDateFormat format;

    public SubstitutionRealTime(String formatStr)
    {
        super(true, false);

        this.format = new SimpleDateFormat(formatStr);
    }

    @Override
    public String getString(CommandContext context, String original)
    {
        return this.format.format(new Date());
    }
}
