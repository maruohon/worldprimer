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
    //private static final String NUM = "0123456789.";
    //private static final String NUM_OP_PAREN = "0123456789.-+*/%()";

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
                                            ExpressionParser expressionParser) throws SyntaxErrorException
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
                Expression expression = pair.getRight();

                System.out.printf("CommandParser.getCommand(): Arithmetic Region: [%d, %d], expression: '%s'\n",
                                  arithmeticRegion.start, arithmeticRegion.end, expression.getOriginalString());
                // Add the part of the string before the arithmetic part starts
                if (arithmeticRegion.start > unconsumedStartPos)
                {
                    builder.add(plainString(reader, unconsumedStartPos, arithmeticRegion.start - 1));
                }

                builder.add(new ArithmeticExpression(expression, expressionParser));
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
                            ExpressionParser expressionParser) throws SyntaxErrorException
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

    /*
    public static ParsedCommand oldparseCommand(final String original)
    {
        StringReader reader = new StringReader(original);
        ImmutableList.Builder<StringSubstitution> builder = ImmutableList.builder();
        // say foo bar {SPAWN_X}+{SPAWN_Y}+{SPAWN_Z}*{DIMENSION}
        // say foo bar {RAND:0,15}*16+8
        // say foo bar {RAND:0,15}*({RAND:0,15}*((16+{RAND:0,7})*(5-{RAND:0,3}))-5*{RAND:0,2})
        int pos = 0;

        while (reader.canRead())
        {
            Region substitutionRegion = getNextSubstitutionRegion(reader);

            // No more substitutions, return the rest of the command as a simple string
            if (substitutionRegion == null)
            {
                final String str = reader.subString(pos, reader.getLength() - 1);
                builder.add(plainStringWithStrippedEscapes(str));
                //System.out.printf("parseCommand() end: '%s'\n", str);
                break;
            }

            //System.out.printf("parseCommand() sub @ %s => '%s'\n", substitutionRegion, reader.subReader(substitutionRegion).getString());
            BaseSubstitution substitution = getSubstitutionForRegion(reader, substitutionRegion);
            Region arithmeticRegion = substitution.isNumeric() ? getArithmeticRegion(reader, pos, substitutionRegion) : null;
            int replacedStart = arithmeticRegion != null ? arithmeticRegion.start : substitutionRegion.start;
            int replacedEnd = arithmeticRegion != null ? arithmeticRegion.end : substitutionRegion.end;

            // Add the previous plain string before the substitution
            if (replacedStart - pos > 0)
            {
                final String str = reader.subString(pos, replacedStart - 1);
                builder.add(plainStringWithStrippedEscapes(str));
                //System.out.printf("parseCommand() prev: '%s' sub.isNum: %s\n", str, substitution.isNumeric());
            }

            // Arithmetic operations with a substitution
            if (arithmeticRegion != null)
            {
                //System.out.printf("parseCommand() arithmetic @ %s => '%s'\n", arithmeticRegion, reader.subReader(arithmeticRegion).getString());
                // StringReader subReader = reader.subReader(arithmeticRegion);
                // StringSubstitution provider = ArithmeticEquationParser.getArithmeticSubstitutionFor(subReader);
                //
                // if (provider != null)
                // {
                //     builder.add(provider);
                // }
                // else
                // {
                //     WorldPrimer.LOGGER.warn("Failed to get and parse an arithmetic equation for '{}'", subReader.getString());
                // }
            }
            // Just a simple substitution
            else
            {
                final String str = reader.subString(substitutionRegion);
                BaseSubstitution finalSubstitution = substitution.buildSubstitution(str);

                if (finalSubstitution != null)
                {
                    //System.out.printf("parseCommand() regular sub: %s\n", substitution);
                    builder.add(substitution);
                }
                else
                {
                    //System.out.printf("parseCommand() invalid sub, falling back to string: '%s'\n", str);
                    builder.add(plainStringWithStrippedEscapes(str));
                }
            }

            pos = replacedEnd + 1;
            reader.setPos(pos);
        }

        return new ParsedCommand(builder.build(), original);
    }

    private static PlainString plainStringWithStrippedEscapes(String str)
    {
        int firstEscape = str.indexOf('\\');

        if (firstEscape != -1)
        {
            final int length = str.length();
            StringBuilder sb = new StringBuilder(length);
            StringReader reader = new StringReader(str);

            if (firstEscape > 0)
            {
                sb.append(str, 0, firstEscape);
            }

            reader.setPos(firstEscape);

            while (reader.canRead())
            {
                char c = reader.peek();
                char n = reader.peekNext();
                reader.skip();

                if (c == '\\' && (n == '{' || n == '}' || OP.indexOf(n) != -1))
                {
                    continue;
                }

                sb.append(c);
            }

            str = sb.toString();
        }

        return new PlainString(str);
    }

    @Nullable
    private static Region getNextSubstitutionRegion(StringReader reader)
    {
        final int origPos = reader.getPos();
        int startPos = -1;
        int nesting = 0;
        Region region = null;

        while (reader.canRead())
        {
            char c = reader.peek();
            char p = reader.peekPrevious();

            if (c == '{' && p != '\\')
            {
                if (nesting == 0)
                {
                    startPos = reader.getPos();
                }

                ++nesting;
            }
            else if (c == '}' && p != '\\')
            {
                if (nesting > 0 && --nesting <= 0)
                {
                    region = new Region(startPos, reader.getPos());

                    // Check if the substitution is valid, and if not, discard it and continue the search
                    // from the next position after the current start.
                    if (isValidSubstitution(reader, region) == false)
                    {
                        reader.setPos(startPos + 1);
                        region = null;
                        nesting = 0;
                        startPos = -1;
                        continue;
                    }
                    else
                    {
                        break;
                    }
                }
            }

            reader.skip();
        }

        reader.setPos(origPos);

        return region;
    }

    @Nullable
    private static Region getSubstitutionRegionStartingAt(StringReader reader, int startPos)
    {
        if (reader.peekAt(startPos) != '{')
        {
            return null;
        }

        final int origPos = reader.getPos();
        reader.setPos(startPos);
        Region region = getNextSubstitutionRegion(reader);
        reader.setPos(origPos);

        return region != null && region.start == startPos ? region : null;
    }

    @Nullable
    private static Region getSubstitutionRegionEndingAt(StringReader reader, int endPos)
    {
        if (reader.peekAt(endPos) != '}')
        {
            return null;
        }

        final int origPos = reader.getPos();
        int pos = endPos;
        int nesting = 0;
        Region region = null;

        while (pos >= 0)
        {
            char c = reader.peekAt(pos);
            char p = reader.peekAt(pos - 1);

            if (c == '{' && p != '\\')
            {
                if (nesting < 0 && ++nesting == 0)
                {
                    region = new Region(pos, endPos);

                    // Check if the substitution is valid, and if not, just abort
                    if (isValidSubstitution(reader, region) == false)
                    {
                        region = null;
                    }

                    break;
                }
            }
            else if (c == '}' && p != '\\')
            {
                --nesting;
            }

            --pos;
        }

        reader.setPos(origPos);

        return region;
    }

    private static boolean isValidSubstitutionEndingAt(StringReader reader, int endPos)
    {
        return getSubstitutionRegionEndingAt(reader, endPos) != null;
    }

    private static boolean isValidSubstitution(StringReader reader, Region region)
    {
        return getSubstitutionForRegion(reader, region) != null;
    }

    @Nullable
    private static BaseSubstitution getSubstitutionForRegion(StringReader reader, Region region)
    {
        String substitutionString = reader.subString(region.start + 1, region.end - 1);
        return getSubstitutionForString(substitutionString);
    }

    @Nullable
    private static BaseSubstitution getSubstitutionForString(String substitutionString)
    {
        String name = substitutionString;
        int colonIndex = name.indexOf(':');
        boolean hasArgs = colonIndex != -1;

        BaseSubstitution substitution = null;

        if (substitution != null &&
            substitution.hasArguments() == hasArgs &&
            substitution.isArgumentValid(substitutionString))
        {
            return substitution;
        }

        return null;
    }

    @Nullable
    private static Region getArithmeticRegion(StringReader reader, int startPos, Region substitutionRegion)
    {
        reader.storePos();

        int start = substitutionRegion.start;
        int end = substitutionRegion.end;
        int pos = start;

        // (-(123+(456-7)*8/((9+5)-12))/56)+{FOO}-15*2*{BAR} foo baz
        // -(123+(456-7)*8/((9+5)-12))/56

        // Check for valid arithmetic equations preceding the substitution.
        if (pos >= (startPos + 2) && reader.peekAt(pos - 2) != '\\')
        {
            char c = reader.peekAt(pos - 1);

            // First check for an (un-escaped) operator or an opening parenthesis preceding the substitution region.
            if (c == '(' || OP.indexOf(c) != -1)
            {
                pos -= 1;
                int prev = pos;

                // There can't be preceding substitutions, because this
                // method is called for the first found substitution in
                // the still unhandled part of the input string.
                while (pos >= startPos && NUM_OP_PAREN.indexOf(reader.peekAt(pos)) != -1 && reader.peekAt(pos - 1) != '\\')
                {
                    prev = pos;
                    --pos;
                }

                start = prev;
            }
        }

        final int length = reader.getLength();
        pos = substitutionRegion.end + 1;

        // Check for valid arithmetic equations following the substitution.
        if (length > (pos + 1))
        {
            char c = reader.peekAt(pos);

            // First check for an (un-escaped) operator or a closing parenthesis following the substitution region.
            if (c == ')' || OP.indexOf(c) != -1)
            {
                int prev = pos;

                while (pos < length)
                {
                    c = reader.peekAt(pos);

                    // Escape character, no more possible arithmetic stuff, abort
                    if (c == '\\')
                    {
                        break;
                    }

                    // If the character is not one of the arithmetic characters, then
                    // check if this is the beginning of a numeric substitution.
                    if (NUM_OP_PAREN.indexOf(c) == -1)
                    {
                        Region region = getSubstitutionRegionStartingAt(reader, pos);

                        if (region != null)
                        {
                            BaseSubstitution sub = getSubstitutionForRegion(reader, region);

                            if (sub != null && sub.isNumeric())
                            {
                                prev = region.end;
                                pos = prev + 1;
                                continue;
                            }
                        }

                        break;
                    }

                    prev = pos;
                    ++pos;
                }

                end = prev;
            }
        }

        //System.out.printf("getArithmeticRegion() start: %d, end: %d, orig sub: %s -> '%s'\n", start, end, substitutionRegion, reader.subReader(start, end).getString());
        if (start != substitutionRegion.start || end != substitutionRegion.end)
        {
            StringReader subReader = reader.subReader(start, end);

            if (validateArithmeticEquation(subReader))
            {
                return new Region(start, end);
            }
        }

        return null;
    }

    private static boolean validateArithmeticEquation(StringReader reader)
    {
        reader.setPos(0);

        int parenLevel = 0;
        char lastChar = 0;

        while (reader.canRead())
        {
            char c = reader.peek();

            if (c == '(')
            {
                // An opening parenthesis can only be preceded by another opening
                // parenthesis or an operator, or it can be the first character.
                // OK: "(((123+456..." ; "123/((456-7)*3)" ; "(123+45)/7"
                // NOT: "123(456+78)/9" ; "{FOO}(123+45)/8" ; "123)(456"
                if (lastChar != 0 && lastChar != '(' && OP.indexOf(lastChar) == -1)
                {
                    //System.out.printf("validateArithmeticEquation(): invalid left paren @ %d\n", reader.getPos());
                    return false;
                }

                ++parenLevel;
            }
            else if (c == ')')
            {
                // A closing parenthesis can only be preceded by another closing parenthesis or a number.
                // OK: "((123+4)/(3+7))*4" ; "(123+{FOO})/8"
                // NOT: "((123+4-)/8)-65" ; "(123+{FOO}()/8"
                if (lastChar != ')' && NUM.indexOf(lastChar) == -1 && isValidSubstitutionEndingAt(reader, reader.getPos() - 1) == false)
                {
                    //System.out.printf("validateArithmeticEquation(): invalid right paren @ %d -> '%s'\n", reader.getPos(), reader.subString(0, reader.getPos()));
                    return false;
                }

                --parenLevel;
            }
            // Two operators next to each other "123++456"
            else if (OP.indexOf(c) != -1 && OP.indexOf(lastChar) != -1)
            {
                //System.out.printf("validateArithmeticEquation(): double op: '%s'\n", "" + lastChar + c);
                return false;
            }

            // Unbalanced parenthesis such that there were more closing
            // than opening parenthesis up to this point
            if (parenLevel < 0)
            {
                //System.out.printf("validateArithmeticEquation(): parenLevel < 0 (%d)\n", parenLevel);
                return false;
            }

            lastChar = c;
            reader.skip();
        }

        //System.out.printf("validateArithmeticEquation(): parenLevel: %d\n", parenLevel);
        // Check for balanced parenthesis count
        return parenLevel == 0;
    }
    */
}
