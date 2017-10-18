package fi.dy.masa.worldprimer.event;

import org.apache.commons.lang3.StringUtils;
import net.minecraft.world.World;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import fi.dy.masa.worldprimer.WorldPrimer;
import fi.dy.masa.worldprimer.command.WorldPrimerCommandSender;
import fi.dy.masa.worldprimer.config.Configs;
import fi.dy.masa.worldprimer.util.DataTracker;

public class EventHandler
{
    private boolean runCreationCommands;

    @SubscribeEvent
    public void onCreateSpawn(WorldEvent.CreateSpawnPosition event)
    {
        WorldPrimer.logInfo("WorldEvent.CreateSpawnPosition");
        World world = event.getWorld();

        // When creating the overworld spawn, which happens once, when the level.dat doesn't yet exist.
        // This is only used if the load tracking is not used.
        if (Configs.enableDimensionLoadTracking == false &&
            world.isRemote == false && world.provider.getDimension() == 0)
        {
            if (Configs.enableEarlyWorldCreationCommands)
            {
                WorldPrimer.logInfo("WorldEvent.CreateSpawnPosition - running earlyWorldCreationCommands");
                WorldPrimerCommandSender.instance().runCommands(world, Configs.earlyWorldCreationCommands);
            }

            // Defer running the commands until the world is actually ready to load
            this.runCreationCommands = Configs.enablePostWorldCreationCommands;
        }
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event)
    {
        World world = event.getWorld();

        if (world.isRemote == false)
        {
            int dimension = world.provider.getDimension();
            WorldPrimer.logInfo("WorldEvent.Load, DIM: {}", dimension);

            // The creation commands are only run via this method when not using dimension load count tracking
            if (this.runCreationCommands && dimension == 0)
            {
                WorldPrimer.logInfo("WorldEvent.Load - running postWorldCreationCommands");
                WorldPrimerCommandSender.instance().runCommands(world, Configs.postWorldCreationCommands);
                this.runCreationCommands = false;
            }

            if (Configs.enableDimensionLoadTracking)
            {
                DataTracker.instance().dimensionLoaded(dimension);
            }

            if (Configs.enableDimensionLoadingCommands)
            {
                WorldPrimer.logInfo("WorldEvent.Load - running dimensionLoadingCommands");
                this.runDimensionLoadingCommands(dimension, world);
            }
        }
    }

    @SubscribeEvent
    public void onWorldSave(WorldEvent.Save event)
    {
        DataTracker.instance().writeToDisk();
    }

    private void runDimensionLoadingCommands(int dimension, World world)
    {
        String[] commands = Configs.dimensionLoadingCommands;

        for (String command : commands)
        {
            if (StringUtils.isBlank(command))
            {
                continue;
            }

            String[] parts = command.split(" ");

            if (parts.length >= 3 && parts[0].equals("worldprimer-dim-command"))
            {
                this.runDimLoadingCommandsRegular(dimension, world, command, parts);
            }
            else if (Configs.enableDimensionLoadTracking && parts.length >= 4 && parts[0].equals("worldprimer-dim-command-nth"))
            {
                this.runDimLoadingCommandsNth(dimension, world, command, parts);
            }
            else
            {
                WorldPrimerCommandSender.instance().runCommands(world, command);
            }
        }
    }

    private void runDimLoadingCommandsRegular(int dimension, World world, String fullCommand, String[] cmdParts)
    {
        try
        {
            int dim = Integer.parseInt(cmdParts[1]);

            if (dimension == dim)
            {
                cmdParts = dropFirstStrings(cmdParts, 2);
                WorldPrimerCommandSender.instance().runCommands(world, String.join(" ", cmdParts));
            }
        }
        catch (NumberFormatException e)
        {
            WorldPrimer.logger.warn("Invalid dimension id '{}' in dimension-specific command '{}'", cmdParts[1], fullCommand);
        }
    }

    private void runDimLoadingCommandsNth(int dimension, World world, String fullCommand, String[] cmdParts)
    {
        try
        {
            int dim = Integer.parseInt(cmdParts[1]);

            if (dimension == dim)
            {
                String countStr = cmdParts[2];
                boolean modulo = false;

                if (countStr.charAt(0) == '%')
                {
                    countStr = countStr.substring(1, countStr.length());
                    modulo = true;
                }

                int count = Integer.parseInt(countStr);
                int loadCount = DataTracker.instance().getDimensionLoadCount(dimension);

                if ((modulo && count != 0 && (loadCount % count) == 0) || (modulo == false && loadCount == count))
                {
                    cmdParts = dropFirstStrings(cmdParts, 3);
                    WorldPrimerCommandSender.instance().runCommands(world, String.join(" ", cmdParts));
                }
            }
        }
        catch (NumberFormatException e)
        {
            WorldPrimer.logger.warn("Invalid syntax in dimension-specific command '{}'", fullCommand);
        }
    }

    public static String[] dropFirstStrings(String[] input, int toDrop)
    {
        if (toDrop >= input.length)
        {
            return new String[0];
        }

        String[] arr = new String[input.length - toDrop];
        System.arraycopy(input, toDrop, arr, 0, input.length - toDrop);
        return arr;
    }
}
