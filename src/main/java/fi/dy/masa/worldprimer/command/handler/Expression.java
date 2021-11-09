package fi.dy.masa.worldprimer.command.handler;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.command.SyntaxErrorException;
import fi.dy.masa.worldprimer.command.parser.ExpressionParser;
import fi.dy.masa.worldprimer.command.parser.token.Token;
import fi.dy.masa.worldprimer.command.parser.token.TokenCategory;
import fi.dy.masa.worldprimer.command.parser.token.TokenType;
import fi.dy.masa.worldprimer.command.parser.value.BooleanValue;
import fi.dy.masa.worldprimer.command.substitution.CommandContext;

public class Expression
{
    protected final List<Token> tokens;
    protected final String originalString;

    public Expression(List<Token> tokens, String originalString)
    {
        this.tokens = tokens;
        this.originalString = originalString;
    }

    public String getOriginalString()
    {
        return this.originalString;
    }

    @Nullable
    protected Token reduceAndSubstitute(CommandContext ctx, ExpressionParser parser) throws SyntaxErrorException
    {
        List<Token> tokens = this.tokens;

        if (tokens.size() > 1)
        {
            tokens = parser.reduceRpn(tokens, ctx);
        }

        if (tokens.size() == 1)
        {
            return parser.getSubstitutedValueToken(tokens.get(0), ctx);
        }

        return null;
    }

    public boolean evaluateAsBoolean(CommandContext ctx, ExpressionParser parser) throws SyntaxErrorException
    {
        Token token = this.reduceAndSubstitute(ctx, parser);
        return token != null && token.getType() == TokenType.CONST_BOOLEAN &&
               ((BooleanValue) token.getValue()).getValue();
    }

    public String evaluateAsString(CommandContext ctx, ExpressionParser parser) throws SyntaxErrorException
    {
        Token token = this.reduceAndSubstitute(ctx, parser);

        if (token != null && token.getType().getTokenCategory() == TokenCategory.CONSTANT_VALUE)
        {
            return token.getValue().toString();
        }

        return this.originalString;
    }
}
