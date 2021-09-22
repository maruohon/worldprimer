package fi.dy.masa.worldprimer.command.parser.value;

public class IntValue extends Value
{
    protected final int value;

    public IntValue(int value)
    {
        this.value = value;
    }

    @Override
    public boolean isNumber()
    {
        return true;
    }

    public int getValue()
    {
        return this.value;
    }

    @Override
    public String toString()
    {
        return String.valueOf(this.value);
    }
}
