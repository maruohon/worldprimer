package fi.dy.masa.worldprimer.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;
import fi.dy.masa.worldprimer.WorldPrimer;
import fi.dy.masa.worldprimer.config.Configs;
import fi.dy.masa.worldprimer.reference.Reference;

public class DimensionLoadTracker
{
    private static final DimensionLoadTracker INSTANCE = new DimensionLoadTracker();
    private File worldDir = new File(".");
    private final Map<Integer, Integer> loadCounts = new HashMap<Integer, Integer>();
    private int serverStarts;
    private boolean dirty;

    public static DimensionLoadTracker instance()
    {
        return INSTANCE;
    }

    public void serverStarted()
    {
        this.serverStarts++;
        this.dirty = true;
    }

    public void dimensionLoaded(int dimension)
    {
        Integer count = this.loadCounts.get(dimension);

        if (count == null)
        {
            count = Integer.valueOf(1);
        }
        else
        {
            count = count + 1;
        }

        this.loadCounts.put(dimension, count);

        this.dirty = true;
    }

    public int getLoadCountFor(int dimension)
    {
        Integer count = this.loadCounts.get(dimension);
        return count != null ? count.intValue() : 0;
    }

    private void readFromNBT(NBTTagCompound nbt)
    {
        if (nbt == null || nbt.hasKey("DimLoadCounts", Constants.NBT.TAG_LIST) == false)
        {
            return;
        }

        this.loadCounts.clear();

        NBTTagList tagList = nbt.getTagList("DimLoadCounts", Constants.NBT.TAG_COMPOUND);
        int tagCount = tagList.tagCount();

        for (int i = 0; i < tagCount; i++)
        {
            NBTTagCompound tag = tagList.getCompoundTagAt(i);
            this.loadCounts.put(tag.getInteger("Dim"), tag.getInteger("Count"));
        }

        this.serverStarts = nbt.getInteger("ServerStarts");
    }

    private NBTTagCompound writeToNBT(@Nonnull NBTTagCompound nbt)
    {
        NBTTagList tagList = new NBTTagList();

        for (Map.Entry<Integer, Integer> entry : this.loadCounts.entrySet())
        {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setInteger("Dim", entry.getKey());
            tag.setInteger("Count", entry.getValue());
            tagList.appendTag(tag);
        }

        nbt.setTag("DimLoadCounts", tagList);
        nbt.setInteger("ServerStarts", this.serverStarts);

        return nbt;
    }

    @Nullable
    private File getDataDir(boolean createDirs)
    {
        File saveDir = this.worldDir;

        if (saveDir != null)
        {
            saveDir = new File(saveDir, Reference.MOD_ID);

            if (saveDir.exists() == false && (createDirs == false || saveDir.mkdirs() == false))
            {
                if (createDirs)
                {
                    WorldPrimer.logger.warn("Failed to create a directory for storing the dimension load counts file '{}'", saveDir.getPath());
                }

                return null;
            }

            return saveDir;
        }

        return null;
    }

    public void readFromDisk(File worldDir)
    {
        if (Configs.enableDimensionLoadTracking == false)
        {
            return;
        }

        // Clear old data regardless of whether there is a data file
        this.loadCounts.clear();
        this.serverStarts = 0;

        this.worldDir = worldDir;
        File file = new File(new File(worldDir, Reference.MOD_ID), "dim_loads.nbt");

        try
        {
            if (file != null && file.exists() && file.isFile())
            {
                this.readFromNBT(CompressedStreamTools.readCompressed(new FileInputStream(file)));
            }
        }
        catch (Exception e)
        {
            WorldPrimer.logger.warn("Failed to read dimension load counts from file '{}'", file.getPath());
        }
    }

    public void writeToDisk()
    {
        if (this.dirty == false || Configs.enableDimensionLoadTracking == false)
        {
            return;
        }

        try
        {
            File saveDir = this.getDataDir(true);

            if (saveDir != null)
            {
                File fileTmp  = new File(saveDir, "dim_loads.nbt.tmp");
                File fileReal = new File(saveDir, "dim_loads.nbt");
                CompressedStreamTools.writeCompressed(this.writeToNBT(new NBTTagCompound()), new FileOutputStream(fileTmp));

                if (fileReal.exists())
                {
                    fileReal.delete();
                }

                fileTmp.renameTo(fileReal);
                this.dirty = false;
            }
        }
        catch (Exception e)
        {
            WorldPrimer.logger.warn("Failed to write dimension load counts to file");
        }
    }
}
