package fi.dy.masa.worldprimer.util;

import java.util.Set;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import fi.dy.masa.worldprimer.WorldPrimer;

public class WorldUtils
{
    public static void loadChunks(World world, int chunkX1, int chunkZ1, int chunkX2, int chunkZ2, Set<ChunkPos> loadedChunks)
    {
        final int xStart = Math.min(chunkX1, chunkX2);
        final int zStart = Math.min(chunkZ1, chunkZ2);
        final int xEnd = Math.max(chunkX1, chunkX2);
        final int zEnd = Math.max(chunkZ1, chunkZ2);
        final int dimension = world.provider.getDimension();

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

    public static void unloadLoadedChunks(World world, Set<ChunkPos> loadedChunks)
    {
        if (world instanceof WorldServer)
        {
            WorldServer worldServer = (WorldServer) world;

            for (ChunkPos pos : loadedChunks)
            {
                if (worldServer.getPlayerChunkMap().contains(pos.x, pos.z) == false &&
                    worldServer.isBlockLoaded(new BlockPos(pos.x << 4, 0, pos.z << 4)))
                {
                    WorldPrimer.logInfo("Queueing chunk [{},{}] for unloading in dimension {}", pos.x, pos.z, world.provider.getDimension());
                    worldServer.getChunkProvider().queueUnload(worldServer.getChunkFromChunkCoords(pos.x, pos.z));
                }
            }
        }

        loadedChunks.clear();
    }
}
