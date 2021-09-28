package fi.dy.masa.worldprimer.command;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import org.apache.commons.io.IOUtils;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.NumberInvalidException;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.template.Template;
import fi.dy.masa.worldprimer.WorldPrimer;
import fi.dy.masa.worldprimer.config.Configs;
import fi.dy.masa.worldprimer.util.PositionUtils;
import fi.dy.masa.worldprimer.util.Schematic;
import fi.dy.masa.worldprimer.util.TemplateWorldPrimer;
import fi.dy.masa.worldprimer.util.WorldUtils;

public class SubCommandCreateStructure extends SubCommandPlaceStructure
{
    public SubCommandCreateStructure(CommandWorldPrimer baseCommand)
    {
        super(baseCommand);
    }

    @Override
    public String getName()
    {
        return "create-structure";
    }

    @Override
    public void printHelpGeneric(ICommandSender sender)
    {
        this.sendMessage(sender, this.getUsage());
    }

    private String getUsage()
    {
        return this.getUsageStringCommon() + " <x1> <y1> <z1> <x2> <y2> <z2> <vanilla | schematic> <structurename> [override]";
    }

    @Override
    protected List<String> getTabCompletionsSub(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos)
    {
        // Position arguments for the first corner
        if (args.length <= 3)
        {
            return CommandBase.getTabCompletionCoordinate(args, 0, targetPos);
        }
        // Position arguments for the second corner
        else if (args.length <= 6)
        {
            return CommandBase.getTabCompletionCoordinate(args, 3, targetPos);
        }
        // Structure type
        else if (args.length == 7)
        {
            return CommandBase.getListOfStringsMatchingLastWord(args, "vanilla", "schematic");
        }
        // Structure filename
        else if (args.length == 8)
        {
            return CommandBase.getListOfStringsMatchingLastWord(args, this.getExistingStructureFileNames());
        }
        // Override argument
        else if (args.length == 9)
        {
            return CommandBase.getListOfStringsMatchingLastWord(args, "override");
        }

        return Collections.emptyList();
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
    {
        if (args.length >= 8 && args.length <= 9)
        {
            if (args[6].equals("schematic") == false && args[6].equals("vanilla") == false)
            {
                throwUsage(this.getUsage());
            }

            StructureType type = args[6].equals("schematic") ? StructureType.SCHEMATIC : StructureType.STRUCTURE;
            boolean override = args.length == 9 && args[8].equals("override");
            String fileName = args[7];
            File file = new File(this.getStructureDirectory(), fileName + type.getExtension());

            if (override == false && file.exists())
            {
                throwCommand("worldprimer.commands.error.create_schematic.file_exists_no_override", file.getAbsolutePath());
            }

            try
            {
                BlockPos pos1 = CommandBase.parseBlockPos(sender, args, 0, false);
                BlockPos pos2 = CommandBase.parseBlockPos(sender, args, 3, false);
                BlockPos posStart = PositionUtils.getMinCorner(pos1, pos2);
                BlockPos posEnd = PositionUtils.getMaxCorner(pos1, pos2);
                BlockPos size = posEnd.subtract(posStart).add(1, 1, 1);
                World world = sender.getEntityWorld();
                boolean cbCrossWorld = Configs.enableChiselsAndBitsCrossWorldFormat;

                WorldUtils.loadBlocks(world, posStart.getX(), posStart.getZ(), posEnd.getX(), posEnd.getZ());

                if (cbCrossWorld)
                {
                    WorldPrimer.LOGGER.info("Using a cross-world compatible format for any Chisels & Bits blocks");
                }

                if (this.tryCreateSchematicWrapper(server, world, posStart, size, type, fileName, cbCrossWorld, sender))
                {
                    CommandBase.notifyCommandListener(sender, this.getBaseCommand(), "worldprimer.commands.info.create_schematic.success", fileName);
                }
                else
                {
                    throwCommand("worldprimer.commands.error.create_schematic.failed");
                }

                WorldUtils.unloadLoadedChunks(world);
            }
            catch (NumberInvalidException e)
            {
                throwUsage(this.getUsage());
            }
        }
        else
        {
            throwUsage(this.getUsage());
        }
    }

    private boolean tryCreateSchematicWrapper(MinecraftServer server, World world, BlockPos posStart, BlockPos size, StructureType type,
            String structureName, boolean cbCrossWorld, ICommandSender sender) throws CommandException
    {
        if (type == StructureType.STRUCTURE)
        {
            return this.tryCreateVanillaStructure(server, world, posStart, size, structureName, cbCrossWorld, sender);
        }
        else if (type == StructureType.SCHEMATIC)
        {
            return this.tryCreateSchematic(server, world, posStart, size, structureName, cbCrossWorld, sender);
        }
        else
        {
            return false;
        }
    }

    private boolean tryCreateVanillaStructure(MinecraftServer server, World world, BlockPos posStart, BlockPos size,
            String fileName, boolean cbCrossWorld, ICommandSender sender) throws CommandException
    {
        Template template = cbCrossWorld ? new TemplateWorldPrimer() : new Template();

        template.takeBlocksFromWorld(world, posStart, size, true, Blocks.STRUCTURE_VOID);
        template.setAuthor(sender.getName());

        return this.writeTemplateToFile(template, fileName);
    }

    private boolean tryCreateSchematic(MinecraftServer server, World world, BlockPos posStart, BlockPos size,
            String structureName, boolean cbCrossWorld, ICommandSender sender) throws CommandException
    {
        File dir = this.getStructureDirectory();

        if (dir.exists() == false && dir.mkdirs() == false)
        {
            return false;
        }

        File file = new File(dir, structureName + StructureType.SCHEMATIC.getExtension());
        Schematic schematic = Schematic.createFromWorld(world, posStart, size, cbCrossWorld);

        return schematic.writeToFile(file);
    }

    private boolean writeTemplateToFile(Template template, String structureName)
    {
        File dir = this.getStructureDirectory();

        if (dir.exists() == false && dir.mkdirs() == false)
        {
            WorldPrimer.LOGGER.warn("Failed to create structure directory '{}'", dir.getAbsolutePath());
            return false;
        }

        File file = new File(dir, structureName + StructureType.STRUCTURE.getExtension());
        OutputStream os = null;

        try
        {
            NBTTagCompound nbt = template.writeToNBT(new NBTTagCompound());
            os = new FileOutputStream(file);
            CompressedStreamTools.writeCompressed(nbt, os);
            return true;
        }
        catch (Throwable t)
        {
            WorldPrimer.LOGGER.warn("Failed to write structure data to file '{}'", file.getAbsolutePath(), t);
        }
        finally
        {
            IOUtils.closeQuietly(os);
        }

        return false;
    }
}
