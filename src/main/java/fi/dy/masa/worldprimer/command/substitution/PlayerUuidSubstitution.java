package fi.dy.masa.worldprimer.command.substitution;

import net.minecraft.entity.player.EntityPlayer;

public class PlayerUuidSubstitution extends BaseSubstitution
{
    public PlayerUuidSubstitution()
    {
        super("PLAYER_UUID", false, false);
    }

    @Override
    public String getString(CommandContext context)
    {
        EntityPlayer player = context.getPlayer();
        return player != null ? player.getUniqueID().toString() : this.getOriginalFullSubstitutionString();
    }
}
