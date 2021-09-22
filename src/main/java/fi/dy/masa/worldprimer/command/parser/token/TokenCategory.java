package fi.dy.masa.worldprimer.command.parser.token;

public enum TokenCategory
{
    PRECEDENCE,
    OP_BOOL_UNARY,
    OP_BOOL_BINARY,
    OP_ARITH_UNARY,
    OP_ARITH_BINARY,
    OP_NUM_RELATION,
    OP_EQUALITY,
    CONSTANT_VALUE,
    VARIABLE_VALUE,
    INVALID
}
