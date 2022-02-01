package fi.dy.masa.worldprimer.command;

import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import fi.dy.masa.worldprimer.command.handler.CommandHandler;
import fi.dy.masa.worldprimer.command.handler.ParsedCommand;
import fi.dy.masa.worldprimer.command.util.CommandScheduler;

public class SubCommandDelay extends SubCommand
{
    public SubCommandDelay(CommandWorldPrimer baseCommand)
    {
        super(baseCommand);
    }

    @Override
    public String getName()
    {
        return "delay";
    }

    @Override
    public void printHelpGeneric(ICommandSender sender)
    {
        this.sendMessage(sender, this.getUsageStringCommon() + " <delay> <actual command and args>");
    }

    @Override
    protected List<String> getTabCompletionsSub(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos)
    {
        return Collections.emptyList();
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
    {
        if (args.length < 2)
        {
            throwUsage(this.getUsageStringCommon() + " <delay> <actual command and args>");
        }

        int delay = CommandWorldPrimer.parseInt(args[0]);
        args = dropFirstStrings(args, 1);
        String commandStr = String.join(" ", args);
        ParsedCommand command = CommandHandler.INSTANCE.buildCommand("Scheduled Command", commandStr);

        if (command != null)
        {
            World world = sender.getEntityWorld();

            if (world != null)
            {
                long currentTime = world.getTotalWorldTime();
                int dimension = world.provider.getDimension();

                CommandScheduler.INSTANCE.addScheduleCommand(command, currentTime + delay, 0, false, dimension);
                CommandScheduler.INSTANCE.updateAllScheduledCommands(false);
            }
            else
            {
                throwCommand("Failed to get world for delayed command '" + commandStr + "'");
            }
        }
        else
        {
            throwCommand("Failed to parse command from '" + commandStr + "'");
        }
    }
}
