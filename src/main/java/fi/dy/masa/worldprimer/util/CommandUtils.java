package fi.dy.masa.worldprimer.util;

import javax.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import fi.dy.masa.worldprimer.WorldPrimer;
import fi.dy.masa.worldprimer.command.WorldPrimerCommandSender;
import fi.dy.masa.worldprimer.config.Configs;
import fi.dy.masa.worldprimer.util.DataTracker.PlayerDataType;
import fi.dy.masa.worldprimer.util.DataTracker.PlayerDimensionDataType;

public class CommandUtils
{
    private static boolean runCreationCommands;

    public static void onCreateSpawn(final World world)
    {
        final int dimension = world.provider.getDimension();
        WorldPrimer.logInfo("WorldEvent.CreateSpawnPosition, DIM: {}", dimension);

        // When creating the overworld spawn, which happens once, when the level.dat doesn't yet exist.
        // This is only used if the load tracking is not used.
        if (Configs.enableDataTracking == false && world.isRemote == false && dimension == 0)
        {
            if (Configs.enableEarlyWorldCreationCommands)
            {
                WorldPrimer.logInfo("WorldEvent.CreateSpawnPosition: Running earlyWorldCreationCommands for DIM: {}", dimension);
                WorldPrimerCommandSender.instance().runCommands(world, Configs.earlyWorldCreationCommands);
            }

            // Defer running the commands until the world is actually ready to load
            runCreationCommands = Configs.enablePostWorldCreationCommands;
        }
    }

    public static void onWorldLoad(final World world)
    {
        if (world.isRemote == false)
        {
            final int dimension = world.provider.getDimension();
            WorldPrimer.logInfo("WorldEvent.Load, DIM: {}", dimension);

            if (Configs.enableTimedCommands)
            {
                TimedCommands.updateTimedCommandsForDimension(dimension);
            }

            // The creation commands are only run via this method when not using dimension load count tracking
            if (runCreationCommands && dimension == 0)
            {
                WorldPrimer.logInfo("WorldEvent.Load: Running postWorldCreationCommands for DIM: {}", dimension);
                WorldPrimerCommandSender.instance().runCommands(world, Configs.postWorldCreationCommands);
                runCreationCommands = false;
            }

            if (Configs.enableDataTracking)
            {
                DataTracker.instance().dimensionLoaded(dimension);
            }

            if (Configs.enableDimensionLoadingCommands)
            {
                WorldPrimer.logInfo("WorldEvent.Load - running dimensionLoadingCommands");
                final int currentCount = DataTracker.instance().getDimensionLoadCount(dimension);
                runDimensionCommands(null, world, world.provider.getDimension(), currentCount, Configs.dimensionLoadingCommands);
            }
        }
    }

    public static void onPlayerJoin(final EntityPlayer player)
    {
        WorldPrimer.logInfo("PlayerLoggedInEvent player: {}", player);
        handlePlayerEvent(player, PlayerDataType.JOIN, Configs.enablePlayerJoinCommands, Configs.playerJoinCommands);

        if (Configs.enablePlayerChangedDimensionEnterCommands)
        {
            int dimension = player.getEntityWorld().provider.getDimension();
            DataTracker.instance().incrementPlayerDimensionEventCount(player, dimension, PlayerDimensionDataType.ENTER);

            final int currentCount = DataTracker.instance().getPlayerDimensionEventCount(player, dimension, PlayerDimensionDataType.ENTER);
            runDimensionCommands(player, player.getEntityWorld(), dimension, currentCount, Configs.playerChangedDimensionEnterCommands);
        }
    }

    public static void onPlayerQuit(final EntityPlayer player)
    {
        WorldPrimer.logInfo("PlayerLoggedOutEvent player: {}", player);
        handlePlayerEvent(player, PlayerDataType.QUIT, Configs.enablePlayerQuitCommands, Configs.playerQuitCommands);

        if (Configs.enablePlayerChangedDimensionLeaveCommands)
        {
            int dimension = player.getEntityWorld().provider.getDimension();
            DataTracker.instance().incrementPlayerDimensionEventCount(player, dimension, PlayerDimensionDataType.LEAVE);

            final int currentCount = DataTracker.instance().getPlayerDimensionEventCount(player, dimension, PlayerDimensionDataType.LEAVE);
            runDimensionCommands(player, player.getEntityWorld(), dimension, currentCount, Configs.playerChangedDimensionLeaveCommands);
        }
    }

