package fi.dy.masa.worldprimer.command.substitutions;

public interface IStringProvider
{
    /**
     * Returns a String for the provided context
     * @param context The context for this substitution
     * @param original The original string being substituted
     * @return
     */
    String getString(CommandContext context, String original);
}
