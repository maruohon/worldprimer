package fi.dy.masa.worldprimer.command.util;

import fi.dy.masa.worldprimer.command.handler.ParsedCommand;

public class ScheduledCommand implements Comparable<ScheduledCommand>
{
    private final ParsedCommand command;
    private final int dimension;
    private final boolean isPeriodic;
    private final long time;
    private final long offset;
    private long nextExecution;

    public ScheduledCommand(ParsedCommand command, int dimension, long time, long offset, boolean isPeriodic)
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
    public int compareTo(ScheduledCommand other)
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
        return String.format("ScheduledCommand{dimension=%d,time=%d,periodic=%s,nextExecution=%d,command=%s}",
                             this.dimension, this.time, this.isPeriodic, this.nextExecution, this.command);
    }
}
