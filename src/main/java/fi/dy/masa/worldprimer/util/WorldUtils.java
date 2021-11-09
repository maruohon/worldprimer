package fi.dy.masa.worldprimer.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.entity.player.EntityPlayer;
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

    @Nullable
    public static BlockPos getPlayerBedSpawnLocation(EntityPlayer player)
    {
        int dimension = player.dimension;
        BlockPos bedPos = player.getBedLocation(dimension);

        if (bedPos != null)
        {
            return EntityPlayer.getBedSpawnLocation(player.getEntityWorld(), bedPos, player.isSpawnForced(dimension));
        }

        return null;
    }

    public static int getTopYAt(World world, int x, int z)
    {
        // Load an area of 3x3 chunks around the target location, to generate the world and structures
        WorldUtils.loadChunks(world, (x >> 4) - 1, (z >> 4) - 1, (x >> 4) + 1, (z >> 4) + 1);

        // world.getTopSolidOrLiquidBlock() will return -1 over void
        final int top = Math.max(0, world.getTopSolidOrLiquidBlock(new BlockPos(x, 0, z)).getY());

        WorldUtils.unloadLoadedChunks(world);

        return top;
    }

    public static boolean executeChunkLoadingCommand(String command, @Nullable World world)
    {
        if (world == null)
        {
            WorldPrimer.LOGGER.warn("Failed to run the chunk loading command '{}', because the world wasn't loaded", command);
            return false;
        }

        String[] parts = command.split("\\s+");

        if (parts.length == 5 && (parts[0].equals("worldprimer-load-chunks") || parts[0].equals("worldprimer-load-blocks")))
        {
            try
            {
                final int x1 = Integer.parseInt(parts[1]);
                final int z1 = Integer.parseInt(parts[2]);
                final int x2 = Integer.parseInt(parts[3]);
                final int z2 = Integer.parseInt(parts[4]);

                if (parts[0].equals("worldprimer-load-blocks"))
                {
                    loadChunks(world, x1 >> 4, z1 >> 4, x2 >> 4, z2 >> 4);
                }
                else
                {
                    loadChunks(world, x1, z1, x2, z2);
                }

                return true;
            }
            catch (NumberFormatException ignore) {}
        }

        WorldPrimer.LOGGER.warn("Invalid chunk loading command '{}'", command);

        return false;
    }

    public static void loadBlocks(@Nonnull World world, int blockX1, int blockZ1, int blockX2, int blockZ2)
    {
        loadChunks(world, blockX1 >> 4, blockZ1 >> 4, blockX2 >> 4, blockZ2 >> 4);
    }

    public static void loadChunks(@Nonnull World world, int chunkX1, int chunkZ1, int chunkX2, int chunkZ2)
    {
        final int xStart = Math.min(chunkX1, chunkX2);
        final int zStart = Math.min(chunkZ1, chunkZ2);
        final int xEnd = Math.max(chunkX1, chunkX2);
        final int zEnd = Math.max(chunkZ1, chunkZ2);
        final int dimension = world.provider.getDimension();

        Set<ChunkPos> loadedChunks = LOADED_CHUNKS.computeIfAbsent(world.provider.getDimension(), k -> new HashSet<>());

        WorldPrimer.logInfo("Attempting to load chunks [{},{}] to [{},{}] in dimension {}", xStart, zStart, xEnd, zEnd, dimension);

        for (int x = xStart; x <= xEnd; x++)
        {
            for (int z = zStart; z <= zEnd; z++)
            {
                if (world.isBlockLoaded(new BlockPos(x << 4, 0, z << 4)) == false)
                {
                    WorldPrimer.logInfo("Loading chunk [{},{}] in dimension {}", x, z, dimension);
                    loadedChunks.add(new ChunkPos(x, z));
                    world.getChunk(x, z);
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
                        worldServer.getChunkProvider().queueUnload(worldServer.getChunk(pos.x, pos.z));
                    }
                }

                LOADED_CHUNKS.remove(world.provider.getDimension());
            }
        }
    }
}
