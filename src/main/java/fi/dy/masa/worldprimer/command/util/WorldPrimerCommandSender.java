package fi.dy.masa.worldprimer.command.util;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.apache.commons.lang3.StringUtils;
import net.minecraft.command.CommandException;
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
import fi.dy.masa.worldprimer.config.Configs;
import fi.dy.masa.worldprimer.util.WorldUtils;
import mcp.MethodsReturnNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class WorldPrimerCommandSender implements ICommandSender
{
    public static final WorldPrimerCommandSender INSTANCE = new WorldPrimerCommandSender();

    private World executionWorld;
    @Nullable private String senderName = null;

    public void executeCommand(String command, @Nullable World world)
    {
        ICommandManager manager = this.getServer().getCommandManager();
        this.executionWorld = world;

        if (StringUtils.isBlank(command) == false)
        {
            command = this.handleCommandSenderNamePrefix(command);

            World worldTmp = this.getEntityWorld();
            String dim = worldTmp != null ? String.valueOf(worldTmp.provider.getDimension()) : "<none>";

            if (this.isChunkLoadingCommand(command))
            {
                WorldPrimer.logInfo("Attempting to load chunks in dimension {}", dim);
                WorldUtils.executeChunkLoadingCommand(command, worldTmp);
            }
            // Execute our own commands directly, because the commands haven't been registered
            // yet when the dimensions first load during server start.
            else if (this.isWorldPrimerCommand(command))
            {
                WorldPrimer.logInfo("Attempting to directly execute a (possibly substituted) " +
                                    "worldprimer command '{}' in dimension {}", command, dim);

                try
                {
                    command = command.substring(12); // remove "worldprimer " from the beginning
                    WorldPrimer.commandWorldPrimer.execute(worldTmp.getMinecraftServer(), this, command.trim().split(" "));
                }
                catch (CommandException e)
                {
                    WorldPrimer.LOGGER.warn("Failed to execute the command '{}'", command, e);
                }
            }
            else
            {
                WorldPrimer.logInfo("Running a (possibly substituted) command: '{}' in dimension {}", command, dim);
                manager.executeCommand(this, command);
            }

            this.senderName = null;
        }

        WorldUtils.unloadLoadedChunks(world);

        this.executionWorld = null;
    }

    private String handleCommandSenderNamePrefix(String originalCommand)
    {
        String namePrefix = "worldprimer-command-sender ";

        if (originalCommand.startsWith(namePrefix))
        {
            int namePrefixLength = namePrefix.length();
            String command = originalCommand.substring(namePrefixLength);

            if (command.charAt(0) == '"')
            {
                int endQuotePos = command.indexOf('"', 1);

                if (endQuotePos > 0 && endQuotePos < command.length() - 3)
                {
                    this.senderName = command.substring(1, endQuotePos);
                    return command.substring(endQuotePos + 2);
                }
                else
                {
                    WorldPrimer.LOGGER.warn("Malformed command sender name prefix for command '{}'", originalCommand);
                }
            }
            else
            {
                int nameEndPos = command.indexOf(' ', 1);

                if (nameEndPos > 0 && nameEndPos < command.length() - 2)
                {
                    this.senderName = command.substring(0, nameEndPos);
                    return command.substring(nameEndPos + 1);
                }
                else
                {
                    WorldPrimer.LOGGER.warn("Malformed command sender name prefix for command '{}'", originalCommand);
                }
            }
        }

        return originalCommand;
    }

    private boolean isChunkLoadingCommand(String command)
    {
        String[] parts = command.split(" ");
        return parts.length > 0 && (parts[0].equals("worldprimer-load-chunks") || parts[0].equals("worldprimer-load-blocks"));
    }

    private boolean isWorldPrimerCommand(String command)
    {
        String[] parts = command.split(" ");
        return parts.length > 0 && parts[0].equals("worldprimer");
    }

    @Override
    public String getName()
    {
        return this.senderName != null ? this.senderName : Configs.commandSenderName;
    }

    @Override
    public ITextComponent getDisplayName()
    {
        return new TextComponentString(this.getName());
    }

    @Override
    public void sendMessage(ITextComponent component)
    {
        WorldPrimer.logInfo(component.getUnformattedText());
    }

    @Override
    public boolean canUseCommand(int permLevel, String commandName)
    {
        return true;
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
        return this.executionWorld != null ? this.executionWorld : FMLCommonHandler.instance().getMinecraftServerInstance().worlds[0];
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
