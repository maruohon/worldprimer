package fi.dy.masa.worldprimer.event;

import org.apache.commons.lang3.StringUtils;
import net.minecraft.world.World;
import net.minecraftforge.event.world.WorldEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import fi.dy.masa.worldprimer.WorldPrimer;
import fi.dy.masa.worldprimer.command.WorldPrimerCommandSender;
import fi.dy.masa.worldprimer.config.Configs;

public class EventHandler
{
    private boolean runCreationCommands;

    @SubscribeEvent
    public void onCreateSpawn(WorldEvent.CreateSpawnPosition event)
    {
        World world = event.world;

        // When creating the overworld spawn, which happens once, when the level.dat doesn't yet exist
        if (world.isRemote == false && world.provider.dimensionId == 0)
        {
            // Defer running the commands until the world is actually ready to load
            this.runCreationCommands = Configs.enableWorldCreationCommands;
        }
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event)
    {
        World world = event.world;

        if (world.isRemote == false)
        {
            int dimension = world.provider.dimensionId;

            if (dimension == 0)
            {
                if (this.runCreationCommands)
                {
                    WorldPrimerCommandSender.instance().runCommands(Configs.worldCreationCommands);
                    this.runCreationCommands = false;
                }

                if (Configs.enableWorldLoadingCommands)
                {
                    WorldPrimerCommandSender.instance().runCommands(Configs.worldLoadingCommands);
                }
            }

            if (Configs.enableDimensionLoadingCommands)
            {
                this.runDimensionLoadingCommands(dimension);
            }
        }
    }

    private void runDimensionLoadingCommands(int dimension)
    {
        String[] commands = Configs.dimensionLoadingCommands;

        for (String command : commands)
        {
            if (StringUtils.isBlank(command))
            {
                continue;
            }

            String[] parts = command.split(" ");

            if (parts.length < 3)
            {
                WorldPrimerCommandSender.instance().runCommands(command);
            }
            else if (parts[0].equals("worldprimer-dim-command"))
            {
                try
                {
                    int dim = Integer.parseInt(parts[1]);

                    if (dimension == dim)
                    {
                        parts = dropFirstStrings(parts, 2);
                        WorldPrimerCommandSender.instance().runCommands(String.join(" ", parts));
                    }
                }
                catch (NumberFormatException e)
                {
                    WorldPrimer.logger.warn("Invalid dimension id '{}' in dimension-specific command '{}'", parts[1], command);
                }
            }
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
