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

public class DataTracker
{
    private static final DataTracker INSTANCE = new DataTracker();
    private File worldDir = new File(".");
    private final Map<Integer, Integer> dimensionLoadCounts = new HashMap<Integer, Integer>();
    private int serverStarts;
    private boolean dirty;

    public static DataTracker instance()
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
        Integer count = this.dimensionLoadCounts.get(dimension);

        if (count == null)
        {
            count = Integer.valueOf(1);
        }
        else
        {
            count = count + 1;
        }

        this.dimensionLoadCounts.put(dimension, count);

        this.dirty = true;
    }

    public int getDimensionLoadCount(int dimension)
    {
        Integer count = this.dimensionLoadCounts.get(dimension);
        return count != null ? count.intValue() : 0;
    }

    public int getServerStartCount()
    {
        return this.serverStarts;
    }

    private void readFromNBT(NBTTagCompound nbt)
    {
        if (nbt == null || nbt.hasKey("DimLoadCounts", Constants.NBT.TAG_LIST) == false)
        {
            return;
        }

        this.dimensionLoadCounts.clear();

        NBTTagList tagList = nbt.getTagList("DimLoadCounts", Constants.NBT.TAG_COMPOUND);
        int tagCount = tagList.tagCount();

        for (int i = 0; i < tagCount; i++)
        {
            NBTTagCompound tag = tagList.getCompoundTagAt(i);
            this.dimensionLoadCounts.put(tag.getInteger("Dim"), tag.getInteger("Count"));
        }

        this.serverStarts = nbt.getInteger("ServerStarts");
    }

    private NBTTagCompound writeToNBT(@Nonnull NBTTagCompound nbt)
    {
        NBTTagList tagList = new NBTTagList();

        for (Map.Entry<Integer, Integer> entry : this.dimensionLoadCounts.entrySet())
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
                    WorldPrimer.logger.warn("Failed to create a directory for storing the data tracker file '{}'", saveDir.getPath());
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
        this.dimensionLoadCounts.clear();
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
            WorldPrimer.logger.warn("Failed to read tracker data from file '{}'", file.getPath());
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
            WorldPrimer.logger.warn("Failed to write tracker data to file");
        }
    }
}
