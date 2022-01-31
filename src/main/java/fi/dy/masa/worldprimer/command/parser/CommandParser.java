package fi.dy.masa.worldprimer.command.parser;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.apache.commons.lang3.tuple.Pair;
import com.google.common.collect.ImmutableList;
import net.minecraft.command.SyntaxErrorException;
import fi.dy.masa.worldprimer.WorldPrimer;
import fi.dy.masa.worldprimer.command.handler.ConditionalCommand;
import fi.dy.masa.worldprimer.command.handler.Expression;
import fi.dy.masa.worldprimer.command.handler.ParsedCommand;
import fi.dy.masa.worldprimer.command.parser.token.Token;
import fi.dy.masa.worldprimer.command.substitution.ArithmeticExpression;
import fi.dy.masa.worldprimer.command.substitution.BaseSubstitution;
import fi.dy.masa.worldprimer.command.substitution.CommandContext;
import fi.dy.masa.worldprimer.command.substitution.PlainString;
import fi.dy.masa.worldprimer.command.substitution.StringSubstitution;
import fi.dy.masa.worldprimer.util.WorldUtils;

public class CommandParser
{
    public static final CommandParser INSTANCE = new CommandParser();

    protected final ImmutableList<CommandPrefix> commandPrefixes;

    protected CommandParser()
    {
        ImmutableList.Builder<CommandPrefix> builder = ImmutableList.builder();

        addPrefixes(builder, this::csn,  "wp-csn",  "worldprimer-command-sender-name");
        addPrefixes(builder, this::eid,  "wp-eid",  "worldprimer-execute-in-dimension");
        addPrefixes(builder, this::eap,  "wp-eap",  "worldprimer-execute-as-player");
        addPrefixes(builder, this::eupe, "wp-eupe", "worldprimer-execute-using-player-entity");

        this.commandPrefixes = builder.build();
    }

    @Nullable
    public ParsedCommand parseCommand(String originalString,
                                      SubstitutionParser substitutionParser,
                                      ExpressionParser expressionParser) throws SyntaxErrorException
    {
        StringReader reader = new StringReader(originalString);
        Expression conditionExpression = this.getConditionExpression(reader, substitutionParser, expressionParser);
        ParsedCommand.Builder commandBuilder = this.getCommandPrefixes(reader);
        ParsedCommand baseCommand = this.getCommand(reader, substitutionParser, expressionParser, commandBuilder);

        //System.out.printf("CommandParser.parseCommand(): baseCommand = '%s', conditionExpression = '%s'\n", baseCommand != null ? baseCommand.getOriginalString() : "<null>", conditionExpression != null ? conditionExpression.getOriginalString() : "<null>");
        if (baseCommand != null && conditionExpression != null)
        {
            return new ConditionalCommand(baseCommand, conditionExpression, commandBuilder);
        }

        return baseCommand;
    }

    @Nullable
    protected Expression getConditionExpression(StringReader reader,
                                                SubstitutionParser substitutionParser,
                                                ExpressionParser expressionParser) throws SyntaxErrorException
    {
        String prefix = "worldprimer-condition[";
        String subString = reader.subString();
        int startIndex = prefix.length();

        if (subString.startsWith(prefix) && subString.length() > startIndex + 2)
        {
            int endIndex = subString.indexOf(']', startIndex + 1);

            if (endIndex != -1)
            {
                String conditionString = subString.substring(startIndex, endIndex);
                StringTokenizer tokenizer = new StringTokenizer(substitutionParser, new StringReader(conditionString));
                List<Token> tokens = expressionParser.parseAndReduceToRpn(tokenizer);

                if (tokens.isEmpty() == false)
                {
                    reader.movePos(endIndex + 1);
                    reader.skipNextSpaces();
                    return new Expression(tokens, conditionString);
                }
            }
        }

        return null;
    }

