package fi.dy.masa.worldprimer.command.parser;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Stack;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.command.SyntaxErrorException;
import fi.dy.masa.worldprimer.command.handler.Expression;
import fi.dy.masa.worldprimer.command.parser.token.ArithmeticBinaryOperatorToken;
import fi.dy.masa.worldprimer.command.parser.token.BooleanBinaryOperatorToken;
import fi.dy.masa.worldprimer.command.parser.token.EqualityOperatorToken;
import fi.dy.masa.worldprimer.command.parser.token.NumericRelationOperatorToken;
import fi.dy.masa.worldprimer.command.parser.token.Token;
import fi.dy.masa.worldprimer.command.parser.token.TokenCategory;
import fi.dy.masa.worldprimer.command.parser.token.TokenType;
import fi.dy.masa.worldprimer.command.parser.token.ValueToken;
import fi.dy.masa.worldprimer.command.parser.value.BooleanValue;
import fi.dy.masa.worldprimer.command.parser.value.DoubleValue;
import fi.dy.masa.worldprimer.command.parser.value.IntValue;
import fi.dy.masa.worldprimer.command.substitution.BaseSubstitution;
import fi.dy.masa.worldprimer.command.substitution.CommandContext;

public class ExpressionParser
{
    protected final SubstitutionParser substitutionParser;

    public ExpressionParser(SubstitutionParser substitutionParser)
    {
        this.substitutionParser = substitutionParser;
    }

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
    public Expression parseAndReduceToExpression(StringReader reader) throws SyntaxErrorException
    {
        StringTokenizer tokenizer = new StringTokenizer(this.substitutionParser, reader);
        List<Token> tokens = this.parseToRpn(tokenizer::readNextToken);
        return tokens.isEmpty() == false ? new Expression(tokens, reader.getString()) : null;
    }

    public List<Token> parseAndReduceToRpn(StringTokenizer tokenizer) throws SyntaxErrorException
    {
        List<Token> parsedTokens = this.parseToRpn(tokenizer::readNextToken);
        return this.reduceRpn(parsedTokens);
    }

    public List<Token> parseAndReduceToRpn(List<Token> tokens) throws SyntaxErrorException
    {
        List<Token> parsedTokens = this.parseToRpn(tokens);
        return this.reduceRpn(parsedTokens);
    }

    public List<Token> parseToRpn(List<Token> tokens) throws SyntaxErrorException
    {
        Iterator<Token> it = tokens.iterator();
        Supplier<Optional<Token>> tokenSource = () -> it.hasNext() ? Optional.of(it.next()) : Optional.empty();
        return this.parseToRpn(tokenSource);
    }

