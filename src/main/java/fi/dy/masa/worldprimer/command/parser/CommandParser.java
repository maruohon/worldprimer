package fi.dy.masa.worldprimer.command.parser;

import java.util.List;
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
import fi.dy.masa.worldprimer.command.substitution.PlainString;
import fi.dy.masa.worldprimer.command.substitution.StringSubstitution;

public class CommandParser
{
    private static final String OP = "-+*/%";

    @Nullable
    public static ParsedCommand parseCommand(String originalString,
                                             SubstitutionParser substitutionParser,
                                             ExpressionParser expressionParser) throws SyntaxErrorException
    {
        StringReader reader = new StringReader(originalString);
        Expression conditionExpression = getConditionExpression(reader, substitutionParser, expressionParser);
        ParsedCommand baseCommand = getCommand(reader, substitutionParser, expressionParser);

        System.out.printf("CommandParser.parseCommand(): baseCommand = '%s', conditionExpression = '%s'\n", baseCommand != null ? baseCommand.getOriginalString() : "<null>", conditionExpression != null ? conditionExpression.getOriginalString() : "<null>");
        if (baseCommand != null && conditionExpression != null)
        {
            return new ConditionalCommand(baseCommand, conditionExpression);
        }

        return baseCommand;
    }

    @Nullable
    private static Expression getConditionExpression(StringReader reader,
                                                     SubstitutionParser substitutionParser,
                                                     ExpressionParser expressionParser) throws SyntaxErrorException
    {
        String prefix = "worldprimer-condition[";
        String subString = reader.subString();
        int prefixLength = prefix.length();

        if (subString.startsWith(prefix) && subString.length() > prefixLength + 2)
        {
            int startIndex = prefixLength;
            int endIndex = subString.indexOf(']', startIndex + 1);

            if (endIndex != -1)
            {
                String conditionString = subString.substring(startIndex, endIndex);
                StringTokenizer tokenizer = new StringTokenizer(substitutionParser, new StringReader(conditionString));
                List<Token> tokens = expressionParser.parseAndReduceToRpn(tokenizer);

                if (tokens.isEmpty() == false)
                {
                    reader.setPos(reader.getPos() + endIndex + 1);

                    while (reader.canRead() && reader.peek() == ' ')
                    {
                        reader.skip();
                    }

                    return new Expression(tokens, conditionString);
                }
            }
        }

        return null;
    }

    @Nullable
    private static ParsedCommand getCommand(StringReader reader,
                                            SubstitutionParser substitutionParser,
                                            ExpressionParser expressionParser)
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
                if (reader.getPos() < reader.getLength() - 1)
                {
                    builder.add(plainString(reader, unconsumedStartPos));
                }

                break;
            }

            Pair<Region, Expression> pair = getArithmeticExpression(reader, substitutionRegion,
                                                                    unconsumedStartPos, expressionParser);

            // Found an arithmetic expression encompassing the next substitution
            if (pair != null)
            {
                Region arithmeticRegion = pair.getLeft();

                System.out.printf("CommandParser.getCommand(): Arithmetic Region: [%d, %d], expression: '%s'\n",
                                  arithmeticRegion.start, arithmeticRegion.end, pair.getRight().getOriginalString());
                // Add the part of the string before the arithmetic part starts
                if (arithmeticRegion.start > unconsumedStartPos)
                {
                    builder.add(plainString(reader, unconsumedStartPos, arithmeticRegion.start - 1));
                }

                builder.add(new ArithmeticExpression(pair.getRight(), expressionParser));
                unconsumedStartPos = arithmeticRegion.end + 1;
                reader.setPos(unconsumedStartPos);
            }
            // Just a plain substitution
            else
            {
                BaseSubstitution substitution = substitutionParser.getSubstitutionForFullRegion(reader, substitutionRegion, true);

                if (substitution != null)
                {
                    // Add the part of the string before the substitution starts
                    if (substitutionRegion.start > unconsumedStartPos)
                    {
                        builder.add(plainString(reader, unconsumedStartPos, substitutionRegion.start - 1));
                    }

                    builder.add(substitution);
                    unconsumedStartPos = substitutionRegion.end + 1;
                    reader.setPos(unconsumedStartPos);
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
            return new ParsedCommand(parts, reader.subString(startPos));
        }

        return null;
    }

    private static PlainString plainString(StringReader reader, int startPos)
    {
        return plainString(reader, startPos, reader.getLength() - 1);
    }

    private static PlainString plainString(StringReader reader, int startPos, int endPos)
    {
        String str = reader.subString(startPos, endPos).replaceAll("\\\\([*/%+-])", "\1").replaceAll("\\\\", "\\");
        return new PlainString(str);
    }

    @Nullable
    private static Pair<Region, Expression>
    getArithmeticExpression(StringReader reader, Region substitutionRegion, int minimumPos,
                            ExpressionParser expressionParser)
    {
        int startPos = substitutionRegion.start;
        int endPos = substitutionRegion.end;
        char charBefore = reader.peekAt(substitutionRegion.start - 1);
        char charAfter = reader.peekAt(substitutionRegion.end + 1);

        if (substitutionRegion.start > minimumPos &&
            (charBefore == '(' || OP.indexOf(charBefore) != -1) &&
            (reader.peekAt(startPos - 2) != '\\' || reader.peekAt(startPos - 3) == '\\'))
        {
            while (startPos > minimumPos && reader.peekAt(startPos - 1) != ' ')
            {
                --startPos;
            }
        }

        if (substitutionRegion.end < reader.getLength() - 1 &&
            (charAfter == ')' || OP.indexOf(charAfter) != -1))
        {
            int max = reader.getLength() - 1;

            while (endPos < max && reader.peekAt(endPos + 1) != ' ')
            {
                ++endPos;
            }
        }

        if (startPos != substitutionRegion.start ||
            endPos != substitutionRegion.end)
        {
            StringReader subReader = reader.subReader(startPos, endPos);
            Expression expression = null;

            try
            {
                expression = expressionParser.parseAndReduceToExpression(subReader);
            }
            catch (Exception e)
            {
                WorldPrimer.logInfo("CommandParser.getArithmeticExpression(): Failed to parse expression '{}' (this may not be an error, depending on your command string structure)", subReader.getString());
            }

            if (expression != null)
            {
                return Pair.of(new Region(startPos, endPos), expression);
            }
        }

        return null;
    }
}