    @Nullable
    protected ParsedCommand getCommand(StringReader reader,
                                       SubstitutionParser substitutionParser,
                                       ExpressionParser expressionParser,
                                       ParsedCommand.Builder commandBuilder)
    {
        ImmutableList.Builder<StringSubstitution> builder = ImmutableList.builder();
        final int startPos = reader.getPos();
        int unconsumedStartPos = startPos;

        while (reader.canRead())
        {
            Region substitutionRegion = substitutionParser.getNextSubstitutionRegion(reader);

            // No more substitutions, add the rest of the command string as-is
            if (substitutionRegion == null)
            {
                if (unconsumedStartPos < reader.getLength())
                {
                    builder.add(this.plainString(reader, unconsumedStartPos));
                }

                break;
            }

            Pair<Region, Expression> pair = this.getArithmeticExpression(reader, substitutionRegion,
                                                                         unconsumedStartPos, expressionParser);

            // Found an arithmetic expression encompassing the next substitution
            if (pair != null)
            {
                //System.out.printf("CommandParser.getCommand(): Arithmetic Region: [%d, %d], expression: '%s'\n", pair.getLeft().start, pair.getLeft().end, pair.getRight().getOriginalString());
                StringSubstitution substitution = new ArithmeticExpression(pair.getRight(), expressionParser);
                unconsumedStartPos = this.addAndAdvance(pair.getLeft(), substitution, builder, reader, unconsumedStartPos);
            }
            // Just a plain substitution
            else
            {
                BaseSubstitution substitution = substitutionParser.getSubstitutionForFullRegion(reader, substitutionRegion, true);

                if (substitution != null)
                {
                    //System.out.printf("CommandParser.getCommand(): Substitution: [%d, %d], sub: '%s'\n", substitutionRegion.start, substitutionRegion.end, substitution.getOriginalFullSubstitutionString());
                    unconsumedStartPos = this.addAndAdvance(substitutionRegion, substitution, builder, reader, unconsumedStartPos);
                }
                else
                {
                    reader.skip();
                }
            }
        }

        ImmutableList<StringSubstitution> parts = builder.build();

        if (parts.isEmpty() == false)
        {
            return new ParsedCommand(parts, reader.subString(startPos), commandBuilder);
        }

        return null;
    }

    protected int addAndAdvance(Region region, StringSubstitution substitution,
                                ImmutableList.Builder<StringSubstitution> builder,
                                StringReader reader, int unconsumedStartPos)
    {
        // Add the part of the string before the substitution or arithmetic expression region starts
        if (region.start > unconsumedStartPos)
        {
            builder.add(this.plainString(reader, unconsumedStartPos, region.start - 1));
        }

        builder.add(substitution);
        unconsumedStartPos = region.end + 1;
        reader.setPos(unconsumedStartPos);

        return unconsumedStartPos;
    }

    protected PlainString plainString(StringReader reader, int startPos)
    {
        return this.plainString(reader, startPos, reader.getLength() - 1);
    }

    protected PlainString plainString(StringReader reader, int startPos, int endPos)
    {
        String str = reader.subString(startPos, endPos);
        str = str.replaceAll("\\$\\\\\\(", "\\$("); // $\( => $(
        str = str.replaceAll("\\$\\\\\\\\\\(", "\\$\\\\("); // $\\( => $\(
        str = str.replaceAll("\\\\\\{", "{"); // \{ => {
        return new PlainString(str);
    }

    protected ParsedCommand.Builder getCommandPrefixes(StringReader reader)
    {
        ParsedCommand.Builder builder = ParsedCommand.builder();

        while (reader.canRead())
        {
            if (this.getFirstPrefixAndAdvance(reader, builder) == false)
            {
                break;
            }
        }

        return builder;
    }

    protected boolean getFirstPrefixAndAdvance(StringReader reader, ParsedCommand.Builder builder)
    {
        for (CommandPrefix prefix : this.commandPrefixes)
        {
            if (reader.startsWith(prefix.name))
            {
                int prefixLength = prefix.name.length();

                if (prefix.hasArgument == false)
                {
                    prefix.nonParameterizedBuilderFunction.accept(builder);
                    reader.movePos(prefixLength);
                }
                else
                {
                    String arg = readBracketedArgument(reader, reader.getPos() + prefixLength);
                    //System.out.printf("getFirstPrefixAndAdvance(): arg: %s\n", arg);

                    if (arg == null)
                    {
                        WorldPrimer.LOGGER.warn("Invalid command prefix argument for prefix '{}' in command '{}'",
                                                prefix.name, reader.getString());
                        break;
                    }

                    prefix.parameterizedBuilderFunction.accept(builder, arg);
                    reader.movePos(prefixLength + arg.length() + 2);
                }

                reader.skipNextSpaces();
                return true;
            }
        }

        return false;
    }

