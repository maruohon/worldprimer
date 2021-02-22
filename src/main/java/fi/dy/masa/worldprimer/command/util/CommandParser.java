package fi.dy.masa.worldprimer.command.util;

import java.util.HashMap;
import javax.annotation.Nullable;
import com.google.common.collect.ImmutableList;
import net.minecraft.world.World;
import fi.dy.masa.worldprimer.WorldPrimer;
import fi.dy.masa.worldprimer.command.substitutions.IStringProvider;
import fi.dy.masa.worldprimer.command.substitutions.PlainString;
import fi.dy.masa.worldprimer.command.substitutions.SubstitutionBase;
import fi.dy.masa.worldprimer.command.substitutions.SubstitutionCount;
import fi.dy.masa.worldprimer.command.substitutions.SubstitutionDimension;
import fi.dy.masa.worldprimer.command.substitutions.SubstitutionPlayerName;
import fi.dy.masa.worldprimer.command.substitutions.SubstitutionPlayerPosition;
import fi.dy.masa.worldprimer.command.substitutions.SubstitutionPlayerPosition.PositionType;
import fi.dy.masa.worldprimer.command.substitutions.SubstitutionRandomNumber;
import fi.dy.masa.worldprimer.command.substitutions.SubstitutionRealTime;
import fi.dy.masa.worldprimer.command.substitutions.SubstitutionSpawnPoint;
import fi.dy.masa.worldprimer.command.substitutions.SubstitutionTopBlockY;
import fi.dy.masa.worldprimer.command.substitutions.SubstitutionTopBlockYRand;
import fi.dy.masa.worldprimer.command.substitutions.SubstitutionWorldTime;
import fi.dy.masa.worldprimer.util.Coordinate;
import fi.dy.masa.worldprimer.util.WorldUtils;

public class CommandParser
{
    public static final String NUM = "0123456789.";
    public static final String OP = "-+*/%";
    private static final String NUM_OP_PAREN = "0123456789.-+*/%()";

    private static final HashMap<String, SubstitutionBase> SUBSTITUTIONS = new HashMap<>();

