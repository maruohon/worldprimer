package fi.dy.masa.worldprimer.command.parser.token;

import fi.dy.masa.worldprimer.command.parser.value.ValueCategory;

public enum TokenType
{
    LEFT_PAREN              (0, 0, TokenCategory.PRECEDENCE,       ValueCategory.NONE),
    RIGHT_PAREN             (0, 0, TokenCategory.PRECEDENCE,       ValueCategory.NONE),
    OP_ARITH_UNARY_PLUS     (1, 1, TokenCategory.OP_ARITH_UNARY,   ValueCategory.NUMBER),
    OP_ARITH_UNARY_MINUS    (1, 1, TokenCategory.OP_ARITH_UNARY,   ValueCategory.NUMBER),
    OP_BOOLEAN_UNARY_NEG    (1, 1, TokenCategory.OP_BOOL_UNARY,    ValueCategory.BOOLEAN),
    OP_ARITH_MULT_MULT      (2, 2, TokenCategory.OP_ARITH_BINARY,  ValueCategory.NUMBER),
    OP_ARITH_MULT_DIV       (2, 2, TokenCategory.OP_ARITH_BINARY,  ValueCategory.NUMBER),
    OP_ARITH_MULT_MOD       (2, 2, TokenCategory.OP_ARITH_BINARY,  ValueCategory.NUMBER),
    OP_ARITH_ADD_ADD        (3, 2, TokenCategory.OP_ARITH_BINARY,  ValueCategory.NUMBER),
    OP_ARITH_ADD_SUB        (3, 2, TokenCategory.OP_ARITH_BINARY,  ValueCategory.NUMBER),
    OP_EQ_EQ                (4, 2, TokenCategory.OP_EQUALITY,      ValueCategory.ANY_VALUE),
    OP_EQ_NE                (4, 2, TokenCategory.OP_EQUALITY,      ValueCategory.ANY_VALUE),
    OP_REL_GT               (5, 2, TokenCategory.OP_NUM_RELATION,  ValueCategory.NUMBER),
    OP_REL_LT               (5, 2, TokenCategory.OP_NUM_RELATION,  ValueCategory.NUMBER),
    OP_REL_GE               (5, 2, TokenCategory.OP_NUM_RELATION,  ValueCategory.NUMBER),
    OP_REL_LE               (5, 2, TokenCategory.OP_NUM_RELATION,  ValueCategory.NUMBER),
    OP_BOOL_AND             (6, 2, TokenCategory.OP_BOOL_BINARY,   ValueCategory.BOOLEAN),
    OP_BOOL_OR              (7, 2, TokenCategory.OP_BOOL_BINARY,   ValueCategory.BOOLEAN),
    CONST_INT               (8, 0, TokenCategory.CONSTANT_VALUE,   ValueCategory.NUMBER),
    CONST_DOUBLE            (8, 0, TokenCategory.CONSTANT_VALUE,   ValueCategory.NUMBER),
    CONST_BOOLEAN           (8, 0, TokenCategory.CONSTANT_VALUE,   ValueCategory.BOOLEAN),
    CONST_STR               (8, 0, TokenCategory.CONSTANT_VALUE,   ValueCategory.STRING),
    VARIABLE                (8, 0, TokenCategory.VARIABLE_VALUE,   ValueCategory.UNKNOWN),
    INVALID                 (9, 0, TokenCategory.INVALID,          ValueCategory.NONE);

    private final TokenCategory tokenCategory;
    private final ValueCategory valueCategory;
    private final int precedence;
    private final int argumentCount;

    TokenType(int precedence, int argumentCount, TokenCategory tokenCategory, ValueCategory valueCategory)
    {
        this.precedence = precedence;
        this.argumentCount = argumentCount;
        this.tokenCategory = tokenCategory;
        this.valueCategory = valueCategory;
    }

    public TokenCategory getTokenCategory()
    {
        return this.tokenCategory;
    }

    public ValueCategory getValueCategory()
    {
        return this.valueCategory;
    }

    public int getArgumentCount()
    {
        return this.argumentCount;
    }

    public int getPrecedence()
    {
        return this.precedence;
    }

    public boolean hasHigherPrecedenceThan(TokenType other)
    {
        return this.precedence < other.precedence;
    }
}
