package fi.dy.masa.worldprimer.event;

import javax.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerChangedDimensionEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerRespawnEvent;
import fi.dy.masa.worldprimer.WorldPrimer;
import fi.dy.masa.worldprimer.command.WorldPrimerCommandSender;
import fi.dy.masa.worldprimer.config.Configs;
import fi.dy.masa.worldprimer.util.DataTracker;
import fi.dy.masa.worldprimer.util.DataTracker.PlayerDataType;
import fi.dy.masa.worldprimer.util.DataTracker.PlayerDimensionDataType;

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
        if (Configs.enableDataTracking == false &&
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

            if (Configs.enableDataTracking)
            {
                DataTracker.instance().dimensionLoaded(dimension);
            }

            if (Configs.enableDimensionLoadingCommands)
            {
                WorldPrimer.logInfo("WorldEvent.Load - running dimensionLoadingCommands");
                final int currentCount = DataTracker.instance().getDimensionLoadCount(dimension);
                this.runDimensionCommands(null, world, world.provider.getDimension(), currentCount, Configs.dimensionLoadingCommands);
            }
        }
    }

    @SubscribeEvent
    public void onWorldSave(WorldEvent.Save event)
    {
        DataTracker.instance().writeToDisk();
    }

    @SubscribeEvent
    public void onPlayerJoin(PlayerLoggedInEvent event)
    {
        WorldPrimer.logInfo("PlayerLoggedInEvent player: {}", event.player);
        this.handlePlayerEvent(event.player, PlayerDataType.JOIN, Configs.enablePlayerJoinCommands, Configs.playerJoinCommands);
    }

    @SubscribeEvent
    public void onPlayerQuit(PlayerLoggedOutEvent event)
    {
        WorldPrimer.logInfo("PlayerLoggedOutEvent player: {}", event.player);
        this.handlePlayerEvent(event.player, PlayerDataType.QUIT, Configs.enablePlayerQuitCommands, Configs.playerQuitCommands);
    }

    @SubscribeEvent
    public void onPlayerDeath(LivingDeathEvent event)
    {
        if (event.getEntityLiving() instanceof EntityPlayer)
        {
            WorldPrimer.logInfo("LivingDeathEvent for player: {}", event.getEntityLiving());
            this.handlePlayerEvent((EntityPlayer) event.getEntityLiving(), PlayerDataType.DEATH, Configs.enablePlayerDeathCommands, Configs.playerDeathCommands);
        }
    }

    @SubscribeEvent
    public void onPlayerRespawn(PlayerRespawnEvent event)
    {
        // Don't run the respawn commands when just leaving the End
        if (event.isEndConquered() == false)
        {
            WorldPrimer.logInfo("PlayerRespawnEvent player: {}", event.player);
            this.handlePlayerEvent(event.player, PlayerDataType.RESPAWN, Configs.enablePlayerRespawnCommands, Configs.playerRespawnCommands);
        }
        // The PlayerChangedDimensionEvent doesn't seem to fire when leaving the End, so run the leave commands also here
        else if (Configs.enablePlayerChangedDimensionLeaveCommands)
        {
            final EntityPlayer player = event.player;
            final int dimension = 1;
            WorldPrimer.logInfo("PlayerRespawnEvent player: {}", player);
            DataTracker.instance().incrementPlayerDimensionEventCount(player, dimension, PlayerDimensionDataType.LEAVE);
            final int currentCount = DataTracker.instance().getPlayerDimensionEventCount(player, dimension, PlayerDimensionDataType.LEAVE);

            // Note: the dimension MUST be passed from here instead of getting it from the WorldProvider,
            // because the player is in the target dimension already at this point
            this.runDimensionCommands(player, player.getEntityWorld(), dimension, currentCount, Configs.playerChangedDimensionLeaveCommands);
        }
    }

    @SubscribeEvent
    public void onPlayerChangedDimension(PlayerChangedDimensionEvent event)
    {
        final EntityPlayer player = event.player;

        if (Configs.enablePlayerChangedDimensionLeaveCommands)
        {
            final int dimension = event.fromDim;
            WorldPrimer.logInfo("PlayerChangedDimensionEvent player: {} left dim {}", event.player, dimension);
            DataTracker.instance().incrementPlayerDimensionEventCount(player, dimension, PlayerDimensionDataType.LEAVE);
            final int currentCount = DataTracker.instance().getPlayerDimensionEventCount(player, dimension, PlayerDimensionDataType.LEAVE);

            // Note: the dimension MUST be passed from here instead of getting it from the WorldProvider,
            // because the player is in the target dimension already at this point
            this.runDimensionCommands(player, player.getEntityWorld(), dimension, currentCount, Configs.playerChangedDimensionLeaveCommands);
        }

        if (Configs.enablePlayerChangedDimensionEnterCommands)
        {
            final int dimension = event.toDim;
            WorldPrimer.logInfo("PlayerChangedDimensionEvent player: {} entered dim {}", event.player, dimension);
            DataTracker.instance().incrementPlayerDimensionEventCount(player, dimension, PlayerDimensionDataType.ENTER);
            final int currentCount = DataTracker.instance().getPlayerDimensionEventCount(player, dimension, PlayerDimensionDataType.ENTER);

            this.runDimensionCommands(player, player.getEntityWorld(), dimension, currentCount, Configs.playerChangedDimensionEnterCommands);
        }
    }

    private void runDimensionCommands(@Nullable EntityPlayer player, World world, final int dimension, final int currentCount, String[] commands)
    {
        for (String command : commands)
        {
            if (StringUtils.isBlank(command))
            {
                continue;
            }

            String[] parts = command.split(" ");

            if (parts.length >= 3 && parts[0].equals("worldprimer-dim-command"))
            {
                this.runDimensionCommandsRegular(player, world, dimension, command, parts);
            }
            else if (Configs.enableDataTracking && parts.length >= 4 && parts[0].equals("worldprimer-dim-command-nth"))
            {
                this.runDimensionCommandsNth(player, world, dimension, command, parts, currentCount);
            }
            else
            {
                WorldPrimerCommandSender.instance().runCommands(player, world, command);
            }
        }
    }

    private void runDimensionCommandsRegular(@Nullable EntityPlayer player, World world, final int dimension,
            String fullCommand, String[] cmdParts)
    {
        try
        {
            int dim = Integer.parseInt(cmdParts[1]);

            if (dimension == dim)
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

    private void runDimensionCommandsNth(@Nullable EntityPlayer player, World world, final int dimension,
            String fullCommand, String[] cmdParts, final int currentCount)
    {
        try
        {
            final int parsedDim = Integer.parseInt(cmdParts[1]);

            if (dimension == parsedDim)
            {
                String countStr = cmdParts[2];
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

    private void handlePlayerEvent(EntityPlayer player, PlayerDataType type, boolean enabled, String[] commands)
    {
        if (enabled)
        {
            DataTracker.instance().incrementPlayerDataCount(player, type);
            final int currentCount = DataTracker.instance().getPlayerDataCount(player, type);
            this.runTrackedCommands(player, commands, currentCount);
        }
    }

    private void runTrackedCommands(EntityPlayer player, String[] commands, int currentCount)
    {
        for (String command : commands)
        {
            if (StringUtils.isBlank(command))
            {
                continue;
            }

            String[] parts = command.split(" ");

            if (Configs.enableDataTracking && parts.length >= 3 && parts[0].equals("worldprimer-tracked-nth"))
            {
                this.runTrackedCommandsNth(player, command, parts, currentCount);
            }
            else
            {
                WorldPrimerCommandSender.instance().runCommands(player, player.getEntityWorld(), command);
            }
        }
    }

    private void runTrackedCommandsNth(EntityPlayer player, String fullCommand, String[] cmdParts, int currentCount)
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
