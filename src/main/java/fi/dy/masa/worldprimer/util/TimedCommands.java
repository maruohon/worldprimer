package fi.dy.masa.worldprimer.util;

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
import fi.dy.masa.worldprimer.command.WorldPrimerCommandSender;

public class TimedCommands
{
    private static final Map<Integer, List<TimedCommand>> TIMED_COMMANDS = new HashMap<>();
    private static long delayUntilNextCommand;

    public static class TimedCommand implements Comparable<TimedCommand>
    {
        private final String command;
        private final int dimension;
        private final boolean isPeriodic;
        private final long time;
        private final long offset;
        private long nextExecution;

        public TimedCommand(String command, int dimension, long time, long offset, boolean isPeriodic)
        {
            this.command = command;
            this.dimension = dimension;
            this.time = time;
            this.offset = offset;
            this.isPeriodic = isPeriodic;
        }

        public String getCommand()
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
                    // The next time the command should run - basically the next time the module is 0, plus the offset
                    this.nextExecution = (((currentTime - this.offset) / this.time) + 1) * this.time + this.offset;
                }
            }
            else
            {
                this.nextExecution = this.time;
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

        for (String cmd : rawCommands)
        {
            if (StringUtils.isBlank(cmd) || (cmd.length() > 0 && cmd.charAt(0) == '#'))
            {
                continue;
            }

            String[] parts = cmd.split("\\s+", 4);

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
                        timeStr = timeStr.substring(1, timeStr.length());
                        int index = timeStr.indexOf('-');

                        if (index == -1)
                        {
                            index = timeStr.indexOf('+');
                        }

                        // Offset value present (<time><+|-><offset>), ie. in the format 54321-15 
                        if (index != -1)
                        {
                            offset = Long.parseLong(timeStr.substring(index, timeStr.length()));
                            timeStr = timeStr.substring(0, index);
                        }
                    }

                    final long time = Long.parseLong(timeStr);
                    final int dimension = Integer.parseInt(parts[2]);
                    List<TimedCommand> list = TIMED_COMMANDS.get(dimension);

                    if (list == null)
                    {
                        list = new ArrayList<TimedCommand>();
                        TIMED_COMMANDS.put(dimension, list);
                    }

                    String command = String.join(" ", CommandUtils.dropFirstStrings(parts, 3));
                    list.add(new TimedCommand(command, dimension, time, offset, isPeriodic));
                }
                catch (NumberFormatException e)
                {
                    WorldPrimer.logger.warn("Invalid time or dimension value in timed command '{}'", cmd);
                }
            }
            else
            {
                WorldPrimer.logger.warn("Invalid timed command '{}', ignoring it!", cmd);
            }
        }

        updateAllTimedCommands(false);
    }

    public static void updateAllTimedCommands(boolean removeFromCurrentTick)
    {
        delayUntilNextCommand = -1;

        if (TIMED_COMMANDS.size() > 0)
        {
            Iterator<Map.Entry<Integer, List<TimedCommand>>> mapIter = TIMED_COMMANDS.entrySet().iterator();

            while (mapIter.hasNext())
            {
                Map.Entry<Integer, List<TimedCommand>> entry = mapIter.next();

                if (updateTimedCommandsForDimension(entry.getKey(), removeFromCurrentTick))
                {
                    mapIter.remove();
                }
            }
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
                World world = DimensionManager.getWorld(entry.getKey());

                if (world != null)
                {
                    final long currentTime = world.getTotalWorldTime();
                    List<TimedCommand> list = entry.getValue();

                    for (TimedCommand command : list)
                    {
                        if (command.getNextExecution() == currentTime)
                        {
                            WorldPrimer.logInfo("Executing a timed command '{}' @ tick {} in dim {}", command.getCommand(), currentTime, entry.getKey());
                            WorldPrimerCommandSender.instance().runCommands(null, world, command.getCommand());
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
}
