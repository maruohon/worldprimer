package fi.dy.masa.worldprimer.command.parser;

import javax.annotation.Nullable;
import fi.dy.masa.worldprimer.command.substitution.BaseSubstitution;
import fi.dy.masa.worldprimer.command.substitution.SubstitutionRegistry;

public class SubstitutionParser
{
    public static final SubstitutionParser INSTANCE = new SubstitutionParser();

    protected final SubstitutionRegistry registry;

    public SubstitutionParser()
    {
        this.registry = SubstitutionRegistry.INSTANCE;
    }

    @Nullable
    public Region getNextSubstitutionRegion(StringReader reader)
    {
        final int origPos = reader.getPos();
        int startPos = -1;
        int nesting = 0;
        Region region = null;

        while (reader.canRead())
        {
            char c = reader.peek();

            if (c == '{' && reader.peekPrevious() != '\\')
            {
                if (nesting == 0)
                {
                    startPos = reader.getPos();
                }

                ++nesting;
            }
            else if (c == '}' && nesting > 0 && --nesting == 0)
            {
                String nameAndArgs = reader.subString(startPos + 1, reader.getPos() - 1);

                // Check if the substitution is valid, and if not, discard it and continue the search
                // from the next position after the current start.
                if (this.isValidSubstitution(nameAndArgs, false))
                {
                    region = new Region(startPos, reader.getPos());
                    break;
                }
                else
                {
                    reader.setPos(startPos + 1);
                    region = null;
                    nesting = 0;
                    startPos = -1;
                    continue;
                }
            }

            reader.skip();
        }

        reader.setPos(origPos);

        return region;
    }

    @Nullable
    public Region getSubstitutionRegionStartingAt(StringReader reader, int startPos)
    {
        if (reader.peekAt(startPos) != '{')
        {
            return null;
        }

        Region region = this.getNextSubstitutionRegion(reader);
        return region != null && region.start == startPos ? region : null;
    }

    @Nullable
    public BaseSubstitution getSubstitutionStartingAt(StringReader reader, int startPos)
    {
        Region region = this.getSubstitutionRegionStartingAt(reader, startPos);
        return region != null ? this.getSubstitutionForFullRegion(reader, region, false) : null;
    }

    /*
    public boolean isValidFullSubstitution(StringReader reader, Region region)
    {
        return this.getSubstitutionForFullRegion(reader, region, false) != null;
    }
    */

    @Nullable
    public BaseSubstitution getSubstitutionForFullRegion(StringReader reader, Region region,
                                                         boolean buildFinalSubstitution)
    {
        String nameAndArgs = reader.subString(region.start + 1, region.end - 1);
        return this.getSubstitutionFor(nameAndArgs, buildFinalSubstitution);
    }

    public boolean isValidSubstitution(String nameAndArgs, boolean buildFinalSubstitution)
    {
        return this.getSubstitutionFor(nameAndArgs, buildFinalSubstitution) != null;
    }

    @Nullable
    public BaseSubstitution getSubstitutionFor(String nameAndArgs, boolean buildFinalSubstitution)
    {
        String name = nameAndArgs;
        final int colonIndex = name.indexOf(':');
        final boolean hasArgs = colonIndex != -1;

        if (hasArgs)
        {
            if (colonIndex == nameAndArgs.length() - 1)
            {
                return null;
            }

            name = name.substring(0, colonIndex);
        }

        BaseSubstitution substitution = this.registry.getSubstitution(name);

        if (substitution != null &&
            substitution.hasArguments() == hasArgs)
        {
            if (hasArgs && buildFinalSubstitution)
            {
                String arg = nameAndArgs.substring(colonIndex + 1);
                return substitution.buildSubstitution(arg);
            }
            else
            {
                return substitution;
            }
        }

        return null;
    }
}
