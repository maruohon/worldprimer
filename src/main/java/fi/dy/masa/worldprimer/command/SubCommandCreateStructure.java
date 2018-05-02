package fi.dy.masa.worldprimer.command;

import java.io.File;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.NumberInvalidException;
import net.minecraft.init.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.template.Template;
import net.minecraft.world.gen.structure.template.TemplateManager;
import fi.dy.masa.worldprimer.util.PositionUtils;
import fi.dy.masa.worldprimer.util.Schematic;
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
        this.sendMessage(sender, this.getUsageStringCommon());
    }

    @Override
    protected String getUsageStringCommon()
    {
        return "/" + this.getBaseCommand().getName() + " " + this.getName() + " <x1> <y1> <z1> <x2> <y2> <z2> <vanilla | schematic> <structurename> [override]";
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
                throwCommand("worldprimer.commands.help.generic.usage", this.getUsageStringCommon());
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

                WorldUtils.loadBlocks(world, posStart.getX(), posStart.getZ(), posEnd.getX(), posEnd.getZ());

                if (this.tryCreateSchematicWrapper(server, world, posStart, size, type, fileName, sender))
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
                throwCommand("worldprimer.commands.help.generic.usage", this.getUsageStringCommon());
            }
        }
        else
        {
            throwCommand("worldprimer.commands.help.generic.usage", this.getUsageStringCommon());
        }
    }

    private boolean tryCreateSchematicWrapper(MinecraftServer server, World world, BlockPos posStart, BlockPos size, StructureType type,
            String structureName, ICommandSender sender) throws CommandException
    {
        if (type == StructureType.STRUCTURE)
        {
            return this.tryCreateVanillaStructure(server, world, posStart, size, structureName, sender);
        }
        else if (type == StructureType.SCHEMATIC)
        {
            return this.tryCreateSchematic(server, world, posStart, size, structureName, sender);
        }
        else
        {
            return false;
        }
    }

    private boolean tryCreateVanillaStructure(MinecraftServer server, World world, BlockPos posStart, BlockPos size,
            String fileName, ICommandSender sender) throws CommandException
    {
        ResourceLocation id = new ResourceLocation(fileName);
        TemplateManager manager = this.getTemplateManager();
        Template template = manager.getTemplate(server, id);

        if (template != null)
        {
            template.takeBlocksFromWorld(world, posStart, size, true, Blocks.STRUCTURE_VOID);
            template.setAuthor(sender.getName());
            manager.writeTemplate(server, id);

            return true;
        }

        return false;
    }

    private boolean tryCreateSchematic(MinecraftServer server, World world, BlockPos posStart, BlockPos size,
            String structureName, ICommandSender sender) throws CommandException
    {
        File file = new File(this.getStructureDirectory(), structureName + StructureType.SCHEMATIC.getExtension());
        Schematic schematic = Schematic.createFromWorld(world, posStart, size);
        return schematic.writeToFile(file);
    }
}
