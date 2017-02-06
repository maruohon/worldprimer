package fi.dy.masa.worldprimer.command;

import javax.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;
import net.minecraft.command.CommandResultStats.Type;
import net.minecraft.command.ICommandManager;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.FMLCommonHandler;
import fi.dy.masa.worldprimer.WorldPrimer;
import fi.dy.masa.worldprimer.reference.Reference;

public class WorldPrimerCommandSender implements ICommandSender
{
    private static final ITextComponent DISPLAY_NAME = new TextComponentString(Reference.MOD_NAME + " CommandSender");
    private static final WorldPrimerCommandSender INSTANCE = new WorldPrimerCommandSender();

    public static WorldPrimerCommandSender instance()
    {
        return INSTANCE;
    }

    public void runCommands(String... commands)
    {
        ICommandManager manager = this.getServer().getCommandManager();

        for (String command : commands)
        {
            if (StringUtils.isBlank(command) == false)
            {
                manager.executeCommand(this, command);
            }
        }
    }

    @Override
    public String getName()
    {
        return Reference.MOD_NAME + " CommandSender";
    }

    @Override
    public ITextComponent getDisplayName()
    {
        return DISPLAY_NAME;
    }

    @Override
    public void sendMessage(ITextComponent component)
    {
        WorldPrimer.logInfo(component.getUnformattedText());
    }

    @Override
    public boolean canUseCommand(int permLevel, String commandName)
    {
        return permLevel <= 2;
    }

    @Override
    public BlockPos getPosition()
    {
        return BlockPos.ORIGIN;
    }

    @Override
    public Vec3d getPositionVector()
    {
        return Vec3d.ZERO;
    }

    @Override
    public World getEntityWorld()
    {
        return FMLCommonHandler.instance().getMinecraftServerInstance().worlds[0];
    }

    @Override
    @Nullable
    public Entity getCommandSenderEntity()
    {
        return null;
    }

    @Override
    public boolean sendCommandFeedback()
    {
        return false;
    }

    @Override
    public void setCommandStat(Type type, int amount)
    {
    }

    @Override
    public MinecraftServer getServer()
    {
        return FMLCommonHandler.instance().getMinecraftServerInstance();
    }
}