    public List<Token> parseToRpn(Supplier<Optional<Token>> tokenSource) throws SyntaxErrorException
    {
        List<Token> outputQueue = new ArrayList<>();
        Stack<Token> operatorStack = new Stack<>();

        while (true)
        {
            Optional<Token> tokenOptional = tokenSource.get();

            if (tokenOptional.isPresent() == false)
            {
                break;
            }

            Token token = tokenOptional.get();

            if (token.getType() == TokenType.INVALID)
            {
                throw new SyntaxErrorException("Invalid token: '" + token.getOriginalString() + "'");
            }

            TokenType type = token.getType();
            TokenCategory category = type.getTokenCategory();

            if (category == TokenCategory.CONSTANT_VALUE ||
                category == TokenCategory.VARIABLE_VALUE)
            {
                outputQueue.add(token);
            }
            else if (type.getArgumentCount() == 2)
            {
                // while ((there is a operator at the top of the operator stack)
                //         and ((the operator at the top of the operator stack has greater precedence)
                //              or (the operator at the top of the operator stack has equal precedence and the token is left associative))
                //         and (the operator at the top of the operator stack is not a left parenthesis)):
                // pop operators from the operator stack onto the output queue.
                while (operatorStack.isEmpty() == false)
                {
                    Token lastOp = operatorStack.peek();
                    TokenType lastOpType = lastOp.getType();

                    // All the existing/supported binary operators are left-associative
                    if (lastOpType != TokenType.LEFT_PAREN &&
                        lastOpType.getPrecedence() <= type.getPrecedence())
                    {
                        outputQueue.add(operatorStack.pop());
                    }
                    else
                    {
                        break;
                    }
                }

                operatorStack.push(token);
            }
            else if (type == TokenType.LEFT_PAREN)
            {
                operatorStack.push(token);
            }
            else if (type == TokenType.RIGHT_PAREN)
            {
                while (operatorStack.isEmpty() == false &&
                       operatorStack.peek().getType() != TokenType.LEFT_PAREN)
                {
                    outputQueue.add(operatorStack.pop());
                }

                if (operatorStack.isEmpty() == false &&
                    operatorStack.peek().getType() == TokenType.LEFT_PAREN)
                {
                    operatorStack.pop();
                }
                // Mismatched parenthesis
                else
                {
                    throw new SyntaxErrorException("ExpressionParser#parseToRpn(): Mismatched/extra right parenthesis found");
                }
            }
        }

        while (operatorStack.isEmpty() == false)
        {
            Token op = operatorStack.pop();

            // Mismatched parenthesis
            if (op.getType().getTokenCategory() == TokenCategory.PRECEDENCE)
            {
                throw new SyntaxErrorException("ExpressionParser#parseToRpn(): Mismatched parenthesis in operator stack (size: {})", operatorStack.size());
            }

            outputQueue.add(op);
        }

        return outputQueue;
    }

    public List<Token> reduceRpn(List<Token> tokenQueue) throws SyntaxErrorException
    {
        return this.reduceRpn(tokenQueue, null);
    }

    public List<Token> reduceRpn(List<Token> tokenQueue, @Nullable CommandContext ctx) throws SyntaxErrorException
    {
        List<Token> reducedTokens = new ArrayList<>(tokenQueue);

        for (int i = 0; i < reducedTokens.size(); ++i)
        {
            Token operatorToken = reducedTokens.get(i);
            TokenType operatorTokenType = operatorToken.getType();

            // If there are constant values that can be computed,
            // then do those calculations and replace the operands and the
            // operator in the token queue with the calculated value.

            if (i >= 1 && operatorTokenType.getArgumentCount() == 1)
            {
                //System.out.printf("try unary reduce for op %s\n", operatorToken);
                Token valueToken1 = this.getSubstitutedValueToken(reducedTokens.get(i - 1), ctx);
                Token reducedToken = this.reduceUnaryValue(operatorToken, valueToken1);

                if (reducedToken != null)
                {
                    //System.out.printf("UNARY reducing '%s' using op '%s' to '%s'\n", valueToken1, operatorToken, reducedToken);
                    reducedTokens.set(i - 1, reducedToken);
                    reducedTokens.remove(i);
                    i -= 1; // continue from the position following the newly replaced value token
                }
            }
            else if (i >= 2 && operatorToken.getType().getArgumentCount() == 2)
            {
                //System.out.printf("try binary reduce for op %s\n", operatorToken);
                Token valueToken1 = this.getSubstitutedValueToken(reducedTokens.get(i - 2), ctx);
                Token valueToken2 = this.getSubstitutedValueToken(reducedTokens.get(i - 1), ctx);
                Token reducedToken = this.reduceBinaryValue(operatorToken, valueToken1, valueToken2);

                if (reducedToken != null)
                {
                    //System.out.printf("BINARY reducing '%s' and '%s' using op '%s' to '%s'\n", valueToken1, valueToken2, operatorToken, reducedToken);
                    reducedTokens.set(i - 2, reducedToken);
                    reducedTokens.remove(i);
                    reducedTokens.remove(i - 1);
                    i -= 2; // continue from the position following the newly replaced value token
                }
            }
        }

        return reducedTokens;
    }

