package fi.dy.masa.worldprimer.command;

import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import fi.dy.masa.worldprimer.WorldPrimer;
import fi.dy.masa.worldprimer.command.util.WorldPrimerCommandSender;

public class SubCommandTestExecutors extends SubCommand
{
    public SubCommandTestExecutors(CommandWorldPrimer baseCommand)
    {
        super(baseCommand);
    }

    @Override
    public String getName()
    {
        return "test-executors";
    }

    @Override
    public void printHelpGeneric(ICommandSender sender)
    {
        this.sendMessage(sender, this.getUsageStringCommon());
    }

    @Override
    protected List<String> getTabCompletionsSub(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos)
    {
        return Collections.emptyList();
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
    {
        String str = String.format("test-executors: sender = %s, sender entity = %s, args = '%s'",
                                   sender, sender.getCommandSenderEntity(), String.join(" ", args));

        WorldPrimer.LOGGER.info(str);

        if (sender != WorldPrimerCommandSender.INSTANCE)
        {
            this.sendMessage(sender, str);
        }
    }
}
