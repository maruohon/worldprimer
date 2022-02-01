package fi.dy.masa.worldprimer.command.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import fi.dy.masa.worldprimer.WorldPrimer;
import fi.dy.masa.worldprimer.command.CommandWorldPrimer;
import fi.dy.masa.worldprimer.command.handler.CommandHandler;
import fi.dy.masa.worldprimer.command.handler.ParsedCommand;
import fi.dy.masa.worldprimer.command.substitution.CommandContext;

public class CommandScheduler
{
    public static final CommandScheduler INSTANCE = new CommandScheduler();

    private final Map<Integer, List<ScheduledCommand>> scheduledCommands = new HashMap<>();
    private long delayUntilNextCommand;

    public void setScheduledCommands(String[] rawCommands)
    {
        this.scheduledCommands.clear();

        for (String cmdStr : rawCommands)
        {
            if (StringUtils.isBlank(cmdStr) || (cmdStr.length() > 0 && cmdStr.charAt(0) == '#'))
            {
                continue;
            }

            String[] parts = cmdStr.split("\\s+", 4);

            if (parts.length >= 4 && parts[0].equals("worldprimer-scheduled-command"))
            {
                String timeStr = parts[1];
                boolean isPeriodic = false;
                long offset = 0;

                try
                {
                    // Periodic command
                    if (timeStr.length() > 1 && timeStr.charAt(0) == '%')
                    {
                        isPeriodic = true;
                        timeStr = timeStr.substring(1);
                    }

                    int index = timeStr.indexOf('-');

                    if (index == -1)
                    {
                        index = timeStr.indexOf('+');
                    }

                    // Offset value present (<time><+|-><offset>), ie. in the format 54321-15
                    if (index != -1)
                    {
                        offset = Long.parseLong(timeStr.substring(index));
                        timeStr = timeStr.substring(0, index);
                    }

                    cmdStr = String.join(" ", CommandWorldPrimer.dropFirstStrings(parts, 3));
                    ParsedCommand command = CommandHandler.INSTANCE.buildCommand("Scheduled Command", cmdStr);

                    if (command != null)
                    {
                        final long time = Long.parseLong(timeStr);
                        final int dimension = Integer.parseInt(parts[2]);
                        List<ScheduledCommand> list = this.scheduledCommands.computeIfAbsent(dimension, (dim) -> new ArrayList<>());

                        list.add(new ScheduledCommand(command, dimension, time, offset, isPeriodic));
                    }
                }
                catch (NumberFormatException e)
                {
                    WorldPrimer.LOGGER.warn("Invalid time or dimension value in scheduled command '{}'", cmdStr);
                }
            }
            else
            {
                WorldPrimer.LOGGER.warn("Invalid scheduled command '{}', ignoring...", cmdStr);
            }
        }

        this.updateAllScheduledCommands(false);
    }

    public void updateAllScheduledCommands(boolean removeFromCurrentTick)
    {
        this.delayUntilNextCommand = -1;

        if (this.scheduledCommands.size() > 0)
        {
            this.scheduledCommands.entrySet().removeIf(entry -> this.updateScheduledCommandsForDimension(entry.getKey(), removeFromCurrentTick));
        }
    }

    public void updateScheduledCommandsForDimension(int dimension)
    {
        if (this.updateScheduledCommandsForDimension(dimension, false))
        {
            this.scheduledCommands.remove(dimension);
        }
    }

    private boolean updateScheduledCommandsForDimension(int dimension, boolean removeFromCurrentTick)
    {
        World world = DimensionManager.getWorld(dimension);
        List<ScheduledCommand> list = this.scheduledCommands.get(dimension);

        if (world != null && list != null)
        {
            final long currentTime = world.getTotalWorldTime();
            Iterator<ScheduledCommand> iterator = list.iterator();

            while (iterator.hasNext())
            {
                ScheduledCommand command = iterator.next();
                command.update(currentTime);
                //System.out.printf("updated command: %s @ %d\n", command, currentTime);

                if (command.getNextExecution() < currentTime || (removeFromCurrentTick && command.getNextExecution() == currentTime))
                {
                    //System.out.printf("removing command: %s @ %d\n", command, currentTime);
                    iterator.remove();
                }
            }

            if (list.isEmpty())
            {
                //System.out.printf("removing list for dim %d @ %d\n", dimension, currentTime);
                return true;
            }

            list.sort(ScheduledCommand::compareTo);
            long delay = list.get(0).getNextExecution() - currentTime;

            if (this.delayUntilNextCommand < 0 || delay < this.delayUntilNextCommand)
            {
                this.delayUntilNextCommand = delay;
            }
        }

        return false;
    }

    public void runScheduledCommands()
    {
        if (this.delayUntilNextCommand > 0 && --this.delayUntilNextCommand == 0)
        {
            for (Map.Entry<Integer, List<ScheduledCommand>> entry : this.scheduledCommands.entrySet())
            {
                int dimension = entry.getKey();
                World world = DimensionManager.getWorld(dimension);

                if (world != null)
                {
                    this.runScheduledCommandsFromList(entry.getValue(), world, dimension);
                }
            }

            this.updateAllScheduledCommands(true);
        }
    }

    protected void runScheduledCommandsFromList(List<ScheduledCommand> commands, World world, int dimension)
    {
        CommandContext ctx = new CommandContext(world, null, 0, dimension);
        final long currentTime = world.getTotalWorldTime();

        for (ScheduledCommand command : commands)
        {
            if (command.getNextExecution() <= currentTime)
            {
                WorldPrimer.logInfo("Executing a scheduled command: tick: {}, dim: {}", currentTime, dimension);
                CommandHandler.INSTANCE.executeCommand(command.getCommand(), ctx);
            }
            else if (command.getNextExecution() > currentTime)
            {
                break;
            }
        }
    }
}