    public Token getSubstitutedValueToken(Token valueToken, @Nullable CommandContext ctx)
    {
        if (ctx != null && valueToken.getType() == TokenType.VARIABLE)
        {
            String origString = valueToken.getOriginalString();
            String substitutionString = origString.substring(1, origString.length() - 1);
            BaseSubstitution sub = this.substitutionParser.getSubstitutionFor(substitutionString, true);

            if (sub != null)
            {
                String subStr = sub.getString(ctx);
                Token token = StringTokenizer.readToken(new StringReader(subStr), this.substitutionParser, null);
                return token != null ? token : valueToken;
            }
        }

        return valueToken;
    }

    @Nullable
    protected Token reduceUnaryValue(Token operatorToken, Token valueToken) throws SyntaxErrorException
    {
        TokenType operatorTokenType = operatorToken.getType();
        TokenType valueTokenType = valueToken.getType();

        if (valueTokenType.getTokenCategory() == TokenCategory.CONSTANT_VALUE)
        {
            if (operatorTokenType.getValueCategory() == valueTokenType.getValueCategory())
            {
                if (operatorTokenType == TokenType.OP_ARITH_UNARY_PLUS)
                {
                    return valueToken;
                }
                else if (operatorTokenType == TokenType.OP_ARITH_UNARY_MINUS)
                {
                    if (valueTokenType == TokenType.CONST_INT)
                    {
                        IntValue value = new IntValue(-(((IntValue) valueToken.getValue()).getValue()));
                        return new ValueToken(valueTokenType, valueToken.getOriginalString(), value);
                    }
                    else if (valueTokenType == TokenType.CONST_DOUBLE)
                    {
                        DoubleValue value = new DoubleValue(-(((DoubleValue) valueToken.getValue()).getValue()));
                        return new ValueToken(valueTokenType, valueToken.getOriginalString(), value);
                    }
                }
                else if (operatorTokenType == TokenType.OP_BOOLEAN_UNARY_NEG)
                {
                    if (valueTokenType == TokenType.CONST_BOOLEAN)
                    {
                        BooleanValue value = new BooleanValue(!(((BooleanValue) valueToken.getValue()).getValue()));
                        return new ValueToken(valueTokenType, valueToken.getOriginalString(), value);
                    }
                }
            }
            else
            {
                throw new SyntaxErrorException("The operator '" + operatorTokenType +
                                               "' can't be applied to the argument '" + valueTokenType + "'");
            }
        }

        return null;
    }

    @Nullable
    protected Token reduceBinaryValue(Token operatorToken, Token valueToken1, Token valueToken2)
    {
        TokenType operatorTokenType = operatorToken.getType();
        TokenType valueTokenType1 = valueToken1.getType();
        TokenType valueTokenType2 = valueToken2.getType();

        if (valueTokenType1.getTokenCategory() == TokenCategory.CONSTANT_VALUE &&
            valueTokenType2.getTokenCategory() == TokenCategory.CONSTANT_VALUE)
        {
            TokenCategory opTokenCategory = operatorTokenType.getTokenCategory();

            if (opTokenCategory == TokenCategory.OP_BOOL_BINARY)
            {
                return ((BooleanBinaryOperatorToken) operatorToken).getOperator().reduceTerms(valueToken1, valueToken2);
            }
            else if (opTokenCategory == TokenCategory.OP_ARITH_BINARY)
            {
                return ((ArithmeticBinaryOperatorToken) operatorToken).getOperator().reduceTerms(valueToken1, valueToken2);
            }
            else if (opTokenCategory == TokenCategory.OP_NUM_RELATION)
            {
                return ((NumericRelationOperatorToken) operatorToken).getOperator().reduceTerms(valueToken1, valueToken2);
            }
            else if (opTokenCategory == TokenCategory.OP_EQUALITY)
            {
                return ((EqualityOperatorToken) operatorToken).getOperator().reduceTerms(valueToken1, valueToken2);
            }
        }
        //System.out.printf("Didn't reduce: %s %s %s\n", valueToken1, operatorToken, valueToken2);

        return null;
    }
}
