package fi.dy.masa.worldprimer.command;

import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;
import net.minecraft.command.ICommandManager;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.gen.ChunkProviderServer;
import cpw.mods.fml.common.FMLCommonHandler;
import fi.dy.masa.worldprimer.WorldPrimer;
import fi.dy.masa.worldprimer.reference.Reference;

public class WorldPrimerCommandSender implements ICommandSender
{
    private static final ChunkCoordinates ORIGIN = new ChunkCoordinates(0, 0, 0);
    private static final IChatComponent DISPLAY_NAME = new ChatComponentText(Reference.MOD_NAME + " CommandSender");
    private static final WorldPrimerCommandSender INSTANCE = new WorldPrimerCommandSender();
    private World executionWorld;
    private final Set<ChunkCoordIntPair> loadedChunks = new HashSet<>();

    public static WorldPrimerCommandSender instance()
    {
        return INSTANCE;
    }

    public void runCommands(@Nullable World world, String... commands)
    {
        ICommandManager manager = FMLCommonHandler.instance().getMinecraftServerInstance().getCommandManager();
        this.executionWorld = world;

        for (String command : commands)
        {
            if (StringUtils.isBlank(command) == false)
            {
                String newCommand = this.doCommandSubstitutions(world, command);
                World worldTmp = this.getEntityWorld();
                String dim = worldTmp != null ? String.valueOf(worldTmp.provider.dimensionId) : "<none>";

                if (this.isChunkLoadingCommand(newCommand))
                {
                    WorldPrimer.logInfo("Attempting to load chunks in dimension {}", dim);
                    this.executeChunkLoadingCommand(newCommand, worldTmp);
                }
                else
                {
                    WorldPrimer.logInfo("Running a (possibly substituted) command: '{}' in dimension {}", newCommand, dim);
                    manager.executeCommand(this, newCommand);
                }
            }
        }

        this.unloadLoadedChunks(world);

        this.executionWorld = null;
    }

    private String doCommandSubstitutions(@Nullable World world, String originalCommand)
    {
        if (world == null)
        {
            return originalCommand;
        }

        ChunkCoordinates spawn = null;

        if (world instanceof WorldServer)
        {
            spawn = ((WorldServer) world).getSpawnPoint();
        }

        if (spawn == null)
        {
            spawn = world.getSpawnPoint();
        }

        int dim = world.provider.dimensionId;
        String[] parts = originalCommand.split(" ");

        for (int i = 0; i < parts.length; i++)
        {
            parts[i] = this.substituteNumber(parts[i], "{DIMENSION}", dim);
            parts[i] = this.substituteNumber(parts[i], "{SPAWNX}", spawn.posX);
            parts[i] = this.substituteNumber(parts[i], "{SPAWNY}", spawn.posY);
            parts[i] = this.substituteNumber(parts[i], "{SPAWNZ}", spawn.posZ);
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

    private boolean isChunkLoadingCommand(String command)
    {
        String[] parts = command.split(" ");
        return parts.length > 0 && parts[0].equals("worldprimer-load-chunks");
    }

    private boolean executeChunkLoadingCommand(String command, @Nullable World world)
    {
        if (world == null)
        {
            WorldPrimer.logger.warn("Failed to run the chunk loading command '{}', because the world wasn't loaded", command);
            return false;
        }

        String[] parts = command.split(" ");

        if (parts.length == 5 && parts[0].equals("worldprimer-load-chunks"))
        {
            try
            {
                final int x1 = Integer.parseInt(parts[1]);
                final int z1 = Integer.parseInt(parts[2]);
                final int x2 = Integer.parseInt(parts[3]);
                final int z2 = Integer.parseInt(parts[4]);
                final int xStart = Math.min(x1, x2);
                final int zStart = Math.min(z1, z2);
                final int xEnd = Math.max(x1, x2);
                final int zEnd = Math.max(z1, z2);
                int dimension = world.provider.dimensionId;

                WorldPrimer.logInfo("Attempting to load chunks [{},{}] to [{},{}] in dimension {}", xStart, zStart, xEnd, zEnd, dimension);

                for (int x = xStart; x <= xEnd; x++)
                {
                    for (int z = zStart; z <= zEnd; z++)
                    {
                        WorldPrimer.logInfo("Loading chunk [{},{}] in dimension {}", x, z, dimension);
                        this.loadedChunks.add(new ChunkCoordIntPair(x, z));
                        world.getChunkFromChunkCoords(x, z);
                    }
                }

                return true;
            }
            catch (NumberFormatException e)
            {
            }
        }

        WorldPrimer.logger.warn("Invalid chunk loading command '{}'", command);

        return false;
    }

    private void unloadLoadedChunks(World world)
    {
        if (world instanceof WorldServer)
        {
            WorldServer worldServer = (WorldServer) world;

            for (ChunkCoordIntPair pos : this.loadedChunks)
            {
                if (worldServer.getPlayerManager().func_152621_a(pos.chunkXPos, pos.chunkZPos) == false &&
                    worldServer.blockExists(pos.chunkXPos << 4, 0, pos.chunkZPos << 4))
                {
                    WorldPrimer.logInfo("Queueing chunk [{},{}] for unloading in dimension {}", pos.chunkXPos, pos.chunkZPos, world.provider.dimensionId);
                    ((ChunkProviderServer) worldServer.getChunkProvider()).unloadChunksIfNotNearSpawn(pos.chunkXPos, pos.chunkZPos);
                }
            }
        }

        this.loadedChunks.clear();
    }

    @Override
    public String getCommandSenderName()
    {
        return Reference.MOD_NAME + " CommandSender";
    }

    @Override
    public IChatComponent func_145748_c_()
    {
        return DISPLAY_NAME;
    }

    @Override
    public void addChatMessage(IChatComponent component)
    {
        WorldPrimer.logInfo(component.getUnformattedText());
    }

    @Override
    public boolean canCommandSenderUseCommand(int permLevel, String commandName)
    {
        return true;
    }

    @Override
    public ChunkCoordinates getPlayerCoordinates()
    {
        return ORIGIN;
    }

    @Override
    public World getEntityWorld()
    {
        return this.executionWorld != null ? this.executionWorld : FMLCommonHandler.instance().getMinecraftServerInstance().worldServers[0];
    }
}
