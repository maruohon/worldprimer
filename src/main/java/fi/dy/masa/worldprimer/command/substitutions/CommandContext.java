package fi.dy.masa.worldprimer.command.substitutions;

import javax.annotation.Nullable;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;

public class CommandContext
{
    protected final MinecraftServer server;
    @Nullable protected final World world;
    @Nullable protected final EntityPlayer player;

    public CommandContext(MinecraftServer server, @Nullable World world, @Nullable EntityPlayer player)
    {
        this.server = server;
        this.world = world;
        this.player = player;
    }

    public MinecraftServer getServer()
    {
        return server;
    }

    @Nullable
    public World getWorld()
    {
        return world;
    }

    @Nullable
    public Entity getPlayer()
    {
        return player;
    }
}
