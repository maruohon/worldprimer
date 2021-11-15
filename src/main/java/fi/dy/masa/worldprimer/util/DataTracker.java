package fi.dy.masa.worldprimer.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.util.Constants;
import fi.dy.masa.worldprimer.WorldPrimer;
import fi.dy.masa.worldprimer.command.handler.CommandHandler;
import fi.dy.masa.worldprimer.config.Configs;
import fi.dy.masa.worldprimer.reference.Reference;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;

public class DataTracker
{
    public static final DataTracker INSTANCE = new DataTracker();

    private final Int2IntOpenHashMap dimensionLoadCounts = new Int2IntOpenHashMap();
    private final Map<UUID, PlayerData> playerData = new HashMap<>();
    private final Map<Integer, Map<UUID, BlockPos>> playerSpreadPositions = new HashMap<>();
    private File worldDir = new File(".");
    private int serverStarts;
    private boolean dirty;

    public DataTracker()
    {
        this.dimensionLoadCounts.defaultReturnValue(0);
    }

    public int getServerStartCount()
    {
        return this.serverStarts;
    }

    public int getDimensionLoadCount(int dimension)
    {
        return this.dimensionLoadCounts.get(dimension);
    }

    public int getPlayerDataCount(EntityPlayer player, PlayerDataType type)
    {
        return this.getOrCreatePlayerData(player).getCount(type);
    }

    public int getPlayerDimensionEventCount(EntityPlayer player, int dimension, PlayerDimensionDataType type)
    {
        return this.getOrCreatePlayerData(player).getDimensionEventCount(dimension, type);
    }

    public Collection<BlockPos> getPlayerSpreadPositions(int dimension)
    {
        Map<UUID, BlockPos> map = this.playerSpreadPositions.get(dimension);
        return map != null ? map.values() : Collections.emptyList();
    }

    @Nullable
    public BlockPos getLastPlayerSpreadPosition(EntityPlayer player)
    {
        int dimension = player.getEntityWorld().provider.getDimension();
        Map<UUID, BlockPos> map = this.playerSpreadPositions.get(dimension);
        return map != null ? map.get(player.getUniqueID()) : null;
    }

    public void incrementServerStartCount()
    {
        this.serverStarts++;
        this.dirty = true;
    }

    public int incrementDimensionLoadCount(int dimension)
    {
        int count = this.dimensionLoadCounts.get(dimension) + 1;
        this.dimensionLoadCounts.put(dimension, count);
        this.dirty = true;

        return count;
    }

    public void resetDimensionLoadCountFor(int dimension)
    {
        this.dimensionLoadCounts.remove(dimension);

        for (PlayerData data : this.playerData.values())
        {
            data.resetDimensionEventCountsFor(dimension);
        }

        this.dirty = true;
    }

    public int incrementPlayerDataCount(EntityPlayer player, PlayerDataType type)
    {
        PlayerData data = this.getOrCreatePlayerData(player);
        int newValue = data.incrementCount(type);
        this.dirty = true;
        return newValue;
    }

    public int incrementPlayerDimensionEventCount(EntityPlayer player, int dimension, PlayerDimensionDataType type)
    {
        PlayerData data = this.getOrCreatePlayerData(player);
        int newValue = data.incrementDimensionEventCount(dimension, type);
        this.dirty = true;
        return newValue;
    }

    public void addPlayerSpreadPosition(EntityPlayer player, BlockPos pos)
    {
        int dimension = player.getEntityWorld().provider.getDimension();
        this.addPlayerSpreadPosition(dimension, player.getUniqueID(), pos);
    }

    public void addPlayerSpreadPosition(int dimension, UUID uuid, BlockPos pos)
    {
        Map<UUID, BlockPos> map = this.playerSpreadPositions.computeIfAbsent(dimension, k -> new HashMap<>());
        map.put(uuid, pos);
        this.dirty = true;
    }

