package fi.dy.masa.worldprimer.command.parser.value;

public class BooleanValue extends Value
{
    protected final boolean value;

    public BooleanValue(boolean value)
    {
        this.value = value;
    }

    @Override
    public boolean isBoolean()
    {
        return true;
    }

    public boolean getValue()
    {
        return this.value;
    }

    @Override
    public String toString()
    {
        return String.valueOf(this.value);
    }
}
