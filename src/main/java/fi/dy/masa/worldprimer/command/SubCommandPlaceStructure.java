package fi.dy.masa.worldprimer.command;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Mirror;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.template.PlacementSettings;
import net.minecraft.world.gen.structure.template.Template;
import net.minecraft.world.gen.structure.template.TemplateManager;
import net.minecraftforge.fml.common.FMLCommonHandler;
import fi.dy.masa.worldprimer.WorldPrimer;
import fi.dy.masa.worldprimer.util.WorldUtils;

public class SubCommandPlaceStructure extends SubCommand
{
    private TemplateManager templateManager;

    public SubCommandPlaceStructure(CommandWorldPrimer baseCommand)
    {
        super(baseCommand);
    }

    @Override
    public String getName()
    {
        return "place-structure";
    }

    @Override
    public void printHelpGeneric(ICommandSender sender)
    {
        this.sendMessage(sender, this.getUsageStringCommon());
    }

    @Override
    protected String getUsageStringCommon()
    {
        return super.getUsageStringCommon() + " <x> <y> <z> <structurename> [rotation: cw_90 | cw_180 | ccw_90 | none] [mirror: left_right | front_back | none] [centered]";
    }

    @Override
    protected List<String> getTabCompletionsSub(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos)
    {
        // Structure filename
        if (args.length == 4)
        {
            return CommandBase.getListOfStringsMatchingLastWord(args, this.getExistingStructureFileNames());
        }
        // Rotation argument
        else if (args.length == 5)
        {
            return CommandBase.getListOfStringsMatchingLastWord(args, "cw_90", "cw_180", "ccw_90", "none");
        }
        // Mirror argument
        else if (args.length == 6)
        {
            return CommandBase.getListOfStringsMatchingLastWord(args, "front_back", "left_right", "none");
        }
        // Centered argument
        else if (args.length == 7)
        {
            return CommandBase.getListOfStringsMatchingLastWord(args, "centered");
        }
        // Position arguments
        else if (args.length <= 3)
        {
            return CommandBase.getTabCompletionCoordinate(args, 0, targetPos);
        }

        return Collections.emptyList();
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
    {
        if (args.length >= 4 && args.length <= 7)
        {
            try
            {
                BlockPos pos = CommandBase.parseBlockPos(sender, args, 0, false);
                Rotation rotation = args.length >= 5 ? this.getRotation(args[4]) : Rotation.NONE;
                Mirror mirror     = args.length >= 6 ? this.getMirror(args[5]) : Mirror.NONE;
                boolean centered = args.length == 7 && args[6].equals("centered");

                this.tryPlaceStructure(server, sender.getEntityWorld(), pos, rotation, mirror, centered, args[3]);
            }
            catch (NumberFormatException e)
            {
                throwCommand("worldprimer.commands.help.generic.usage", this.getUsageStringCommon());
            }
        }
        else
        {
            throwCommand("worldprimer.commands.help.generic.usage", this.getUsageStringCommon());
        }
    }

    private boolean tryPlaceStructure(MinecraftServer server, World world, BlockPos pos, Rotation rotation, Mirror mirror, boolean centered, String structureFile)
    {
        Template template = this.getTemplateManager().getTemplate(server, new ResourceLocation(structureFile));

        if (template != null)
        {
            PlacementSettings placement = new PlacementSettings();
            placement.setRotation(rotation);
            placement.setMirror(mirror);

            if (centered)
            {
                BlockPos size = Template.transformedBlockPos(placement, template.getSize());
                pos = pos.add(-(size.getX() / 2), 0, -(size.getZ() / 2));
            }

            this.loadChunks(world, pos, template.getSize());
            template.addBlocksToWorld(world, pos, placement);
            WorldUtils.unloadLoadedChunks(world);

            return true;
        }

        return false;
    }

    private TemplateManager getTemplateManager()
    {
        // Lazy load/create the TemplateManager, so that the MinecraftServer is actually running at this point
        if (this.templateManager == null)
        {
            this.templateManager = new TemplateManager(this.getStructureDirectory().toString(), FMLCommonHandler.instance().getDataFixer());
        }

        return this.templateManager;
    }

    private File getStructureDirectory()
    {
        return new File(new File(WorldPrimer.configDirPath), "structures");
    }

    private List<String> getExistingStructureFileNames()
    {
        File dir = this.getStructureDirectory();

        if (dir.isDirectory())
        {
            String[] names = dir.list();
            List<String> list = new ArrayList<>();

            for (String name : names)
            {
                list.add(name.endsWith(".nbt") ? name.substring(0, name.length() - 4) : name);
            }

            return list;
        }

        return Collections.emptyList();
    }

    private Rotation getRotation(String arg) throws CommandException
    {
        if (arg.equals("cw_90"))
        {
            return Rotation.CLOCKWISE_90;
        }
        else if (arg.equals("cw_180"))
        {
            return Rotation.CLOCKWISE_180;
        }
        else if (arg.equals("ccw_90"))
        {
            return Rotation.COUNTERCLOCKWISE_90;
        }
        else if (arg.equals("none"))
        {
            return Rotation.NONE;
        }

        throwCommand("worldprimer.commands.error.invalid.argument.rotation", arg);

        return Rotation.NONE; // dummy
    }

    private Mirror getMirror(String arg) throws CommandException
    {
        if (arg.equals("front_back"))
        {
            return Mirror.FRONT_BACK;
        }
        else if (arg.equals("left_right"))
        {
            return Mirror.LEFT_RIGHT;
        }
        else if (arg.equals("none"))
        {
            return Mirror.NONE;
        }

        throwCommand("worldprimer.commands.error.invalid.argument.mirror", arg);

        return Mirror.NONE; // dummy
    }

    private void loadChunks(World world, BlockPos pos, BlockPos size)
    {
        WorldUtils.loadChunks(world, pos.getX() >> 4, pos.getZ() >> 4,
                              (pos.getX() + size.getX()) >> 4, (pos.getZ() + size.getZ()) >> 4);
    }
}
