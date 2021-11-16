package fi.dy.masa.worldprimer.command.substitution;

public interface StringSubstitution
{
    /**
     * Evaluates the substitution and returns the final String
     * for this substitution using the provided context
     * @param context The context for this substitution
     * @return the final string value of this substitution
     */
    String evaluate(CommandContext context);
}
