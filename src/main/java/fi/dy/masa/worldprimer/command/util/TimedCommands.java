package fi.dy.masa.worldprimer.command.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import fi.dy.masa.worldprimer.WorldPrimer;
import fi.dy.masa.worldprimer.command.handler.CommandHandler;
import fi.dy.masa.worldprimer.command.handler.ParsedCommand;
import fi.dy.masa.worldprimer.command.substitution.CommandContext;

public class TimedCommands
{
    private static final Map<Integer, List<TimedCommand>> TIMED_COMMANDS = new HashMap<>();
    private static long delayUntilNextCommand;

    public static class TimedCommand implements Comparable<TimedCommand>
    {
        private final ParsedCommand command;
        private final int dimension;
        private final boolean isPeriodic;
        private final long time;
        private final long offset;
        private long nextExecution;

        public TimedCommand(ParsedCommand command, int dimension, long time, long offset, boolean isPeriodic)
        {
            this.command = command;
            this.dimension = dimension;
            this.time = time;
            this.offset = offset;
            this.isPeriodic = isPeriodic;
        }

        public ParsedCommand getCommand()
        {
            return this.command;
        }

        public int getDimension()
        {
            return this.dimension;
        }

        public boolean getIsPeriodic()
        {
            return this.isPeriodic;
        }

        public long getNextExecution()
        {
            return this.nextExecution;
        }

        public void update(final long currentTime)
        {
            if (this.isPeriodic)
            {
                if (this.nextExecution <= currentTime)
                {
                    // The next time the command should run - basically the next time the modulo is 0, plus the offset
                    this.nextExecution = (((currentTime - this.offset) / this.time) + 1) * this.time + this.offset;
                }
            }
            else
            {
                this.nextExecution = this.time + this.offset;
            }
        }

        @Override
        public int compareTo(TimedCommand other)
        {
            if (this.nextExecution < other.nextExecution)
            {
                return -1;
            }
            else if (this.nextExecution > other.nextExecution)
            {
                return 1;
            }

            return 0;
        }

        @Override
        public String toString()
        {
            return String.format("TimedCommand{dimension=%d,time=%d,periodic=%s,nextExecution=%d,command=%s}",
                    this.dimension, this.time, this.isPeriodic, this.nextExecution, this.command);
        }
    }

    public static void setTimedCommands(String[] rawCommands)
    {
        TIMED_COMMANDS.clear();

        for (String cmdStr : rawCommands)
        {
            if (StringUtils.isBlank(cmdStr) || (cmdStr.length() > 0 && cmdStr.charAt(0) == '#'))
            {
                continue;
            }

            String[] parts = cmdStr.split("\\s+", 4);

            if (parts.length >= 4 && parts[0].equals("worldprimer-timed-command"))
            {
                boolean isPeriodic = false;
                String timeStr = parts[1];
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

                    final long time = Long.parseLong(timeStr);
                    final int dimension = Integer.parseInt(parts[2]);
                    List<TimedCommand> list = TIMED_COMMANDS.computeIfAbsent(dimension, (dim) -> new ArrayList<>());

                    cmdStr = String.join(" ", dropFirstStrings(parts, 3));
                    ParsedCommand command = CommandHandler.INSTANCE.buildCommand("Timed Command", cmdStr);

                    if (command != null)
                    {
                        list.add(new TimedCommand(command, dimension, time, offset, isPeriodic));
                    }
                }
                catch (NumberFormatException e)
                {
                    WorldPrimer.LOGGER.warn("Invalid time or dimension value in timed command '{}'", cmdStr);
                }
            }
            else
            {
                WorldPrimer.LOGGER.warn("Invalid timed command '{}', ignoring it!", cmdStr);
            }
        }

        updateAllTimedCommands(false);
    }

    public static void updateAllTimedCommands(boolean removeFromCurrentTick)
    {
        delayUntilNextCommand = -1;

        if (TIMED_COMMANDS.size() > 0)
        {
            TIMED_COMMANDS.entrySet().removeIf(entry -> updateTimedCommandsForDimension(entry.getKey(), removeFromCurrentTick));
        }
    }

    public static void updateTimedCommandsForDimension(int dimension)
    {
        if (updateTimedCommandsForDimension(dimension, false))
        {
            TIMED_COMMANDS.remove(dimension);
        }
    }

    private static boolean updateTimedCommandsForDimension(int dimension, boolean removeFromCurrentTick)
    {
        World world = DimensionManager.getWorld(dimension);
        List<TimedCommand> list = TIMED_COMMANDS.get(dimension);

        if (world != null && list != null)
        {
            final long currentTime = world.getTotalWorldTime();
            Iterator<TimedCommand> listIter = list.iterator();

            while (listIter.hasNext())
            {
                TimedCommand command = listIter.next();
                command.update(currentTime);
                //System.out.printf("updated command: %s @ %d\n", command, currentTime);

                if (command.getNextExecution() < currentTime || (removeFromCurrentTick && command.getNextExecution() == currentTime))
                {
                    //System.out.printf("removing command: %s @ %d\n", command, currentTime);
                    listIter.remove();
                }
            }

            if (list.size() > 0)
            {
                Collections.sort(list);
                long delay = list.get(0).getNextExecution() - currentTime;

                if (delayUntilNextCommand < 0 || delay < delayUntilNextCommand)
                {
                    delayUntilNextCommand = delay;
                }
            }
            else
            {
                //System.out.printf("removing list for dim %d @ %d\n", dimension, currentTime);
                return true;
            }
        }

        return false;
    }

    public static void runTimedCommands()
    {
        if (delayUntilNextCommand > 0 && --delayUntilNextCommand == 0)
        {
            for (Map.Entry<Integer, List<TimedCommand>> entry : TIMED_COMMANDS.entrySet())
            {
                int dimension = entry.getKey();
                World world = DimensionManager.getWorld(dimension);

                if (world != null)
                {
                    CommandContext ctx = new CommandContext(world, null, 0, dimension);
                    final long currentTime = world.getTotalWorldTime();
                    List<TimedCommand> list = entry.getValue();

                    for (TimedCommand command : list)
                    {
                        if (command.getNextExecution() == currentTime)
                        {
                            WorldPrimer.logInfo("Executing a timed command: tick: {}, dim: {}", currentTime, dimension);
                            CommandHandler.INSTANCE.executeCommand(command.getCommand(), ctx);
                        }
                        else if (command.getNextExecution() > currentTime)
                        {
                            break;
                        }
                    }
                }
            }

            updateAllTimedCommands(true);
        }
    }

    public static String[] dropFirstStrings(String[] input, int toDrop)
    {
        if (toDrop >= input.length)
        {
            return new String[0];
        }

        String[] arr = new String[input.length - toDrop];
        System.arraycopy(input, toDrop, arr, 0, input.length - toDrop);
        return arr;
    }
}
