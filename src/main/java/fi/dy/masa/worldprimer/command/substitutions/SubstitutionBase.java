package fi.dy.masa.worldprimer.command.substitutions;

import javax.annotation.Nullable;
import net.minecraft.world.World;

public abstract class SubstitutionBase implements IStringProvider
{
    protected final boolean isNumeric;
    protected final boolean hasArguments;

    protected SubstitutionBase(boolean isNumeric, boolean hasArguments)
    {
        this.isNumeric = isNumeric;
        this.hasArguments = hasArguments;
    }

    public final boolean isNumeric()
    {
        return this.isNumeric;
    }

    public final boolean hasArguments()
    {
        return this.hasArguments;
    }

    public boolean isValid(String originalSubstitutionString)
    {
        return true;
    }

    /**
     * Builds the final substitution for a given substitution string.
     * This is meant for substitutions that take in arguments,
     * to allow them to parse the arguments and pre-build their final state.
     * @param originalSubstitutionString
     * @return
     */
    @Nullable
    public SubstitutionBase buildSubstitution(String originalSubstitutionString)
    {
        return this;
    }

    public int getIntValue(CommandContext context, String original)
    {
        if (this.isNumeric)
        {
            try
            {
                return Integer.parseInt(this.getString(context, original));
            }
            catch (NumberFormatException e)
            {
            }
        }

        return -1;
    }

    public double getDoubleValue(CommandContext context, String original)
    {
        if (this.isNumeric)
        {
            try
            {
                return Double.parseDouble(this.getString(context, original));
            }
            catch (NumberFormatException e)
            {
            }
        }

        return Double.NaN;
    }

    @Nullable
    protected World getWorldFromContext(CommandContext context)
    {
        World world = context.getWorld();

        if (world == null && context.getPlayer() != null)
        {
            world = context.getPlayer().getEntityWorld();
        }

        return world;
    }
}
