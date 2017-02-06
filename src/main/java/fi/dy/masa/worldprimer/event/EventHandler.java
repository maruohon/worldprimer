package fi.dy.masa.worldprimer.event;

import net.minecraft.world.World;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import fi.dy.masa.worldprimer.command.WorldPrimerCommandSender;
import fi.dy.masa.worldprimer.config.Configs;

public class EventHandler
{
    private boolean runCreationCommands;

    @SubscribeEvent
    public void onCreateSpawn(WorldEvent.CreateSpawnPosition event)
    {
        World world = event.getWorld();

        // When creating the overworld spawn, which happens once, when the level.dat doesn't yet exist
        if (world.isRemote == false && world.provider.getDimension() == 0)
        {
            // Defer running the commands until the world is actually ready to load
            this.runCreationCommands = true;
        }
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event)
    {
        World world = event.getWorld();

        if (this.runCreationCommands && world.isRemote == false && world.provider.getDimension() == 0)
        {
            WorldPrimerCommandSender.instance().runCommands(Configs.worldCreationCommands);
            this.runCreationCommands = false;
        }
    }
}
