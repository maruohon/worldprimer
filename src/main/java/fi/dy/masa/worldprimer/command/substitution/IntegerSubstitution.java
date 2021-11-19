package fi.dy.masa.worldprimer.command.substitution;

import java.util.OptionalInt;
import java.util.function.Function;

public class IntegerSubstitution extends BaseSubstitution
{
    protected final Function<CommandContext, OptionalInt> intSource;

    public IntegerSubstitution(String name, Function<CommandContext, OptionalInt> intSource)
    {
        super(name, false);

        this.intSource = intSource;
    }

    @Override
    public String evaluate(CommandContext ctx)
    {
        OptionalInt intValue = this.intSource.apply(ctx);
        return intValue.isPresent() ? String.valueOf(intValue.getAsInt()) : this.getOriginalFullSubstitutionString();
    }
}