    private PlayerData getOrCreatePlayerData(EntityPlayer player)
    {
        PlayerData data = this.playerData.get(player.getUniqueID());

        if (data == null)
        {
            data = new PlayerData();
            this.playerData.put(player.getUniqueID(), data);
            this.dirty = true;
        }

        return data;
    }

    private void readFromNBT(NBTTagCompound nbt)
    {
        if (nbt == null)
        {
            return;
        }

        NBTTagList tagList = nbt.getTagList("DimLoadCounts", Constants.NBT.TAG_COMPOUND);
        int tagCount = tagList.tagCount();

        for (int i = 0; i < tagCount; i++)
        {
            NBTTagCompound tag = tagList.getCompoundTagAt(i);
            this.dimensionLoadCounts.put(tag.getInteger("Dim"), tag.getInteger("Count"));
        }

        tagList = nbt.getTagList("PlayerData", Constants.NBT.TAG_COMPOUND);
        tagCount = tagList.tagCount();

        for (int i = 0; i < tagCount; i++)
        {
            NBTTagCompound tag = tagList.getCompoundTagAt(i);
            UUID uuid = new UUID(tag.getLong("UUIDM"), tag.getLong("UUIDL"));
            PlayerData data = new PlayerData();
            data.readFromNBT(tag);
            this.playerData.put(uuid, data);
        }

        tagList = nbt.getTagList("PlayerSpreadPositions", Constants.NBT.TAG_COMPOUND);
        tagCount = tagList.tagCount();

        for (int i = 0; i < tagCount; i++)
        {
            NBTTagCompound tag = tagList.getCompoundTagAt(i);
            BlockPos pos = NbtUtils.readBlockPos(tag);

            if (pos != null)
            {
                UUID uuid = new UUID(tag.getLong("UUIDM"), tag.getLong("UUIDL"));
                int dimension = tag.getInteger("dim");
                this.addPlayerSpreadPosition(dimension, uuid, pos);
            }
        }

        this.serverStarts = nbt.getInteger("ServerStarts");
    }

    private NBTTagCompound writeToNBT(@Nonnull NBTTagCompound nbt)
    {
        NBTTagList tagList = new NBTTagList();
        nbt.setTag("DimLoadCounts", tagList);

        for (Map.Entry<Integer, Integer> entry : this.dimensionLoadCounts.entrySet())
        {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setInteger("Dim", entry.getKey());
            tag.setInteger("Count", entry.getValue());
            tagList.appendTag(tag);
        }

        tagList = new NBTTagList();
        nbt.setTag("PlayerData", tagList);

        for (Map.Entry<UUID, PlayerData> entry : this.playerData.entrySet())
        {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setLong("UUIDM", entry.getKey().getMostSignificantBits());
            tag.setLong("UUIDL", entry.getKey().getLeastSignificantBits());
            entry.getValue().writeToNBT(tag);
            tagList.appendTag(tag);
        }

        tagList = new NBTTagList();
        nbt.setTag("PlayerSpreadPositions", tagList);

        for (Map.Entry<Integer, Map<UUID, BlockPos>> entry : this.playerSpreadPositions.entrySet())
        {
            Map<UUID, BlockPos> map = entry.getValue();
            int dimension = entry.getKey();

            for (Map.Entry<UUID, BlockPos> posEntry : map.entrySet())
            {
                NBTTagCompound tag = new NBTTagCompound();
                tag.setLong("UUIDM", posEntry.getKey().getMostSignificantBits());
                tag.setLong("UUIDL", posEntry.getKey().getLeastSignificantBits());
                tag.setInteger("dim", dimension);
                NbtUtils.writeBlockPosToTag(posEntry.getValue(), tag);
                tagList.appendTag(tag);
            }
        }

        nbt.setInteger("ServerStarts", this.serverStarts);

        return nbt;
    }

    @Nullable
    private File getDataDir()
    {
        File saveDir = this.worldDir;

        if (saveDir != null)
        {
            saveDir = new File(saveDir, Reference.MOD_ID);

            if (saveDir.exists() == false && saveDir.mkdirs() == false)
            {
                WorldPrimer.LOGGER.warn("Failed to create a directory for storing the data tracker file '{}'",
                                        saveDir.getPath());
                return null;
            }

            return saveDir;
        }

        return null;
    }

