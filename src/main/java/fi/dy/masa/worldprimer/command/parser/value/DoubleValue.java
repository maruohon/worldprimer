package fi.dy.masa.worldprimer.command.parser.value;

public class DoubleValue extends Value
{
    protected final double value;

    public DoubleValue(double value)
    {
        this.value = value;
    }

    @Override
    public boolean isNumber()
    {
        return true;
    }

    @Override
    public boolean isFloatingPoint()
    {
        return true;
    }

    public double getValue()
    {
        return this.value;
    }

    @Override
    public String toString()
    {
        return String.valueOf(this.value);
    }
}
