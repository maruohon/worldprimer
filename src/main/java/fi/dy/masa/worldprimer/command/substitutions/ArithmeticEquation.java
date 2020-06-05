package fi.dy.masa.worldprimer.command.substitutions;

import java.util.Stack;
import com.google.common.collect.ImmutableList;
import fi.dy.masa.worldprimer.command.util.ArithmeticEquationParser.Token;
import fi.dy.masa.worldprimer.command.util.ArithmeticEquationParser.TokenType;

public class ArithmeticEquation implements IStringProvider
{
    protected final ImmutableList<Token> tokens;

    public ArithmeticEquation(ImmutableList<Token> tokens)
    {
        this.tokens = tokens;
    }

    @Override
    public String getString(CommandContext context, String original)
    {
        Stack<Token> stack = new Stack<>();

        for (Token token : this.tokens)
        {
            if (token.type == TokenType.OPERATOR && stack.size() >= 2)
            {
                Token token2 = stack.pop();
                Token token1 = stack.pop();
                TokenType type1 = token1.type;
                TokenType type2 = token2.type;
                double value1;
                double value2;

                if (type1 == TokenType.DIRECT_VALUE)
                {
                    value1 = token1.numericValue;
                }
                else if (type1 == TokenType.SUBSTITUTION_VALUE)
                {
                    value1 = token1.substitution.getNumericValue(context, original);
                }
                else
                {
                    return original;
                }

                if (type2 == TokenType.DIRECT_VALUE)
                {
                    value2 = token2.numericValue;
                }
                else if (type2 == TokenType.SUBSTITUTION_VALUE)
                {
                    value2 = token2.substitution.getNumericValue(context, original);
                }
                else
                {
                    return original;
                }

                stack.push(new Token(token.operator.calculate(value1, value2)));
            }
            else
            {
                stack.push(token);
            }
        }

        //System.out.printf("ArithmeticEquation.getString(): %s\n", stack.size() == 1 ? String.valueOf(stack.pop().numericValue) : original);
        return stack.size() == 1 ? String.valueOf(stack.pop().numericValue) : original;
    }
}
