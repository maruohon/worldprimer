package fi.dy.masa.worldprimer.command.parser.token;

import javax.annotation.Nullable;
import fi.dy.masa.worldprimer.command.parser.value.Value;

public class Token
{
    protected final TokenType type;
    protected final String originalString;

    public Token(TokenType type, String originalString)
    {
        this.type = type;
        this.originalString = originalString;
    }

    public TokenType getType()
    {
        return this.type;
    }

    public String getOriginalString()
    {
        return this.originalString;
    }

    @Nullable
    public Value getValue()
    {
        return null;
    }

    @Override
    public String toString()
    {
        return String.format("Token{type=%s, originalString='%s'}",
                             this.type, this.originalString);
    }
}