    public static void onPlayerDeath(final EntityPlayer player)
    {
        WorldPrimer.logInfo("LivingDeathEvent for player: {}", player);
        handlePlayerEvent(player, PlayerDataType.DEATH, Configs.enablePlayerDeathCommands, Configs.playerDeathCommands);
    }

    public static void onPlayerRespawn(final EntityPlayer player, final boolean isEndConquered)
    {
        // Don't run the respawn commands when just leaving the End
        if (isEndConquered == false)
        {
            WorldPrimer.logInfo("PlayerRespawnEvent player: {}", player);
            handlePlayerEvent(player, PlayerDataType.RESPAWN, Configs.enablePlayerRespawnCommands, Configs.playerRespawnCommands);
        }
        // The PlayerChangedDimensionEvent doesn't seem to fire when leaving the End, so run the leave commands also here
        else
        {
            if (Configs.enablePlayerChangedDimensionLeaveCommands)
            {
                final int dimension = 1;
                WorldPrimer.logInfo("PlayerRespawnEvent (leaving the End) player: {}", player);
                DataTracker.instance().incrementPlayerDimensionEventCount(player, dimension, PlayerDimensionDataType.LEAVE);
                final int currentCount = DataTracker.instance().getPlayerDimensionEventCount(player, dimension, PlayerDimensionDataType.LEAVE);

                // Note: the dimension MUST be passed from here instead of getting it from the WorldProvider,
                // because the player is in the target dimension already at this point
                runDimensionCommands(player, player.getEntityWorld(), dimension, currentCount, Configs.playerChangedDimensionLeaveCommands);
            }

            if (Configs.enablePlayerChangedDimensionEnterCommands)
            {
                int dimension = player.getEntityWorld().provider.getDimension();
                DataTracker.instance().incrementPlayerDimensionEventCount(player, dimension, PlayerDimensionDataType.ENTER);

                final int currentCount = DataTracker.instance().getPlayerDimensionEventCount(player, dimension, PlayerDimensionDataType.ENTER);
                runDimensionCommands(player, player.getEntityWorld(), dimension, currentCount, Configs.playerChangedDimensionEnterCommands);
            }
        }
    }

    public static void onPlayerChangedDimension(final EntityPlayer player, final int fromDim, final int toDim)
    {
        if (Configs.enablePlayerChangedDimensionLeaveCommands)
        {
            WorldPrimer.logInfo("PlayerChangedDimensionEvent player: {} left dim {}", player, fromDim);
            DataTracker.instance().incrementPlayerDimensionEventCount(player, fromDim, PlayerDimensionDataType.LEAVE);
            final int currentCount = DataTracker.instance().getPlayerDimensionEventCount(player, fromDim, PlayerDimensionDataType.LEAVE);

            // Note: the dimension MUST be passed from here instead of getting it from the WorldProvider,
            // because the player is in the target dimension already at this point
            runDimensionCommands(player, player.getEntityWorld(), fromDim, currentCount, Configs.playerChangedDimensionLeaveCommands);
        }

        if (Configs.enablePlayerChangedDimensionEnterCommands)
        {
            WorldPrimer.logInfo("PlayerChangedDimensionEvent player: {} entered dim {}", player, toDim);
            DataTracker.instance().incrementPlayerDimensionEventCount(player, toDim, PlayerDimensionDataType.ENTER);
            final int currentCount = DataTracker.instance().getPlayerDimensionEventCount(player, toDim, PlayerDimensionDataType.ENTER);

            runDimensionCommands(player, player.getEntityWorld(), toDim, currentCount, Configs.playerChangedDimensionEnterCommands);
        }
    }

