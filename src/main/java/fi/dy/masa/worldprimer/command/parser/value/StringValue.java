package fi.dy.masa.worldprimer.command.parser.value;

public class StringValue extends Value
{
    protected final String value;

    public StringValue(String value)
    {
        this.value = value;
    }

    @Override
    public boolean isString()
    {
        return true;
    }

    public String getValue()
    {
        return this.value;
    }

    @Override
    public String toString()
    {
        return this.value;
    }
}
