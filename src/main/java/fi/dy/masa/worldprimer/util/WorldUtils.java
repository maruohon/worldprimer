package fi.dy.masa.worldprimer.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import fi.dy.masa.worldprimer.WorldPrimer;

public class WorldUtils
{
    private static final Map<Integer, Set<ChunkPos>> LOADED_CHUNKS = new HashMap<>();

    public static BlockPos getWorldSpawn(World world)
    {
        BlockPos spawn = null;

        if (world instanceof WorldServer)
        {
            spawn = ((WorldServer) world).getSpawnCoordinate();
        }

        if (spawn == null)
        {
            spawn = world.getSpawnPoint();
        }

        return spawn;
    }

    public static boolean executeChunkLoadingCommand(String command, @Nullable World world)
    {
        if (world == null)
        {
            WorldPrimer.logger.warn("Failed to run the chunk loading command '{}', because the world wasn't loaded", command);
            return false;
        }

        String[] parts = command.split("\\s+");

        if (parts.length == 5 && parts[0].equals("worldprimer-load-chunks"))
        {
            try
            {
                final int x1 = Integer.parseInt(parts[1]);
                final int z1 = Integer.parseInt(parts[2]);
                final int x2 = Integer.parseInt(parts[3]);
                final int z2 = Integer.parseInt(parts[4]);
                loadChunks(world, x1, z1, x2, z2);

                return true;
            }
            catch (NumberFormatException e)
            {
            }
        }

        WorldPrimer.logger.warn("Invalid chunk loading command '{}'", command);

        return false;
    }

    public static void loadChunks(@Nonnull World world, int chunkX1, int chunkZ1, int chunkX2, int chunkZ2)
    {
        final int xStart = Math.min(chunkX1, chunkX2);
        final int zStart = Math.min(chunkZ1, chunkZ2);
        final int xEnd = Math.max(chunkX1, chunkX2);
        final int zEnd = Math.max(chunkZ1, chunkZ2);
        final int dimension = world.provider.getDimension();

        Set<ChunkPos> loadedChunks = LOADED_CHUNKS.get(world.provider.getDimension());

        if (loadedChunks == null)
        {
            loadedChunks = new HashSet<ChunkPos>();
            LOADED_CHUNKS.put(world.provider.getDimension(), loadedChunks);
        }

        WorldPrimer.logInfo("Attempting to load chunks [{},{}] to [{},{}] in dimension {}", xStart, zStart, xEnd, zEnd, dimension);

        for (int x = xStart; x <= xEnd; x++)
        {
            for (int z = zStart; z <= zEnd; z++)
            {
                if (world.isBlockLoaded(new BlockPos(x << 4, 0, z << 4)) == false)
                {
                    WorldPrimer.logInfo("Loading chunk [{},{}] in dimension {}", x, z, dimension);
                    loadedChunks.add(new ChunkPos(x, z));
                    world.getChunkFromChunkCoords(x, z);
                }
            }
        }
    }

    public static void unloadLoadedChunks(@Nullable World world)
    {
        if (world instanceof WorldServer)
        {
            WorldServer worldServer = (WorldServer) world;
            Set<ChunkPos> loadedChunks = LOADED_CHUNKS.get(world.provider.getDimension());

            if (loadedChunks != null)
            {
                for (ChunkPos pos : loadedChunks)
                {
                    if (worldServer.getPlayerChunkMap().contains(pos.x, pos.z) == false &&
                        worldServer.isBlockLoaded(new BlockPos(pos.x << 4, 0, pos.z << 4)))
                    {
                        WorldPrimer.logInfo("Queueing chunk [{},{}] for unloading in dimension {}", pos.x, pos.z, world.provider.getDimension());
                        worldServer.getChunkProvider().queueUnload(worldServer.getChunkFromChunkCoords(pos.x, pos.z));
                    }
                }

                LOADED_CHUNKS.remove(world.provider.getDimension());
            }
        }
    }
}