    public static void init()
    {
        SUBSTITUTIONS.clear();

        SUBSTITUTIONS.put("PLAYER_NAME", new SubstitutionPlayerName());
        SUBSTITUTIONS.put("PLAYER_BED_X", new SubstitutionPlayerPosition(PositionType.BED_POSITION, Coordinate.X));
        SUBSTITUTIONS.put("PLAYER_BED_Y", new SubstitutionPlayerPosition(PositionType.BED_POSITION, Coordinate.Y));
        SUBSTITUTIONS.put("PLAYER_BED_Z", new SubstitutionPlayerPosition(PositionType.BED_POSITION, Coordinate.Z));
        SUBSTITUTIONS.put("PLAYER_BED_SPAWN_X", new SubstitutionPlayerPosition(PositionType.BED_SPAWN_POSITION, Coordinate.X));
        SUBSTITUTIONS.put("PLAYER_BED_SPAWN_Y", new SubstitutionPlayerPosition(PositionType.BED_SPAWN_POSITION, Coordinate.Y));
        SUBSTITUTIONS.put("PLAYER_BED_SPAWN_Z", new SubstitutionPlayerPosition(PositionType.BED_SPAWN_POSITION, Coordinate.Z));
        SUBSTITUTIONS.put("PLAYER_BLOCK_X", new SubstitutionPlayerPosition(PositionType.BLOCK_POSITION, Coordinate.X));
        SUBSTITUTIONS.put("PLAYER_BLOCK_Y", new SubstitutionPlayerPosition(PositionType.BLOCK_POSITION, Coordinate.Y));
        SUBSTITUTIONS.put("PLAYER_BLOCK_Z", new SubstitutionPlayerPosition(PositionType.BLOCK_POSITION, Coordinate.Z));
        SUBSTITUTIONS.put("PLAYER_X", new SubstitutionPlayerPosition(PositionType.EXACT_POSITION, Coordinate.X));
        SUBSTITUTIONS.put("PLAYER_Y", new SubstitutionPlayerPosition(PositionType.EXACT_POSITION, Coordinate.Y));
        SUBSTITUTIONS.put("PLAYER_Z", new SubstitutionPlayerPosition(PositionType.EXACT_POSITION, Coordinate.Z));

        SUBSTITUTIONS.put("COUNT", new SubstitutionCount());
        SUBSTITUTIONS.put("DIMENSION", new SubstitutionDimension());
        SUBSTITUTIONS.put("SPAWN_X", new SubstitutionSpawnPoint(Coordinate.X, WorldUtils::getWorldSpawn));
        SUBSTITUTIONS.put("SPAWN_Y", new SubstitutionSpawnPoint(Coordinate.Y, WorldUtils::getWorldSpawn));
        SUBSTITUTIONS.put("SPAWN_Z", new SubstitutionSpawnPoint(Coordinate.Z, WorldUtils::getWorldSpawn));
        SUBSTITUTIONS.put("SPAWN_POINT_X", new SubstitutionSpawnPoint(Coordinate.X, World::getSpawnPoint));
        SUBSTITUTIONS.put("SPAWN_POINT_Y", new SubstitutionSpawnPoint(Coordinate.Y, World::getSpawnPoint));
        SUBSTITUTIONS.put("SPAWN_POINT_Z", new SubstitutionSpawnPoint(Coordinate.Z, World::getSpawnPoint));
        SUBSTITUTIONS.put("TIME_TICK", new SubstitutionWorldTime(World::getTotalWorldTime));
        SUBSTITUTIONS.put("TIME_TICK_DAY", new SubstitutionWorldTime(World::getWorldTime));
        SUBSTITUTIONS.put("TIME_Y", new SubstitutionRealTime("yyyy"));
        SUBSTITUTIONS.put("TIME_M", new SubstitutionRealTime("MM"));
        SUBSTITUTIONS.put("TIME_D", new SubstitutionRealTime("dd"));
        SUBSTITUTIONS.put("TIME_H", new SubstitutionRealTime("HH"));
        SUBSTITUTIONS.put("TIME_I", new SubstitutionRealTime("mm"));
        SUBSTITUTIONS.put("TIME_S", new SubstitutionRealTime("ss"));
        SUBSTITUTIONS.put("TOP_Y", new SubstitutionTopBlockY());
        SUBSTITUTIONS.put("TOP_Y_RAND", new SubstitutionTopBlockYRand());

        SUBSTITUTIONS.put("RAND", new SubstitutionRandomNumber());
    }

    public static ParsedCommand parseCommand(final String original)
    {
        StringReader reader = new StringReader(original);
        ImmutableList.Builder<IStringProvider> builder = ImmutableList.builder();
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
            SubstitutionBase substitution = getSubstitutionForRegion(reader, substitutionRegion);
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
                StringReader subReader = reader.subReader(arithmeticRegion);
                IStringProvider provider = ArithmeticEquationParser.getArithmeticSubstitutionFor(subReader);

                if (provider != null)
                {
                    builder.add(provider);
                }
                else
                {
                    WorldPrimer.logger.warn("Failed to get and parse an arithmetic equation for '{}'", subReader.getString());
                }
            }
            // Just a simple substitution
            else
            {
                final String str = reader.subString(substitutionRegion);
                SubstitutionBase finalSubstitution = substitution.buildSubstitution(str);

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

        return new ParsedCommand(builder.build());
    }

    public static PlainString plainStringWithStrippedEscapes(String str)
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
    public static Region getSubstitutionRegionStartingAt(StringReader reader, int startPos)
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
    public static SubstitutionBase getSubstitutionForRegion(StringReader reader, Region region)
    {
        String substitutionString = reader.subString(region.start + 1, region.end - 1);
        String name = substitutionString;
        int colonIndex = name.indexOf(':');
        boolean hasArgs = false;

        if (colonIndex != -1)
        {
            name = name.substring(0, colonIndex);
            hasArgs = true;
        }

        SubstitutionBase substitution = SUBSTITUTIONS.get(name);

        if (substitution != null &&
            substitution.hasArguments() == hasArgs &&
            substitution.isValid(substitutionString))
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
                            SubstitutionBase sub = getSubstitutionForRegion(reader, region);

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
}
