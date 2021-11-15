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
    @Nullable
    public static ParsedCommand parseCommand(String originalString,
                                             SubstitutionParser substitutionParser,
                                             ExpressionParser expressionParser) throws SyntaxErrorException
    {
        StringReader reader = new StringReader(originalString);
        Expression conditionExpression = getConditionExpression(reader, substitutionParser, expressionParser);
        ParsedCommand baseCommand = getCommand(reader, substitutionParser, expressionParser);

        System.out.printf("CommandParser.parseCommand(): baseCommand = '%s', conditionExpression = '%s'\n",
                          baseCommand != null ? baseCommand.getOriginalString() : "<null>",
                          conditionExpression != null ? conditionExpression.getOriginalString() : "<null>");
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
                if (unconsumedStartPos < reader.getLength())
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
                StringSubstitution substitution = new ArithmeticExpression(pair.getRight(), expressionParser);
                unconsumedStartPos = addAndAdvance(arithmeticRegion, substitution, builder, reader, unconsumedStartPos);
            }
            // Just a plain substitution
            else
            {
                BaseSubstitution substitution = substitutionParser.getSubstitutionForFullRegion(reader, substitutionRegion, true);

                if (substitution != null)
                {
                    System.out.printf("CommandParser.getCommand(): Substitution: [%d, %d], sub: '%s'\n",
                                      substitutionRegion.start, substitutionRegion.end, substitution.getOriginalFullSubstitutionString());
                    unconsumedStartPos = addAndAdvance(substitutionRegion, substitution, builder, reader, unconsumedStartPos);
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

    private static int addAndAdvance(Region region, StringSubstitution substitution,
                                     ImmutableList.Builder<StringSubstitution> builder,
                                     StringReader reader, int unconsumedStartPos)
    {
        // Add the part of the string before the substitution or arithmetic expression region starts
        if (region.start > unconsumedStartPos)
        {
            builder.add(plainString(reader, unconsumedStartPos, region.start - 1));
        }

        builder.add(substitution);
        unconsumedStartPos = region.end + 1;
        reader.setPos(unconsumedStartPos);

        return unconsumedStartPos;
    }

    private static PlainString plainString(StringReader reader, int startPos)
    {
        return plainString(reader, startPos, reader.getLength() - 1);
    }

    private static PlainString plainString(StringReader reader, int startPos, int endPos)
    {
        String str = reader.subString(startPos, endPos);
        str = str.replaceAll("\\$\\\\\\(", "\\$("); // $\( => $(
        str = str.replaceAll("\\$\\\\\\\\\\(", "\\$\\\\("); // $\\( => $\(
        str = str.replaceAll("\\\\\\{", "{"); // $\{ => ${
        return new PlainString(str);
    }

    @Nullable
    private static Pair<Region, Expression>
    getArithmeticExpression(StringReader reader,
                            Region substitutionRegion, int minimumPos,
                            ExpressionParser expressionParser)
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
}
