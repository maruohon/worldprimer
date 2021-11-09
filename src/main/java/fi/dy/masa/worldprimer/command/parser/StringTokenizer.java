package fi.dy.masa.worldprimer.command.parser;

import java.util.Optional;
import javax.annotation.Nullable;
import fi.dy.masa.worldprimer.WorldPrimer;
import fi.dy.masa.worldprimer.command.parser.operator.ArithmeticBinaryOperator;
import fi.dy.masa.worldprimer.command.parser.operator.BooleanBinaryOperator;
import fi.dy.masa.worldprimer.command.parser.operator.EqualityOperator;
import fi.dy.masa.worldprimer.command.parser.operator.NumericRelationOperator;
import fi.dy.masa.worldprimer.command.parser.token.ArithmeticBinaryOperatorToken;
import fi.dy.masa.worldprimer.command.parser.token.BooleanBinaryOperatorToken;
import fi.dy.masa.worldprimer.command.parser.token.EqualityOperatorToken;
import fi.dy.masa.worldprimer.command.parser.token.NumericRelationOperatorToken;
import fi.dy.masa.worldprimer.command.parser.token.Token;
import fi.dy.masa.worldprimer.command.parser.token.TokenCategory;
import fi.dy.masa.worldprimer.command.parser.token.TokenType;
import fi.dy.masa.worldprimer.command.parser.token.ValueToken;
import fi.dy.masa.worldprimer.command.parser.value.DoubleValue;
import fi.dy.masa.worldprimer.command.parser.value.IntValue;
import fi.dy.masa.worldprimer.command.parser.value.StringValue;
import fi.dy.masa.worldprimer.command.parser.value.SubstitutionValue;
import fi.dy.masa.worldprimer.command.parser.value.Value;
import fi.dy.masa.worldprimer.command.substitution.BaseSubstitution;

public class StringTokenizer
{
    protected final SubstitutionParser substitutionParser;
    protected final StringReader reader;
    @Nullable protected Token previousToken;

    public StringTokenizer(SubstitutionParser substitutionParser, StringReader reader)
    {
        this.substitutionParser = substitutionParser;
        this.reader = reader;
    }

    public StringReader getReader()
    {
        return this.reader;
    }

    @Nullable
    public Optional<Token> readNextToken() throws IllegalArgumentException
    {
        Token token = readToken(this.reader, this.substitutionParser, this.previousToken);
        this.previousToken = token;
        return Optional.ofNullable(token);
    }

    @Nullable
    public static Token readToken(StringReader reader,
                                  SubstitutionParser substitutionParser,
                                  @Nullable Token previousToken) throws IllegalArgumentException
    {
        while (reader.canRead() && reader.peek() == ' ')
        {
            reader.skip();
        }

        if (reader.canRead() == false)
        {
            return null;
        }

        final int startPos = reader.getPos();
        final char c = reader.peek();
        Token token = null;

        if (c == '(')
        {
            token = new Token(TokenType.LEFT_PAREN, "(");
        }
        else if (c == ')')
        {
            token = new Token(TokenType.RIGHT_PAREN, ")");
        }
        else if (c == '+')
        {
            if (canBeUnaryOperator(previousToken))
            {
                token = new Token(TokenType.OP_ARITH_UNARY_PLUS, "+");
            }
            else
            {
                token = new ArithmeticBinaryOperatorToken(TokenType.OP_ARITH_ADD_ADD, "+", ArithmeticBinaryOperator.PLUS);
            }
        }
        else if (c == '-')
        {
            if (canBeUnaryOperator(previousToken))
            {
                token = new Token(TokenType.OP_ARITH_UNARY_MINUS, "-");
            }
            else
            {
                token = new ArithmeticBinaryOperatorToken(TokenType.OP_ARITH_ADD_SUB, "-", ArithmeticBinaryOperator.MINUS);
            }
        }
        else if (c == '*')
        {
            token = new ArithmeticBinaryOperatorToken(TokenType.OP_ARITH_MULT_MULT, "*", ArithmeticBinaryOperator.MULTIPLY);
        }
        else if (c == '/')
        {
            token = new ArithmeticBinaryOperatorToken(TokenType.OP_ARITH_MULT_DIV, "/", ArithmeticBinaryOperator.DIVIDE);
        }
        else if (c == '%')
        {
            token = new ArithmeticBinaryOperatorToken(TokenType.OP_ARITH_MULT_MOD, "%", ArithmeticBinaryOperator.MODULO);
        }
        else if (reader.startsWith("!="))
        {
            token = new EqualityOperatorToken(TokenType.OP_EQ_NE, "!=", EqualityOperator.NOT_EQUAL);
        }
        else if (c == '!')
        {
            token = new Token(TokenType.OP_BOOLEAN_UNARY_NEG, "!");
        }
        else if (reader.startsWith(">="))
        {
            token = new NumericRelationOperatorToken(TokenType.OP_REL_GE, ">=", NumericRelationOperator.GREATER_OR_EQUAL);
        }
        else if (reader.startsWith("<="))
        {
            token = new NumericRelationOperatorToken(TokenType.OP_REL_LE, "<=", NumericRelationOperator.LESS_OR_EQUAL);
        }
        else if (c == '<')
        {
            token = new NumericRelationOperatorToken(TokenType.OP_REL_LT, "<", NumericRelationOperator.LESS);
        }
        else if (c == '>')
        {
            token = new NumericRelationOperatorToken(TokenType.OP_REL_GT, ">", NumericRelationOperator.GREATER);
        }
        else if (reader.startsWith("=="))
        {
            token = new EqualityOperatorToken(TokenType.OP_EQ_EQ, "==", EqualityOperator.EQUAL);
        }
        else if (reader.startsWith("&&"))
        {
            token = new BooleanBinaryOperatorToken(TokenType.OP_BOOL_AND, "&&", BooleanBinaryOperator.AND);
        }
        else if (reader.startsWith("||"))
        {
            token = new BooleanBinaryOperatorToken(TokenType.OP_BOOL_OR, "||", BooleanBinaryOperator.OR);
        }
        else if (c >= '0' && c <= '9')
        {
            token = readNumber(reader, startPos);
        }
        // TODO escaping?
        else if (c == '{')
        {
            token = readVariable(reader, startPos, substitutionParser);
        }

        if (token == null)
        {
            token = readString(reader, startPos);
        }

        if (token == null)
        {
            reader.setPosToEnd();
            return new Token(TokenType.INVALID, reader.subString(startPos));
        }

        //System.out.printf("pre: %d, start: %d\n", this.reader.getPos(), startPos);
        //System.out.printf("str: %s\n", token.originalString);
        reader.skip(token.getOriginalString().length());
        //System.out.printf("post: %d\n", this.reader.getPos());

        return token;
    }

