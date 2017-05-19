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
import net.minecraft.world.WorldServer;
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

    public void runCommands(@Nullable World world, String... commands)
    {
        ICommandManager manager = this.getServer().getCommandManager();

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

        BlockPos spawn = null;

        if (world instanceof WorldServer)
        {
            spawn = ((WorldServer) world).getSpawnCoordinate();
        }

        if (spawn == null)
        {
            spawn = world.getSpawnPoint();
        }

        int dim = world.provider.getDimension();
        String[] parts = originalCommand.split(" ");

        for (int i = 0; i < parts.length; i++)
        {
            parts[i] = this.substituteNumber(parts[i], "{DIMENSION}", dim);
            parts[i] = this.substituteNumber(parts[i], "{SPAWNX}", spawn.getX());
            parts[i] = this.substituteNumber(parts[i], "{SPAWNY}", spawn.getY());
            parts[i] = this.substituteNumber(parts[i], "{SPAWNZ}", spawn.getZ());
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