    @Nullable
    protected static String readBracketedArgument(StringReader reader, int startPos)
    {
        if (reader.peekAt(startPos) == '[')
        {
            int pos = startPos + 1;
            int nesting = 1;

            while (reader.canPeekAt(pos))
            {
                char c = reader.peekAt(pos);

                if (c == ']' && --nesting == 0)
                {
                    if (pos > startPos + 1)
                    {
                        return reader.subString(startPos + 1, pos - 1);
                    }
                    else
                    {
                        return null;
                    }
                }
                else if (c == '[')
                {
                    ++nesting;
                }

                ++pos;
            }
        }

        return null;
    }

    @Nullable
    protected Pair<Region, Expression> getArithmeticExpression(StringReader reader, Region substitutionRegion,
                                                               int minimumPos, ExpressionParser expressionParser)
    {
        final int regStartPos = reader.getString().indexOf("$(", minimumPos);

        // The next arithmetic expression starts after the next/currently parsed substitution
        if (regStartPos > substitutionRegion.start)
        {
            return null;
        }

        int pos = regStartPos + 2;
        int nesting = 1;

        while (reader.canPeekAt(pos))
        {
            char c = reader.peekAt(pos);

            if (c == ')' && --nesting <= 0)
            {
                break;
            }
            else if (c == '(')
            {
                ++nesting;
            }

            ++pos;
        }

        // Found a matching closing parenthesis
        if (nesting == 0)
        {
            final int regEndPos = pos;
            StringReader subReader = reader.subReader(regStartPos + 2, regEndPos - 1);

            try
            {
                Expression expression = expressionParser.parseAndReduceToExpression(subReader);

                if (expression != null)
                {
                    return Pair.of(new Region(regStartPos, regEndPos), expression);
                }
            }
            catch (Exception e)
            {
                WorldPrimer.logInfo("CommandParser.getArithmeticExpression(): Failed to parse expression '{}' " +
                                    "(this may not be an error, depending on your command string structure)",
                                    subReader.getString());
            }
        }

        return null;
    }

    protected void csn(ParsedCommand.Builder builder, String arg)
    {
        builder.withSenderNameFunction((ctx) -> arg);
    }

    protected void eid(ParsedCommand.Builder builder, String arg)
    {
        builder.withExecutionWorldFunction((ctx) -> WorldUtils.getWorldFromDimensionName(ctx, arg));
    }

    protected void eap(ParsedCommand.Builder builder)
    {
        builder.withSenderFunction(CommandContext::getPlayer);
    }

    protected void eupe(ParsedCommand.Builder builder)
    {
        builder.withSenderEntityFunction((CommandContext::getPlayer));
    }

    protected static class CommandPrefix
    {
        public final String name;
        public final boolean hasArgument;
        @Nullable public final BiConsumer<ParsedCommand.Builder, String> parameterizedBuilderFunction;
        @Nullable public final Consumer<ParsedCommand.Builder> nonParameterizedBuilderFunction;

        public CommandPrefix(String name, @Nullable BiConsumer<ParsedCommand.Builder, String> parameterizedBuilderFunction)
        {
            this.name = name;
            this.hasArgument = true;
            this.parameterizedBuilderFunction = parameterizedBuilderFunction;
            this.nonParameterizedBuilderFunction = null;
        }

        public CommandPrefix(String name, @Nullable Consumer<ParsedCommand.Builder> nonParameterizedBuilderFunction)
        {
            this.name = name;
            this.hasArgument = false;
            this.parameterizedBuilderFunction = null;
            this.nonParameterizedBuilderFunction = nonParameterizedBuilderFunction;
        }
    }

    private static void addPrefixes(ImmutableList.Builder<CommandPrefix> builder,
                                    @Nullable BiConsumer<ParsedCommand.Builder, String> parameterizedBuilderFunction,
                                    String... names)
    {
        for (String name : names)
        {
            builder.add(new CommandPrefix(name, parameterizedBuilderFunction));
        }
    }

    private static void addPrefixes(ImmutableList.Builder<CommandPrefix> builder,
                                    @Nullable Consumer<ParsedCommand.Builder> nonParameterizedBuilderFunction,
                                    String... names)
    {
        for (String name : names)
        {
            builder.add(new CommandPrefix(name, nonParameterizedBuilderFunction));
        }
    }
}
