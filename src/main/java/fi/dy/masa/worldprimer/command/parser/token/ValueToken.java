package fi.dy.masa.worldprimer.command.parser.token;

import javax.annotation.Nullable;
import fi.dy.masa.worldprimer.command.parser.value.Value;

public class ValueToken extends Token
{
    protected final Value value;

    public ValueToken(TokenType type, String originalString, @Nullable Value value)
    {
        super(type, originalString);

        this.value = value;
    }

    @Override
    public Value getValue()
    {
        return this.value;
    }

    @Override
    public String toString()
    {
        return String.format("ValueToken{type=%s, originalString='%s', value=%s}",
                             this.type, this.originalString, this.value.toString());
    }
}