    protected static boolean canBeUnaryOperator(@Nullable Token previousToken)
    {
        // -5
        // 5 + -7 ... -5 * -9
        // {DIMENSION} == -1
        // {DIMENSION} <= -1
        // (-5 + 7)
        return previousToken == null ||
               previousToken.getType().getTokenCategory() == TokenCategory.OP_ARITH_BINARY ||
               previousToken.getType().getTokenCategory() == TokenCategory.OP_EQUALITY ||
               previousToken.getType().getTokenCategory() == TokenCategory.OP_NUM_RELATION ||
               previousToken.getType() == TokenType.LEFT_PAREN;
    }

    protected static Token readNumber(StringReader reader, int startPos) throws IllegalArgumentException
    {
        int pos = startPos;
        int digitCount = 0;
        int dotCount = 0;
        int intBase = 10;
        boolean isHex = false;

        if (reader.canPeekAt(pos + 1) &&
            reader.peekAt(pos) == '0' &&
            reader.peekAt(pos + 1) == 'x')
        {
            pos += 2;
            isHex = true;
            intBase = 16;
        }

        while (reader.canPeekAt(pos))
        {
            char c = reader.peekAt(pos);

            if (c == '.')
            {
                ++dotCount;
                /*
                if (isHex || ++dotCount > 1)
                {
                    break;
                }
                */
            }
            else if (c >= '0' && c <= '9')
            {
                ++digitCount;
            }
            else if (isHex && (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F'))
            {
                ++digitCount;
            }
            else
            {
                break;
            }

            ++pos;
        }

        if (dotCount <= 1 && digitCount > 0 && (isHex == false || dotCount == 0))
        {
            TokenType type = dotCount == 1 ? TokenType.CONST_DOUBLE : TokenType.CONST_INT;
            String fullStrValue = reader.subString(startPos, pos - 1);
            String numberStrValue = isHex ? fullStrValue.substring(2) : fullStrValue;
            Value value;

            try
            {
                if (type == TokenType.CONST_DOUBLE)
                {
                    value = new DoubleValue(Double.parseDouble(numberStrValue));
                }
                else
                {
                    value = new IntValue(Integer.parseInt(numberStrValue, intBase));
                }

                return new ValueToken(type, fullStrValue, value);
            }
            catch (Exception e)
            {
                WorldPrimer.LOGGER.warn("Failed to parse a numeric value: '{}'", fullStrValue, e);
            }
        }
        else
        {
            String msg = String.format("Invalid number: '%s'", reader.subString(startPos, pos - 1));
            throw new IllegalArgumentException(msg);
        }

        return new Token(TokenType.INVALID, reader.subString(startPos));
    }

    protected static Token readString(StringReader reader, int startPos)
    {
        boolean quoted = false;
        int pos = startPos;

        if (reader.peekAt(startPos) == '"')
        {
            quoted = true;
            ++pos;
        }

        while (reader.canPeekAt(pos))
        {
            char c = reader.peekAt(pos);

            // TODO don't swallow touching substitutions

            if (quoted && c == '"' && reader.peekAt(pos - 1) != '\\')
            {
                String origString = reader.subString(startPos, pos);
                Value value = new StringValue(origString.substring(1, origString.length() - 1));
                return new ValueToken(TokenType.CONST_STR, origString, value);
            }
            else if (quoted == false && (c == ' ' || c == '\t'))
            {
                String origString = reader.subString(startPos, pos - 1);
                Value value = new StringValue(origString);
                return new ValueToken(TokenType.CONST_STR, origString, value);
            }

            ++pos;
        }

        String origString = reader.getString();
        Value value = new StringValue(origString);
        return new ValueToken(TokenType.CONST_STR, origString, value);
    }

    protected static Token readVariable(StringReader reader, int startPos, SubstitutionParser substitutionParser)
    {
        Region region = substitutionParser.getSubstitutionRegionStartingAt(reader, startPos);

        if (region != null)
        {
            BaseSubstitution substitution = substitutionParser.getSubstitutionForFullRegion(reader, region, false);

            if (substitution != null)
            {
                Value value = new SubstitutionValue(substitution);
                return new ValueToken(TokenType.VARIABLE, reader.subString(startPos, region.end), value);
            }
        }

        return null;
    }
}
