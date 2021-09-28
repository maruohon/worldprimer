package fi.dy.masa.worldprimer.command.util;

import java.util.ArrayList;
import java.util.Stack;
import java.util.function.DoubleBinaryOperator;
import javax.annotation.Nullable;
import com.google.common.collect.ImmutableList;
import fi.dy.masa.worldprimer.WorldPrimer;
import fi.dy.masa.worldprimer.command.parser.Region;
import fi.dy.masa.worldprimer.command.parser.StringReader;
import fi.dy.masa.worldprimer.command.substitutions.ArithmeticEquation;
import fi.dy.masa.worldprimer.command.substitutions.IStringProvider;
import fi.dy.masa.worldprimer.command.substitutions.SubstitutionBase;

public class ArithmeticEquationParser
{
    // Shunting-yard algorithm, from https://en.wikipedia.org/wiki/Shunting-yard_algorithm
    /*
    // This implementation does not implement composite functions,functions with variable number of arguments, and unary operators.

    while there are tokens to be read do:
        read a token.
        if the token is a number, then:
            push it to the output queue.
        else if the token is a function then:
            push it onto the operator stack 
        else if the token is an operator then:
            while ((there is a operator at the top of the operator stack)
                  and ((the operator at the top of the operator stack has greater precedence)
                   or (the operator at the top of the operator stack has equal precedence and the token is left associative))
                  and (the operator at the top of the operator stack is not a left parenthesis)):
                pop operators from the operator stack onto the output queue.
            push it onto the operator stack.
        else if the token is a left parenthesis (i.e. "("), then:
            push it onto the operator stack.
        else if the token is a right parenthesis (i.e. ")"), then:
            while the operator at the top of the operator stack is not a left parenthesis:
                pop the operator from the operator stack onto the output queue.
            // If the stack runs out without finding a left parenthesis, then there are mismatched parentheses.
            if there is a left parenthesis at the top of the operator stack, then:
                pop the operator from the operator stack and discard it
    // After while loop, if operator stack not null, pop everything to output queue
    if there are no more tokens to read then:
        while there are still operator tokens on the stack:
            // If the operator token on the top of the stack is a parenthesis, then there are mismatched parentheses.
            pop the operator from the operator stack onto the output queue.
    exit.
    */

    @Nullable
    private static ArrayList<Token> tokenizeToRpn(StringReader reader)
    {
        ArrayList<Token> outputQueue = new ArrayList<>();
        Stack<Token> operatorStack = new Stack<>();

        while (reader.canRead())
        {
            int pos = reader.getPos();
            Token token = readToken(reader);

            if (token == null)
            {
                WorldPrimer.LOGGER.warn("Failed to read token at position {} in an arithmetic equation '{}'", pos, reader.getString());
                return null;
            }

            TokenType type = token.type;

            if (type == TokenType.DIRECT_VALUE || type == TokenType.SUBSTITUTION_VALUE)
            {
                outputQueue.add(token);
            }
            else if (type == TokenType.OPERATOR)
            {
                while (operatorStack.isEmpty() == false)
                {
                    Token op = operatorStack.peek();

                    if (op.type != TokenType.OPERATOR || op.operator.getPrecedence() >= token.operator.getPrecedence())
                    {
                        break;
                    }

                    outputQueue.add(operatorStack.pop());
                }

                operatorStack.push(token);
            }
            else if (type == TokenType.LEFT_PAREN)
            {
                operatorStack.push(token);
            }
            else if (type == TokenType.RIGHT_PAREN)
            {
                while (operatorStack.isEmpty() == false && operatorStack.peek().type != TokenType.LEFT_PAREN)
                {
                    outputQueue.add(operatorStack.pop());
                }

                // Mismatched parenthesis
                if (operatorStack.isEmpty())
                {
                    WorldPrimer.LOGGER.warn("Mismatched parenthesis at position {} in an arithmetic equation '{}'", pos, reader.getString());
                    return null;
                }

                if (operatorStack.peek().type == TokenType.LEFT_PAREN)
                {
                    operatorStack.pop();
                }
            }
        }

        while (operatorStack.isEmpty() == false)
        {
            Token op = operatorStack.pop();

            // Mismatched parenthesis
            if (op.type == TokenType.LEFT_PAREN || op.type == TokenType.RIGHT_PAREN)
            {
                WorldPrimer.LOGGER.warn("Mismatched parenthesis in operator stack (size: {}) in an arithmetic equation '{}', stack: {}", operatorStack.size() + 1, reader.getString(), operatorStack);
                return null;
            }

            outputQueue.add(op);
        }

        return outputQueue;
    }