    public void readFromDisk(File worldDir)
    {
        if (Configs.enableDataTracking == false)
        {
            return;
        }

        // Clear old data regardless of whether there is a data file
        this.dimensionLoadCounts.clear();
        this.playerData.clear();
        this.playerSpreadPositions.clear();
        this.serverStarts = 0;

        this.worldDir = worldDir;
        File file = new File(new File(worldDir, Reference.MOD_ID), "data_tracker.nbt");

        // Compatibility for reading from the old file - remove at some point
        if (file.exists() == false || file.isFile() == false)
        {
            file = new File(new File(worldDir, Reference.MOD_ID), "dim_loads.nbt");
        }

        try
        {
            if (file.exists() && file.isFile() && file.canRead())
            {
                FileInputStream is = new FileInputStream(file);
                this.readFromNBT(CompressedStreamTools.readCompressed(is));
                is.close();
            }
        }
        catch (Exception e)
        {
            WorldPrimer.LOGGER.warn("Failed to read tracker data from file '{}'", file.getPath());
        }
    }

    public void writeToDisk()
    {
        if (this.dirty == false || Configs.enableDataTracking == false)
        {
            return;
        }

        try
        {
            File saveDir = this.getDataDir();

            if (saveDir != null)
            {
                File fileTmp  = new File(saveDir, "data_tracker.nbt.tmp");
                File fileReal = new File(saveDir, "data_tracker.nbt");
                FileOutputStream os = new FileOutputStream(fileTmp);
                CompressedStreamTools.writeCompressed(this.writeToNBT(new NBTTagCompound()), os);
                os.close();

                if (fileReal.exists() && fileReal.delete() == false)
                {
                    WorldPrimer.LOGGER.warn("Failed to remove old data file '{}'", fileReal.getAbsolutePath());
                }

                if (fileTmp.renameTo(fileReal) == false)
                {
                    WorldPrimer.LOGGER.warn("Failed to rename data file '{}' to '{}'",
                                            fileTmp.getAbsolutePath(), fileReal.getAbsolutePath());
                }

                this.dirty = false;
            }
        }
        catch (Exception e)
        {
            WorldPrimer.LOGGER.warn("Failed to write tracker data to file");
        }
    }

    public static class PlayerData
    {
        private final Int2IntOpenHashMap dimensionEnterCounts = new Int2IntOpenHashMap();
        private final Int2IntOpenHashMap dimensionLeaveCounts = new Int2IntOpenHashMap();
        private int joinCount;
        private int quitCount;
        private int deathCount;
        private int respawnCount;

        public PlayerData()
        {
            this.dimensionEnterCounts.defaultReturnValue(-1);
            this.dimensionLeaveCounts.defaultReturnValue(-1);
        }

        public int getCount(PlayerDataType type)
        {
            return type.getInt(this);
        }

        public int incrementCount(PlayerDataType type)
        {
            return type.increment(this);
        }

        public int getDimensionEventCount(int dimension, PlayerDimensionDataType type)
        {
            Int2IntOpenHashMap map = type == PlayerDimensionDataType.ENTER ? this.dimensionEnterCounts : this.dimensionLeaveCounts;
            int count = map.get(dimension);
            return count != -1 ? count : 0;
        }

        public int incrementDimensionEventCount(int dimension, PlayerDimensionDataType type)
        {
            Int2IntOpenHashMap map = type == PlayerDimensionDataType.ENTER ? this.dimensionEnterCounts : this.dimensionLeaveCounts;
            int oldValue = map.get(dimension);
            int newValue = oldValue != -1 ? oldValue + 1 : 1;

            map.put(dimension, newValue);

            return newValue;
        }

        public void resetDimensionEventCountsFor(int dimension)
        {
            this.dimensionEnterCounts.remove(dimension);
            this.dimensionLeaveCounts.remove(dimension);
        }

