package fi.dy.masa.worldprimer.command.util;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import fi.dy.masa.worldprimer.WorldPrimer;
import fi.dy.masa.worldprimer.command.handler.CommandHandler;
import fi.dy.masa.worldprimer.command.substitution.CommandContext;
import fi.dy.masa.worldprimer.config.Configs;
import fi.dy.masa.worldprimer.util.DataTracker;
import fi.dy.masa.worldprimer.util.DataTracker.PlayerDataType;
import fi.dy.masa.worldprimer.util.DataTracker.PlayerDimensionDataType;

public class CommandUtils
{
    private static boolean runCreationCommands;

    public static int getDimension(EntityPlayer player)
    {
        return getDimension(player.getEntityWorld());
    }

    public static int getDimension(World world)
    {
        return world.provider.getDimension();
    }

    public static void onCreateSpawn(World world)
    {
        final int dimension = getDimension(world);
        WorldPrimer.logInfo("WorldEvent.CreateSpawnPosition, DIM: {}", dimension);

        // When creating the overworld spawn, which happens once, when the level.dat doesn't yet exist.
        // This is only used if the load tracking is not used.
        if (Configs.enableDataTracking == false && world.isRemote == false && dimension == 0)
        {
            if (Configs.enableEarlyWorldCreationCommands)
            {
                WorldPrimer.logInfo("WorldEvent.CreateSpawnPosition: Running earlyWorldCreationCommands for DIM: {}", dimension);
                CommandHandler.INSTANCE.executeCommandsWithSimpleContext(CommandHandler.CommandType.EARLY_WORLD_CREATION, world);
            }

            // Defer running the commands until the world is actually ready to load
            runCreationCommands = Configs.enablePostWorldCreationCommands;
        }
    }

    public static void onWorldLoad(World world)
    {
        if (world.isRemote == false)
        {
            final int dimension = getDimension(world);
            WorldPrimer.logInfo("WorldEvent.Load, DIM: {}", dimension);

            if (Configs.enableTimedCommands)
            {
                TimedCommands.updateTimedCommandsForDimension(dimension);
            }

            // The creation commands are only run via this method when not using dimension load count tracking
            if (runCreationCommands && dimension == 0)
            {
                WorldPrimer.logInfo("WorldEvent.Load: Running postWorldCreationCommands for DIM: {}", dimension);
                CommandHandler.INSTANCE.executeCommandsWithSimpleContext(CommandHandler.CommandType.POST_WORLD_CREATION, world);
                runCreationCommands = false;
            }

            int dimLoadCount = DataTracker.INSTANCE.dimensionLoaded(dimension);

            if (Configs.enableDimensionLoadingCommands)
            {
                WorldPrimer.logInfo("WorldEvent.Load: Running dimensionLoadingCommands");
                CommandContext ctx = new CommandContext(world, null, dimLoadCount);
                CommandHandler.INSTANCE.executeCommands(CommandHandler.CommandType.DIM_LOADING, ctx);
            }
        }
    }

    public static void onPlayerJoin(EntityPlayer player)
    {
        WorldPrimer.logInfo("PlayerLoggedInEvent: running join commands for player {}", player);
        handlePlayerEvent(player, PlayerDataType.JOIN, Configs.enablePlayerJoinCommands);

        if (Configs.enablePlayerChangedDimensionEnterCommands &&
            Configs.runDimensionChangeCommandsOnJoinQuit)
        {
            int dimension = getDimension(player);
            int currentCount = DataTracker.INSTANCE.incrementPlayerDimensionEventCount(player, dimension, PlayerDimensionDataType.ENTER);

            CommandContext ctx = new CommandContext(player.getEntityWorld(), player, currentCount);
            CommandHandler.INSTANCE.executeCommands(CommandHandler.CommandType.PLAYER_DIM_ENTER, ctx);
        }
    }

    public static void onPlayerQuit(EntityPlayer player)
    {
        WorldPrimer.logInfo("PlayerLoggedOutEvent: running quit commands for player {}", player);
        handlePlayerEvent(player, PlayerDataType.QUIT, Configs.enablePlayerQuitCommands);

        if (Configs.enablePlayerChangedDimensionLeaveCommands &&
            Configs.runDimensionChangeCommandsOnJoinQuit)
        {
            int dimension = getDimension(player);
            int currentCount = DataTracker.INSTANCE.incrementPlayerDimensionEventCount(player, dimension, PlayerDimensionDataType.LEAVE);

            CommandContext ctx = new CommandContext(player.getEntityWorld(), player, currentCount);
            CommandHandler.INSTANCE.executeCommands(CommandHandler.CommandType.PLAYER_DIM_LEAVE, ctx);
        }
    }

