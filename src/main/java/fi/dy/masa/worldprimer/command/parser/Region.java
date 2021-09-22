package fi.dy.masa.worldprimer.command.parser;

public class Region
{
    public final int start;
    public final int end;

    public Region(int start, int end)
    {
        this.start = start;
        this.end = end;
    }

    @Override
    public String toString()
    {
        return String.format("Region:{start:%d,end:%d}", this.start, this.end);
    }
}
