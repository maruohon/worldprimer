package fi.dy.masa.worldprimer.command.substitution;

public interface StringSubstitution
{
    /**
     * Returns a String for the provided context
     * @param context The context for this substitution
     * @return the final string value of this substitution
     */
    String getString(CommandContext context);
}
