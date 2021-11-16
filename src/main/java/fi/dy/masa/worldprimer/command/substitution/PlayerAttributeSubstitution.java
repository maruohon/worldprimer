package fi.dy.masa.worldprimer.command.substitution;

import java.util.function.Function;
import net.minecraft.entity.player.EntityPlayer;

public class PlayerAttributeSubstitution extends BaseSubstitution
{
    protected final Function<EntityPlayer, String> stringFunction;

    public PlayerAttributeSubstitution(String name, Function<EntityPlayer, String> stringFunction)
    {
        super(name, false, false);

        this.stringFunction = stringFunction;
    }

    @Override
    public String evaluate(CommandContext context)
    {
        EntityPlayer player = context.getPlayer();
        return player != null ? this.stringFunction.apply(player) : this.getOriginalFullSubstitutionString();
    }
}
