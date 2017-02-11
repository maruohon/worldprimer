package fi.dy.masa.worldprimer.command;

import org.apache.commons.lang3.StringUtils;
import net.minecraft.command.ICommandManager;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.World;
import cpw.mods.fml.common.FMLCommonHandler;
import fi.dy.masa.worldprimer.WorldPrimer;
import fi.dy.masa.worldprimer.reference.Reference;

public class WorldPrimerCommandSender implements ICommandSender
{
    private static final IChatComponent DISPLAY_NAME = new ChatComponentText(Reference.MOD_NAME + " CommandSender");
    private static final ChunkCoordinates POSITION = new ChunkCoordinates(0, 0, 0);
    private static final WorldPrimerCommandSender INSTANCE = new WorldPrimerCommandSender();

    public static WorldPrimerCommandSender instance()
    {
        return INSTANCE;
    }

    public void runCommands(String... commands)
    {
        ICommandManager manager = FMLCommonHandler.instance().getMinecraftServerInstance().getCommandManager();

        for (String command : commands)
        {
            if (StringUtils.isBlank(command) == false)
            {
                manager.executeCommand(this, command);
            }
        }
    }

    @Override
    public String getCommandSenderName()
    {
        return Reference.MOD_NAME + " CommandSender";
    }

    @Override
    public IChatComponent func_145748_c_()
    {
        return DISPLAY_NAME;
    }

    @Override
    public void addChatMessage(IChatComponent message)
    {
        WorldPrimer.logInfo(message.getUnformattedText());
    }

    @Override
    public boolean canCommandSenderUseCommand(int permLevel, String command)
    {
        return permLevel <= 2;
    }

    @Override
    public ChunkCoordinates getPlayerCoordinates()
    {
        return POSITION;
    }

    @Override
    public World getEntityWorld()
    {
        return FMLCommonHandler.instance().getMinecraftServerInstance().worldServers[0];
    }
}