        public void readFromNBT(NBTTagCompound nbt)
        {
            this.joinCount = nbt.getInteger("JoinCount");
            this.quitCount = nbt.getInteger("QuitCount");
            this.deathCount = nbt.getInteger("DeathCount");
            this.respawnCount = nbt.getInteger("RespawnCount");

            this.readDimensionEventData(nbt, "DimEnter", this.dimensionEnterCounts);
            this.readDimensionEventData(nbt, "DimLeave", this.dimensionLeaveCounts);
        }

        private void readDimensionEventData(NBTTagCompound nbt, String key, Map<Integer, Integer> map)
        {
            map.clear();

            if (nbt.hasKey(key, Constants.NBT.TAG_LIST))
            {
                NBTTagList tagList = nbt.getTagList(key, Constants.NBT.TAG_COMPOUND);
                final int tagCount = tagList.tagCount();

                for (int i = 0; i < tagCount; i++)
                {
                    NBTTagCompound tag = tagList.getCompoundTagAt(i);
                    map.put(tag.getInteger("Dim"), tag.getInteger("Count"));
                }
            }
        }

        public NBTTagCompound writeToNBT(@Nonnull NBTTagCompound nbt)
        {
            nbt.setInteger("JoinCount", this.joinCount);
            nbt.setInteger("QuitCount", this.quitCount);
            nbt.setInteger("DeathCount", this.deathCount);
            nbt.setInteger("RespawnCount", this.respawnCount);

            this.writeDimensionEventData(nbt, "DimEnter", this.dimensionEnterCounts);
            this.writeDimensionEventData(nbt, "DimLeave", this.dimensionLeaveCounts);

            return nbt;
        }

        private void writeDimensionEventData(NBTTagCompound nbt, String key, Map<Integer, Integer> map)
        {
            NBTTagList tagList = new NBTTagList();

            for (Map.Entry<Integer, Integer> entry : map.entrySet())
            {
                NBTTagCompound tag = new NBTTagCompound();
                tag.setInteger("Dim", entry.getKey());
                tag.setInteger("Count", entry.getValue());
                tagList.appendTag(tag);
            }

            nbt.setTag(key, tagList);
        }
    }

    public enum PlayerDataType
    {
        JOIN    ((d) -> d.joinCount,    (d, v) -> d.joinCount = v   , () -> CommandHandler.CommandType.PLAYER_JOIN),
        QUIT    ((d) -> d.quitCount,    (d, v) -> d.quitCount = v   , () -> CommandHandler.CommandType.PLAYER_QUIT),
        DEATH   ((d) -> d.deathCount,   (d, v) -> d.deathCount = v  , () -> CommandHandler.CommandType.PLAYER_DEATH),
        RESPAWN ((d) -> d.respawnCount, (d, v) -> d.respawnCount = v, () -> CommandHandler.CommandType.PLAYER_RESPAWN);

        private final ToIntFunction<PlayerData> getter;
        private final BiConsumer<PlayerData, Integer> setter;
        private final Supplier<CommandHandler.CommandType> typeSupplier;

        PlayerDataType(ToIntFunction<PlayerData> getter,
                       BiConsumer<PlayerData, Integer> setter,
                       Supplier<CommandHandler.CommandType> typeSupplier)
        {
            this.getter = getter;
            this.setter = setter;
            this.typeSupplier = typeSupplier;
        }

        public int getInt(PlayerData data)
        {
            return this.getter.applyAsInt(data);
        }

        public int increment(PlayerData data)
        {
            int newValue = this.getter.applyAsInt(data) + 1;
            this.setter.accept(data, newValue);
            return newValue;
        }

        public void reset(PlayerData data)
        {
            this.setter.accept(data, 0);
        }

        public CommandHandler.CommandType getCommandType()
        {
            return this.typeSupplier.get();
        }
    }

    public enum PlayerDimensionDataType
    {
        ENTER,
        LEAVE;
    }
}
