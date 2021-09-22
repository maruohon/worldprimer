package fi.dy.masa.worldprimer.command.parser.value;

public abstract class Value
{
    public boolean isBoolean()
    {
        return false;
    }

    public boolean isNumber()
    {
        return false;
    }

    public boolean isFloatingPoint()
    {
        return false;
    }

    public boolean isString()
    {
        return false;
    }
}
