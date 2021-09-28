package fi.dy.masa.worldprimer.event;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerChangedDimensionEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerRespawnEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ServerTickEvent;
import net.minecraftforge.fml.relauncher.Side;
import fi.dy.masa.worldprimer.WorldPrimer;
import fi.dy.masa.worldprimer.command.util.CommandUtils;
import fi.dy.masa.worldprimer.command.util.TimedCommands;
import fi.dy.masa.worldprimer.config.Configs;
import fi.dy.masa.worldprimer.util.DataTracker;

public class EventHandler
{
    @SubscribeEvent
    public void onCreateSpawn(WorldEvent.CreateSpawnPosition event)
    {
        CommandUtils.onCreateSpawn(event.getWorld());
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event)
    {
        CommandUtils.onWorldLoad(event.getWorld());
    }

    @SubscribeEvent
    public void onWorldSave(WorldEvent.Save event)
    {
        DataTracker.instance().writeToDisk();
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent event)
    {
        if (Configs.enableTimedCommands && event.side == Side.SERVER && event.phase == TickEvent.Phase.END)
        {
            TimedCommands.runTimedCommands();
        }
    }

    @SubscribeEvent
    public void onPlayerJoin(PlayerLoggedInEvent event)
    {
        CommandUtils.onPlayerJoin(event.player);
    }

    @SubscribeEvent
    public void onPlayerQuit(PlayerLoggedOutEvent event)
    {
        CommandUtils.onPlayerQuit(event.player);
    }

    @SubscribeEvent
    public void onPlayerDeath(LivingDeathEvent event)
    {
        if (event.getEntityLiving() instanceof EntityPlayer)
        {
            CommandUtils.onPlayerDeath((EntityPlayer) event.getEntityLiving());
        }
    }

    @SubscribeEvent
    public void onPlayerRespawn(PlayerRespawnEvent event)
    {
        CommandUtils.onPlayerRespawn(event.player, event.isEndConquered());
    }

    @SubscribeEvent
    public void onPlayerChangedDimension(PlayerChangedDimensionEvent event)
    {
        CommandUtils.onPlayerChangedDimension(event.player, event.fromDim, event.toDim);
    }

    @SubscribeEvent
    public void onCommand(CommandEvent event)
    {
        if ("jed".equals(event.getCommand().getName()))
        {
            String[] args = event.getParameters();

            if (args.length == 3 && args[0].equals("broadcast") && args[1].equals("delete-dimension"))
            {
                try
                {
                    int dimension = Integer.parseInt(args[2]);
                    WorldPrimer.logInfo("EventHandler.onCommand: Saw a dimension deletion broadcast " +
                                        "command from JED, resetting the dimension load count for dimension {}", dimension);
                    DataTracker.instance().resetDimensionLoadCountFor(dimension);
                }
                catch (NumberFormatException e)
                {
                    WorldPrimer.LOGGER.warn("EventHandler.onCommand: Invalid dimension argument in 'jed broadcast " +
                                            "delete-dimension <dim>' command: '{}'", args[2]);
                }
            }
        }
    }
}
