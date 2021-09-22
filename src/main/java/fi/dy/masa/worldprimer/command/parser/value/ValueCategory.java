package fi.dy.masa.worldprimer.command.parser.value;

public enum ValueCategory
{
    BOOLEAN,
    NUMBER,
    STRING,
    ANY_VALUE,
    UNKNOWN,
    NONE;

    public boolean isOperatorCompatibleWithArguments(ValueCategory arg1, ValueCategory arg2)
    {
        if (arg1 == arg2)
        {
            if (this == arg1)
            {
                return true;
            }
            else if (this == ANY_VALUE)
            {
                return arg1 == NUMBER || arg1 == STRING || arg1 == BOOLEAN || arg1 == UNKNOWN;
            }
        }

        return false;
    }
}
