package fi.dy.masa.worldprimer.command.util;

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

    public String subString(int start, int end)
    {
        return this.string.substring(start, end + 1);
    }

    public StringReader subReader(Region region)
    {
        return this.subReader(region.start, region.end);
    }

    public StringReader subReader(int start, int end)
    {
        return new StringReader(this.subString(start, end));
    }

    public boolean skip()
    {
        if (this.pos <= this.length)
        {
            ++this.pos;
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
        return this.string.substring(this.pos).indexOf(subStr);
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
        return "StringReader{string='" + string + "',length=" + length + ",pos=" + pos + ",storedPos=" + storedPos + '}';
    }
}
