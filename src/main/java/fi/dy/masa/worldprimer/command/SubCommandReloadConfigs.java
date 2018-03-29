package fi.dy.masa.worldprimer.command;

import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import fi.dy.masa.worldprimer.config.Configs;

public class SubCommandReloadConfigs extends SubCommand
{
    public SubCommandReloadConfigs(CommandWorldPrimer baseCommand)
    {
        super(baseCommand);
    }

    @Override
    public String getName()
    {
        return "reload-configs";
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
        if (args.length == 0)
        {
            Configs.loadConfigsFromFile();
            sendMessage(sender, "worldprimer.commands.info.configs.realoded");
        }
        else
        {
            throwCommand("worldprimer.commands.help.generic.usage", this.getUsageStringCommon());
        }
    }
}
