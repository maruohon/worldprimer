package fi.dy.masa.worldprimer.command.substitution;

import java.util.OptionalInt;
import java.util.function.IntSupplier;
import javax.annotation.Nullable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.FMLCommonHandler;

public class CommandContext
{
    protected final MinecraftServer server;
    @Nullable protected final World world;
    @Nullable protected final EntityPlayer player;
    @Nullable protected IntSupplier dimensionSource;
    protected final int count;

    public CommandContext(@Nullable World world, @Nullable EntityPlayer player, int count)
    {
        this(FMLCommonHandler.instance().getMinecraftServerInstance(), world, player, count);
    }

    public CommandContext(@Nullable World world, @Nullable EntityPlayer player, int count, int eventDimension)
    {
        this(world, player, count);

        this.dimensionSource = () -> eventDimension;
    }

    public CommandContext(MinecraftServer server, @Nullable World world, @Nullable EntityPlayer player, int count)
    {
        this.server = server;
        this.world = world;
        this.player = player;
        this.count = count;
    }

    public int getCount()
    {
        return this.count;
    }

    public MinecraftServer getServer()
    {
        return this.server;
    }

    public OptionalInt getEventDimension()
    {
        if (this.dimensionSource != null)
        {
            return OptionalInt.of(this.dimensionSource.getAsInt());
        }

        World world = this.getWorld();
        return world != null ? OptionalInt.of(world.provider.getDimension()) : OptionalInt.empty();
    }

    @Nullable
    public World getWorld()
    {
        if (this.world == null && this.player != null)
        {
            return this.player.getEntityWorld();
        }

        return this.world;
    }

    @Nullable
    public EntityPlayer getPlayer()
    {
        return this.player;
    }
}
