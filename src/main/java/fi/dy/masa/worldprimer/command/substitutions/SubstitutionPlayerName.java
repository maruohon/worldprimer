package fi.dy.masa.worldprimer.command.substitutions;

import net.minecraft.entity.player.EntityPlayer;

public class SubstitutionPlayerName extends SubstitutionBase
{
    public SubstitutionPlayerName()
    {
        super(false, false);
    }

    @Override
    public String getString(CommandContext context, String original)
    {
        EntityPlayer player = context.getPlayer();
        return player != null ? player.getName() : original;
    }
}