    @Nullable
    private static Token readToken(StringReader reader)
    {
        final int startPos = reader.getPos();

        while (reader.canRead())
        {
            char c = reader.read();

            if (c == '(')
            {
                return new Token(TokenType.LEFT_PAREN);
            }
            else if (c == ')')
            {
                return new Token(TokenType.RIGHT_PAREN);
            }
            else if (CommandParser.OP.indexOf(c) != -1)
            {
                return new Token(Operator.of(c));
            }
            else if (CommandParser.NUM.indexOf(c) != -1)
            {
                while (CommandParser.NUM.indexOf(reader.peek()) != -1)
                {
                    reader.skip();
                }

                return new Token(Double.parseDouble(reader.subString(startPos, reader.getPos() - 1)));
            }
            else if (c == '{')
            {
                Region region = CommandParser.getSubstitutionRegionStartingAt(reader, startPos);

                if (region != null)
                {
                    SubstitutionBase substitution = CommandParser.getSubstitutionForRegion(reader, region);

                    if (substitution != null && substitution.isNumeric())
                    {
                        reader.setPos(region.end + 1);
                        return new Token(substitution);
                    }
                }
            }
            else
            {
                WorldPrimer.LOGGER.warn("Invalid character '{}' at position {} in an arithmetic equation '{}'", c, reader.getPos() - 1, reader.getString());
                return null;
            }
        }

        return null;
    }

    @Nullable
    private static ArrayList<Token> reduceRpn(ArrayList<Token> tokenQueue)
    {
        ArrayList<Token> reducedTokens = new ArrayList<>(tokenQueue);

        for (int i = 2; i < reducedTokens.size(); ++i)
        {
            Token token = reducedTokens.get(i);

            if (token.type == TokenType.OPERATOR)
            {
                Token val1 = reducedTokens.get(i - 2);
                Token val2 = reducedTokens.get(i - 1);

                // If there are direct numeric values that can be computed,
                // then do those calculations and replace the operands and the
                // operator in the token queue with the calculated value.
                if (val1.type == TokenType.DIRECT_VALUE &&
                    val2.type == TokenType.DIRECT_VALUE)
                {
                    //System.out.printf("reducing %.4f and %.4f with %s\n", val1.numericValue, val2.numericValue, token.operator); // TODO remove after debugging
                    reducedTokens.set(i - 2, new Token(token.operator.calculate(val1.numericValue, val2.numericValue)));
                    reducedTokens.remove(i);
                    reducedTokens.remove(i - 1);
                    i -= 1; // continue from the position following the newly replaced value token
                }
            }
        }

        return reducedTokens;
    }

    @Nullable
    public static IStringProvider getArithmeticSubstitutionFor(StringReader reader)
    {
        ArrayList<Token> tokens = tokenizeToRpn(reader);

        if (tokens != null)
        {
            tokens = reduceRpn(tokens);
            return tokens != null ? new ArithmeticEquation(ImmutableList.copyOf(tokens)) : null;
        }

        return null;
    }

    public static class Token
    {
        public final TokenType type;
        public final Operator operator;
        public final double numericValue;
        public final SubstitutionBase substitution;

        public Token(TokenType type)
        {
            this.type = type;
            this.operator = null;
            this.numericValue = 0.0;
            this.substitution = null;
        }

        public Token(Operator operator)
        {
            this.type = TokenType.OPERATOR;
            this.operator = operator;
            this.numericValue = 0.0;
            this.substitution = null;
        }

        public Token(double numericValue)
        {
            this.type = TokenType.DIRECT_VALUE;
            this.operator = null;
            this.numericValue = numericValue;
            this.substitution = null;
        }

        public Token(SubstitutionBase substitution)
        {
            this.type = TokenType.SUBSTITUTION_VALUE;
            this.operator = null;
            this.numericValue = 0.0;
            this.substitution = substitution;
        }

        @Override
        public String toString()
        {
            return "Token{type=" + type + ", operator=" + operator + ", numericValue=" + numericValue + ", substitution=" + substitution + '}';
        }
    }

    public enum TokenType
    {
        OPERATOR,
        DIRECT_VALUE,
        SUBSTITUTION_VALUE,
        LEFT_PAREN,
        RIGHT_PAREN;
    }

    public enum Operator
    {
        PLUS        (2, (v1, v2) -> v1 + v2),
        MINUS       (2, (v1, v2) -> v1 - v2),
        MULTIPLY    (3, (v1, v2) -> v1 * v2),
        DIVIDE      (3, (v1, v2) -> v1 / v2),
        MODULO      (3, (v1, v2) -> v1 % v2);

        private final DoubleBinaryOperator operation;
        private final int precedence;

        Operator(int precedence, DoubleBinaryOperator operation)
        {
            this.operation = operation;
            this.precedence = precedence;
        }

        public int getPrecedence()
        {
            return this.precedence;
        }

        public double calculate(double val1, double val2)
        {
            return this.operation.applyAsDouble(val1, val2);
        }

        @Nullable
        public static Operator of(char op)
        {
            switch (op)
            {
                case '*': return MULTIPLY;
                case '/': return DIVIDE;
                case '%': return MODULO;
                case '+': return PLUS;
                case '-': return MINUS;
            }

            return null;
        }
    }
}
