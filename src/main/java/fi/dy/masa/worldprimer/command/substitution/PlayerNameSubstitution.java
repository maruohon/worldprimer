package fi.dy.masa.worldprimer.command.substitution;

import net.minecraft.entity.player.EntityPlayer;

public class PlayerNameSubstitution extends BaseSubstitution
{
    public PlayerNameSubstitution()
    {
        super("PLAYER_NAME", false, false);
    }

    @Override
    public String getString(CommandContext context)
    {
        EntityPlayer player = context.getPlayer();
        return player != null ? player.getName() : this.getOriginalFullSubstitutionString();
    }
}
