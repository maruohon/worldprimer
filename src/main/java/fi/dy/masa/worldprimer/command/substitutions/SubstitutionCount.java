package fi.dy.masa.worldprimer.command.substitutions;

public class SubstitutionCount extends SubstitutionBase
{
    public SubstitutionCount()
    {
        super(true, false);
    }

    @Override
    public int getIntValue(CommandContext context, String original)
    {
        return context.getCount();
    }

    @Override
    public String getString(CommandContext context, String original)
    {
        int count = context.getCount();

        if (count >= 0)
        {
            return String.valueOf(count);
        }

        return original;
    }
}