    private static void runDimensionCommands(@Nullable EntityPlayer player, World world, final int dimension, final int currentCount, String[] commands)
    {
        for (String command : commands)
        {
            if (StringUtils.isBlank(command) || (command.length() > 0 && command.charAt(0) == '#'))
            {
                continue;
            }

            String[] parts = command.split("\\s+", 4);

            if (parts.length >= 3 && parts[0].equals("worldprimer-dim-command"))
            {
                runDimensionCommandsRegular(player, world, dimension, command, parts);
            }
            else if (Configs.enableDataTracking && parts.length >= 4 && parts[0].equals("worldprimer-dim-command-nth"))
            {
                runDimensionCommandsNth(player, world, dimension, command, parts, currentCount);
            }
            else
            {
                WorldPrimerCommandSender.instance().runCommands(player, world, command);
            }
        }
    }

    private static void runDimensionCommandsRegular(@Nullable EntityPlayer player, World world, final int dimension,
            String fullCommand, String[] cmdParts)
    {
        try
        {
            if (cmdParts[1].equals("*") || dimension == Integer.parseInt(cmdParts[1]))
            {
                cmdParts = dropFirstStrings(cmdParts, 2);
                WorldPrimerCommandSender.instance().runCommands(player, world, String.join(" ", cmdParts));
            }
        }
        catch (NumberFormatException e)
        {
            WorldPrimer.logger.warn("Invalid dimension id '{}' in dimension-specific command '{}'", cmdParts[1], fullCommand);
        }
    }

    private static void runDimensionCommandsNth(@Nullable EntityPlayer player, World world, final int dimension,
            String fullCommand, String[] cmdParts, final int currentCount)
    {
        try
        {
            if (cmdParts[2].equals("*") || dimension == Integer.parseInt(cmdParts[2]))
            {
                String countStr = cmdParts[1];
                boolean modulo = false;

                if (countStr.charAt(0) == '%')
                {
                    countStr = countStr.substring(1, countStr.length());
                    modulo = true;
                }

                int count = Integer.parseInt(countStr);

                if ((modulo && count != 0 && (currentCount % count) == 0) || (modulo == false && currentCount == count))
                {
                    cmdParts = dropFirstStrings(cmdParts, 3);
                    WorldPrimerCommandSender.instance().runCommands(player, world, String.join(" ", cmdParts));
                }
            }
        }
        catch (NumberFormatException e)
        {
            WorldPrimer.logger.warn("Invalid syntax in dimension-specific command '{}'", fullCommand);
        }
    }

    private static void handlePlayerEvent(EntityPlayer player, PlayerDataType type, boolean enabled, String[] commands)
    {
        if (enabled)
        {
            DataTracker.instance().incrementPlayerDataCount(player, type);
            final int currentCount = DataTracker.instance().getPlayerDataCount(player, type);
            runTrackedCommands(player, commands, currentCount);
        }
    }

    private static void runTrackedCommands(EntityPlayer player, String[] commands, int currentCount)
    {
        for (String command : commands)
        {
            if (StringUtils.isBlank(command) || (command.length() > 0 && command.charAt(0) == '#'))
            {
                continue;
            }

            String[] parts = command.split("\\s+", 3);

            if (Configs.enableDataTracking && parts.length >= 3 && parts[0].equals("worldprimer-tracked-command-nth"))
            {
                runTrackedCommandsNth(player, command, parts, currentCount);
            }
            else
            {
                WorldPrimerCommandSender.instance().runCommands(player, player.getEntityWorld(), command);
            }
        }
    }

    private static void runTrackedCommandsNth(EntityPlayer player, String fullCommand, String[] cmdParts, int currentCount)
    {
        try
        {
            String countStr = cmdParts[1];
            boolean modulo = false;

            if (countStr.charAt(0) == '%')
            {
                countStr = countStr.substring(1, countStr.length());
                modulo = true;
            }

            int count = Integer.parseInt(countStr);

            if ((modulo && count != 0 && (currentCount % count) == 0) || (modulo == false && currentCount == count))
            {
                cmdParts = dropFirstStrings(cmdParts, 2);
                WorldPrimerCommandSender.instance().runCommands(player, player.getEntityWorld(), String.join(" ", cmdParts));
            }
        }
        catch (NumberFormatException e)
        {
            WorldPrimer.logger.warn("Invalid syntax in tracked command '{}'", fullCommand);
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
