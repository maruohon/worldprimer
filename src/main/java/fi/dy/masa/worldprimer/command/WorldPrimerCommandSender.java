package fi.dy.masa.worldprimer.command;

import javax.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;
import net.minecraft.command.ICommandManager;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import cpw.mods.fml.common.FMLCommonHandler;
import fi.dy.masa.worldprimer.WorldPrimer;
import fi.dy.masa.worldprimer.reference.Reference;

public class WorldPrimerCommandSender implements ICommandSender
{
    private static final ChunkCoordinates ORIGIN = new ChunkCoordinates(0, 0, 0);
    private static final IChatComponent DISPLAY_NAME = new ChatComponentText(Reference.MOD_NAME + " CommandSender");
    private static final WorldPrimerCommandSender INSTANCE = new WorldPrimerCommandSender();

    public static WorldPrimerCommandSender instance()
    {
        return INSTANCE;
    }

    public void runCommands(@Nullable World world, String... commands)
    {
        ICommandManager manager = FMLCommonHandler.instance().getMinecraftServerInstance().getCommandManager();

        for (String command : commands)
        {
            if (StringUtils.isBlank(command) == false)
            {
                String newCommand = this.doCommandSubstitutions(world, command);
                WorldPrimer.logInfo("Running a (possibly substituted) command: '{}'", newCommand);
                manager.executeCommand(this, newCommand);
            }
        }
    }

    private String doCommandSubstitutions(@Nullable World world, String originalCommand)
    {
        if (world == null)
        {
            return originalCommand;
        }

        ChunkCoordinates spawn = null;

        if (world instanceof WorldServer)
        {
            spawn = ((WorldServer) world).getSpawnPoint();
        }

        if (spawn == null)
        {
            spawn = world.getSpawnPoint();
        }

        int dim = world.provider.dimensionId;
        String[] parts = originalCommand.split(" ");

        for (int i = 0; i < parts.length; i++)
        {
            parts[i] = this.substituteNumber(parts[i], "{DIMENSION}", dim);
            parts[i] = this.substituteNumber(parts[i], "{SPAWNX}", spawn.posX);
            parts[i] = this.substituteNumber(parts[i], "{SPAWNY}", spawn.posY);
            parts[i] = this.substituteNumber(parts[i], "{SPAWNZ}", spawn.posZ);
        }

        return String.join(" ", parts);
    }

    private String substituteNumber(String argument, String placeHolder, int value)
    {
        if (argument.equals(placeHolder))
        {
            return String.valueOf(value);
        }
        else if (argument.startsWith(placeHolder))
        {
            String relative = argument.substring(placeHolder.length(), argument.length());

            if (relative.length() > 1 && (relative.charAt(0) == '-' || relative.charAt(0) == '+'))
            {
                try
                {
                    double relVal = Double.parseDouble(relative);
                    relVal += value;

                    return String.valueOf(relVal);
                }
                catch (NumberFormatException e)
                {
                    WorldPrimer.logger.warn("Failed to parse relative argument '{}'", argument, e);
                }
            }
        }

        return argument;
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
    public void addChatMessage(IChatComponent component)
    {
        WorldPrimer.logInfo(component.getUnformattedText());
    }

    @Override
    public boolean canCommandSenderUseCommand(int permLevel, String commandName)
    {
        return true;
    }

    @Override
    public ChunkCoordinates getPlayerCoordinates()
    {
        return ORIGIN;
    }

    @Override
    public World getEntityWorld()
    {
        return FMLCommonHandler.instance().getMinecraftServerInstance().worldServers[0];
    }
}
