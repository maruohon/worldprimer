package fi.dy.masa.worldprimer.command.util;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.apache.commons.lang3.StringUtils;
import net.minecraft.command.CommandException;
import net.minecraft.command.CommandResultStats.Type;
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
import fi.dy.masa.worldprimer.command.handler.ParsedCommand;
import fi.dy.masa.worldprimer.command.substitution.CommandContext;
import fi.dy.masa.worldprimer.config.Configs;
import mcp.MethodsReturnNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class WorldPrimerCommandSender implements ICommandSender
{
    public static final WorldPrimerCommandSender INSTANCE = new WorldPrimerCommandSender();

    @Nullable private World executionWorld;
    @Nullable private ICommandSender sender;
    @Nullable private Entity senderEntity;
    @Nullable private String senderName;

    public void executeCommand(ParsedCommand command, String commandStr, CommandContext ctx)
    {
        try
        {
            if (StringUtils.isBlank(commandStr) == false)
            {
                this.applyOverrides(command, ctx);

                MinecraftServer server = this.getServer();
                ICommandSender sender = this.getCommandSender();
                World world = this.getExecutionWorld(sender);
                String dimStr = world != null ? String.valueOf(world.provider.getDimension()) : "<none>";

                // TODO move the chunk loading commands to be actual commands
                /*
                if (this.isChunkLoadingCommand(command))
                {
                    WorldPrimer.logInfo("Attempting to load chunks in dimension {}", dim);
                    WorldUtils.executeChunkLoadingCommand(command, worldTmp);
                }
                */
                // Execute our own commands directly, because the commands haven't been registered
                // yet when the dimensions first load during server start.
                if (this.isWorldPrimerCommand(commandStr))
                {
                    WorldPrimer.logInfo("Attempting to directly execute a (possibly substituted) " +
                                        "/worldprimer command '{}' in dimension {}", commandStr, dimStr);

                    try
                    {
                        String subStr = commandStr.substring(12); // remove "worldprimer " from the beginning
                        WorldPrimer.commandWorldPrimer.execute(server, sender, subStr.trim().split(" "));
                    }
                    catch (CommandException e)
                    {
                        WorldPrimer.LOGGER.warn("Failed to execute the command '{}'", commandStr, e);
                    }
                }
                else
                {
                    WorldPrimer.logInfo("Running a (possibly substituted) command: '{}' in dimension {}", commandStr, dimStr);
                    server.getCommandManager().executeCommand(sender, commandStr);
                }
            }
        }
        finally
        {
            this.clearOverrides();
        }
    }

    private void applyOverrides(ParsedCommand command, CommandContext ctx)
    {
        this.executionWorld = command.getExecutionWorld(ctx);
        this.sender = command.getCommandSender(ctx);
        this.senderEntity = command.getCommandSenderEntity(ctx);
        this.senderName = command.getCommandSenderName(ctx);
    }

    private void clearOverrides()
    {
        this.executionWorld = null;
        this.sender = null;
        this.senderEntity = null;
        this.senderName = null;
    }

    private boolean isWorldPrimerCommand(String command)
    {
        return command.startsWith("worldprimer ");
    }

    private ICommandSender getCommandSender()
    {
        return this.sender != null ? this.sender : this;
    }

    @Nullable
    private World getExecutionWorld(ICommandSender sender)
    {
        Entity entity = sender.getCommandSenderEntity();

        if (entity != null)
        {
            return entity.getEntityWorld();
        }

        return sender.getEntityWorld();
    }

    @Override
    @Nullable
    public Entity getCommandSenderEntity()
    {
        return this.senderEntity;
    }

    @Override
    public World getEntityWorld()
    {
        return this.executionWorld != null ? this.executionWorld : this.getServer().worlds[0];
    }

    @Override
    public String getName()
    {
        return this.senderName != null ? this.senderName : Configs.commandSenderName;
    }

    @Override
    public MinecraftServer getServer()
    {
        return FMLCommonHandler.instance().getMinecraftServerInstance();
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
    public boolean sendCommandFeedback()
    {
        return false;
    }

    @Override
    public void setCommandStat(Type type, int amount)
    {
    }
}