    public static void onPlayerDeath(EntityPlayer player)
    {
        WorldPrimer.logInfo("LivingDeathEvent: running death commands for player: {}", player);
        handlePlayerEvent(player, PlayerDataType.DEATH, Configs.enablePlayerDeathCommands);
    }

    public static void onPlayerRespawn(EntityPlayer player, boolean isEndConquered)
    {
        WorldPrimer.logInfo("PlayerRespawnEvent player: {}, leaving the End: {}", player, isEndConquered);

        // Don't run the respawn commands when just leaving the End
        if (isEndConquered == false)
        {
            handlePlayerEvent(player, PlayerDataType.RESPAWN, Configs.enablePlayerRespawnCommands);
        }
        // The PlayerChangedDimensionEvent doesn't seem to fire when leaving the End, so run the leave commands also here
        else
        {
            if (Configs.enablePlayerChangedDimensionLeaveCommands)
            {
                WorldPrimer.logInfo("PlayerRespawnEvent (leaving the End): running dimension leave commands for player {}", player);
                final int dimension = 1;
                int currentCount = DataTracker.INSTANCE.incrementPlayerDimensionEventCount(player, dimension, PlayerDimensionDataType.LEAVE);

                // Note: the dimension MUST be passed from here instead of getting it from the WorldProvider,
                // because the player is in the target dimension already at this point
                CommandContext ctx = new CommandContext(player.getEntityWorld(), player, currentCount, dimension);
                CommandHandler.INSTANCE.executeCommands(CommandHandler.CommandType.PLAYER_DIM_LEAVE, ctx);
            }

            if (Configs.enablePlayerChangedDimensionEnterCommands)
            {
                final int dimension = getDimension(player);
                int currentCount = DataTracker.INSTANCE.incrementPlayerDimensionEventCount(player, dimension, PlayerDimensionDataType.ENTER);

                CommandContext ctx = new CommandContext(player.getEntityWorld(), player, currentCount, dimension);
                CommandHandler.INSTANCE.executeCommands(CommandHandler.CommandType.PLAYER_DIM_ENTER, ctx);
            }
        }
    }

    public static void onPlayerChangedDimension(EntityPlayer player, final int fromDim, final int toDim)
    {
        WorldPrimer.logInfo("PlayerChangedDimensionEvent player: {} from dim {}, to dim {}", player, fromDim, toDim);

        if (Configs.enablePlayerChangedDimensionLeaveCommands)
        {
            WorldPrimer.logInfo("PlayerChangedDimensionEvent: running dimension leave commands for player {}", player);
            int currentCount = DataTracker.INSTANCE.incrementPlayerDimensionEventCount(player, fromDim, PlayerDimensionDataType.LEAVE);

            // Note: the dimension MUST be passed from here instead of getting it from the WorldProvider,
            // because the player is in the target dimension already at this point
            CommandContext ctx = new CommandContext(player.getEntityWorld(), player, currentCount, fromDim);
            CommandHandler.INSTANCE.executeCommands(CommandHandler.CommandType.PLAYER_DIM_LEAVE, ctx);
        }

        if (Configs.enablePlayerChangedDimensionEnterCommands)
        {
            WorldPrimer.logInfo("PlayerChangedDimensionEvent: running dimension enter commands for player {}", player);
            int currentCount = DataTracker.INSTANCE.incrementPlayerDimensionEventCount(player, toDim, PlayerDimensionDataType.ENTER);

            CommandContext ctx = new CommandContext(player.getEntityWorld(), player, currentCount, toDim);
            CommandHandler.INSTANCE.executeCommands(CommandHandler.CommandType.PLAYER_DIM_ENTER, ctx);
        }
    }

    private static void handlePlayerEvent(EntityPlayer player, PlayerDataType type, boolean enabled)
    {
        int currentCount = DataTracker.INSTANCE.incrementPlayerDataCount(player, type);

        if (enabled)
        {
            CommandContext ctx = new CommandContext(player.getEntityWorld(), player, currentCount);
            CommandHandler.INSTANCE.executeCommands(type.getCommandType(), ctx);
        }
    }
}
