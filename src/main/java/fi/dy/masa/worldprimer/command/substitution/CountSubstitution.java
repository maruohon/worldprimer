package fi.dy.masa.worldprimer.command.substitution;

public class CountSubstitution extends BaseSubstitution
{
    public CountSubstitution()
    {
        super("COUNT", true, false);
    }

    @Override
    public String getString(CommandContext context)
    {
        int count = context.getCount();
        return count >= 0 ? String.valueOf(count) : this.getOriginalFullSubstitutionString();
    }
}
