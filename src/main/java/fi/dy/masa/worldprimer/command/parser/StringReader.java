package fi.dy.masa.worldprimer.command.parser;

public class StringReader
{
    protected final String string;
    protected final int length;
    protected int pos;
    protected int storedPos;

    public StringReader(String string)
    {
        this.string = string;
        this.length = string.length();
    }

    public boolean canRead()
    {
        return this.canPeekAt(this.pos);
    }

    public boolean canPeekAt(int pos)
    {
        return pos >= 0 && pos < this.length;
    }

    public char peek()
    {
        return this.peekAt(this.pos);
    }

    public char read()
    {
        return this.peekAt(this.pos++);
    }

    public char peekPrevious()
    {
        return this.peekAt(this.pos - 1);
    }

    public char peekNext()
    {
        return this.peekAt(this.pos + 1);
    }

    public char peekAtOffset(int offset)
    {
        return this.peekAt(this.pos + offset);
    }

    public char peekAt(int pos)
    {
        return this.canPeekAt(pos) ? this.string.charAt(pos) : 0;
    }

    public String subString(Region region)
    {
        return this.subString(region.start, region.end);
    }

    /**
     * Returns the rest of the string starting from the current position
     */
    public String subString()
    {
        return this.string.substring(this.pos);
    }

    /**
     * Returns the rest of the string starting from the given position
     */
    public String subString(int start)
    {
        return this.string.substring(start);
    }

    /**
     * Note: The end index is inclusive, in contrast to the Java String::substring()
     */
    public String subString(int start, int end)
    {
        return this.string.substring(start, end + 1);
    }

    /**
     * Returns a string the length of length starting from the given position start
     */
    public String slice(int start, int length)
    {
        return this.string.substring(start, start + length);
    }

    public boolean startsWith(String str)
    {
        return this.string.startsWith(str, this.pos);
    }

    public boolean startsWith(String str, int startPos)
    {
        return this.string.startsWith(str, startPos);
    }

    public StringReader subReader(Region region)
    {
        return this.subReader(region.start, region.end);
    }

    /**
     * Note: The end index is inclusive, in contrast to the Java String::substring()
     */
    public StringReader subReader(int start, int end)
    {
        return new StringReader(this.subString(start, end));
    }

    public boolean skip()
    {
        return this.skip(1);
    }

    public boolean skip(int amount)
    {
        if (this.pos + amount <= this.length)
        {
            this.pos += amount;
            return true;
        }

        return false;
    }

    public int findNext(char c)
    {
        for (int i = this.pos; i < this.length; ++i)
        {
            if (this.string.charAt(i) == c)
            {
                return i;
            }
        }

        return -1;
    }

    public int findNext(String subStr)
    {
        return this.string.indexOf(subStr, this.pos);
    }

    public int getPos()
    {
        return this.pos;
    }

    public StringReader setPos(int pos)
    {
        if (pos >= 0 && pos <= this.length)
        {
            this.pos = pos;
        }

        return this;
    }

    public StringReader movePos(int amount)
    {
        int pos = this.pos + amount;

        if (pos >= 0 && pos <= this.length)
        {
            this.pos = pos;
        }

        return this;
    }

    public void setPosToEnd()
    {
        this.pos = this.length;
    }

    public void skipNextSpaces()
    {
        while (this.canRead() && this.peek() == ' ')
        {
            this.skip();
        }
    }

    public int getLength()
    {
        return this.length;
    }

    public void storePos()
    {
        this.storedPos = this.pos;
    }

    public void restorePos()
    {
        this.pos = this.storedPos;
    }

    public String getString()
    {
        return this.string;
    }

    @Override
    public String toString()
    {
        return String.format("StringReader{string='%s',length=%d,pos=%d,storedPos=%d}",
                             this.string, this.length, this.pos, this.storedPos);
    }
}
