package net.highwayfrogs.editor.utils.objects;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * A representation of a single string parsed command-line style.
 * For example: "test --flag value" will be parsed with one argument "test", with a single optional argument named "flag", holding a value of "value".
 * There are two representations of the arguments in this file, the "real", and the "user".
 * The "user" representation is used for reading and writing, and represents temporary data, such as the arguments the user has not yet processed.
 * Created by Kneesnap on 10/29/2024.
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class OptionalArguments {
    @NonNull private final List<StringNode> orderedArguments;
    @NonNull private final Map<String, StringNode> namedArguments;
    @NonNull private final List<String> namedArgumentsOrder;
    private Map<String, StringNode> remainingNamedArgumentsUser; // Represents optional arguments with user modifications performed.
    private int orderedArgumentReaderIndex;

    public OptionalArguments() {
        this(new ArrayList<>(), new HashMap<>(), new ArrayList<>());
    }

    /**
     * Writes this object parse-able string format to the StringBuilder.
     * @param builder the builder to write the string to
     */
    public void toString(StringBuilder builder) {
        if (builder == null)
            throw new NullPointerException("builder");

        // Write ordered arguments.
        for (int i = 0; i < this.orderedArguments.size(); i++) {
            if (i > 0)
                builder.append(' ');

            StringNode node = this.orderedArguments.get(i);
            String literal = String.valueOf(node.getAsStringLiteral());
            builder.append(literal.length() > 0 ? literal : "\"\"");
        }

        // Write optional arguments (in order).
        for (int i = 0; i < this.namedArgumentsOrder.size(); i++) {
            String argName = this.namedArgumentsOrder.get(i);
            StringNode node = this.namedArguments.get(argName);

            if (i > 0 || (this.namedArgumentsOrder.size() > 0 && this.orderedArguments.size() > 0))
                builder.append(' ');

            builder.append("--").append(argName);
            String literal;
            if (node != null && ((literal = node.getAsStringLiteral()) == null || literal.length() > 0))
                builder.append(' ').append(literal);
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        toString(builder);
        return builder.toString();
    }

    /**
     * If all named arguments have no values, get them as a comma-separated string.
     */
    public String getNamedArgumentsAsCommaSeparatedString() {
        StringBuilder builder = new StringBuilder();
        getNamedArgumentsAsCommaSeparatedString(builder);
        return builder.toString();
    }

    /**
     * Writes this object parse-able string format to the StringBuilder.
     * @param builder the builder to write the string to
     */
    public void getNamedArgumentsAsCommaSeparatedString(StringBuilder builder) {
        if (builder == null)
            throw new NullPointerException("builder");

        // Write optional arguments (in order).
        for (int i = 0; i < this.namedArgumentsOrder.size(); i++) {
            String argName = this.namedArgumentsOrder.get(i);
            StringNode node = this.namedArguments.get(argName);

            if (i > 0)
                builder.append(", ");

            builder.append(argName);
            String literal;
            if (node != null && ((literal = node.getAsStringLiteral()) == null || literal.length() > 0))
                throw new RuntimeException("The named argument '" + argName + "' had a value of " + literal + ", making it unable to be represented in a comma-separated string!");
        }
    }

    /**
     * Gets the number of ordered arguments provided.
     */
    public int getOrderedArgumentCount() {
        return this.orderedArguments.size();
    }

    /**
     * Gets the remaining number of unread arguments.
     */
    public int getRemainingArgumentCount() {
        return this.orderedArguments.size() - this.orderedArgumentReaderIndex;
    }

    /**
     * Tests whether an ordered argument is present.
     * @param index The index of the argument to test for.
     * @return Whether the ordered argument is present.
     */
    public boolean has(int index) {
        return index >= 0 && index < this.orderedArguments.size();
    }

    /**
     * Gets a normal argument by its absolute index.
     * Throws an ArrayIndexOfOutBoundsException if there is no argument at the given index.
     * @param index The index of the argument to get.
     * @return argumentAtIndex
     */
    public StringNode get(int index) {
        if (index < 0 || index >= this.orderedArguments.size())
            throw new ArrayIndexOutOfBoundsException("Index " + index + " is outside the bounds of the array length " + this.orderedArguments.size() + ".");
        return this.orderedArguments.get(index);
    }

    /**
     * Creates the next ordered argument.
     * @return newNode
     */
    public StringNode createNext() {
        StringNode newNode = new StringNode();
        this.orderedArguments.add(newNode);
        return newNode;
    }

    /**
     * Returns true iff there is another unnamed argument available.
     */
    public boolean hasNext() {
        return this.orderedArguments.size() > this.orderedArgumentReaderIndex;
    }

    /**
     * Removes the next unnamed argument and returns it.
     * If there are no more unnamed arguments, an error is thrown.
     * @return The next unnamed argument value.
     */
    public StringNode useNext() {
        if (this.orderedArgumentReaderIndex >= this.orderedArguments.size())
            throw new IllegalStateException("There are no more ordered/unnamed arguments.");

        return this.orderedArguments.get(this.orderedArgumentReaderIndex++);
    }

    /**
     * Pops the next unnamed argument and returns it for processing.
     * This is intended for use as a way to allow testing if there are any optional arguments remaining.
     * @return The optional argument value, if any. Null means there weren't any more unnamed arguments.
     */
    public StringNode useNextIfPresent() {
        return hasNext() ? useNext() : null;
    }

    /**
     * Tests whether an optional argument is present.
     * @param name The name of the optional argument to test for.
     * @return Whether the optional argument is present.
     */
    public boolean has(String name) {
        return this.namedArguments.get(name) != null;
    }

    /**
     * Gets the value of an optional argument by its name.
     * @param name The name of the optional argument.
     * @return The optional argument value, if any. Null means it was not specific, empty means the value provided was empty.
     */
    public StringNode get(String name) {
        return this.namedArguments.get(name);
    }

    /**
     * Gets or creates the value of an optional argument by its name.
     * @param name The name of the optional argument.
     * @return The optional argument value.
     */
    public StringNode getOrCreate(String name) {
        if (!this.namedArgumentsOrder.contains(name))
            this.namedArgumentsOrder.add(name);

        StringNode node = this.namedArguments.computeIfAbsent(name, key -> new StringNode(""));
        if (this.remainingNamedArgumentsUser != null)
            this.remainingNamedArgumentsUser.putIfAbsent(name, node);

        return node;
    }

    /**
     * Adds an optional argument.
     * @param name The name of the optional argument.
     * #pstsm node The optional argument value, if any.
     */
    public void put(String name, StringNode node) {
        this.namedArguments.put(name, node);
        if (!this.namedArgumentsOrder.contains(name))
            this.namedArgumentsOrder.add(name);
        if (this.remainingNamedArgumentsUser != null)
            this.remainingNamedArgumentsUser.put(name, node);
    }

    private void ensureUserNamedArgumentsExists() {
        if (this.remainingNamedArgumentsUser == null)
            this.remainingNamedArgumentsUser = new HashMap<>(this.namedArguments);
    }

    /**
     * Removes the value of an optional argument by its name.
     * This is often used as a way to allow systems to test if there were any unused arguments to a command.
     * @param name The name of the optional argument.
     * @return The optional argument value, if any. Null means it was not specific, empty means the value provided was empty.
     */
    public StringNode use(String name) {
        ensureUserNamedArgumentsExists();
        return this.remainingNamedArgumentsUser.remove(name);
    }

    /**
     * Removes the value of an optional argument by its name, and returns whether any optional argument existed.
     * @param name The name of the optional argument.
     * @return Whether such an argument existed.
     */
    public boolean useFlag(String name) {
        ensureUserNamedArgumentsExists();
        boolean flagExisted = this.remainingNamedArgumentsUser.containsKey(name);
        if (flagExisted)
            this.remainingNamedArgumentsUser.remove(name);

        return flagExisted;
    }

    /**
     * Clear currently tracked argument data.
     */
    public void clear() {
        this.orderedArguments.clear();
        this.namedArguments.clear();
        this.namedArgumentsOrder.clear();
        this.orderedArgumentReaderIndex = 0;
        this.remainingNamedArgumentsUser = null;
    }

    /**
     * Logs warnings for each optional argument
     * @param logger The logger to print the warnings to.
     */
    public void warnAboutUnusedArguments(Logger logger) {
        String fullLineStr = null;
        for (int i = this.orderedArgumentReaderIndex; i < this.orderedArguments.size(); i++) {
            StringNode value = this.orderedArguments.get(i);
            if (fullLineStr == null)
                fullLineStr = toString();

            logger.warning("Ignoring unsupported argument " + value.getAsStringLiteral() + " in '" + fullLineStr + "'.");
        }

        this.orderedArgumentReaderIndex = this.orderedArguments.size();

        // Optional arguments.
        ensureUserNamedArgumentsExists();
        for (int i = 0; i < this.namedArgumentsOrder.size(); i++) {
            String argumentName = this.namedArgumentsOrder.get(i);
            if (!this.remainingNamedArgumentsUser.containsKey(argumentName))
                continue;

            if (fullLineStr == null)
                fullLineStr = toString();

            logger.warning("Ignoring unsupported argument named '" + argumentName + "' in '" + fullLineStr + "'.");
        }

        this.remainingNamedArgumentsUser.clear();
    }

    /**
     * Read a comma-separated string, adding the strings seen as argument names.
     * @param input the string to read
     */
    public void readCommaSeparatedNamedArguments(String input) {
        if (input == null)
            throw new NullPointerException("input");

        String[] split = input.split(",\\s*");
        for (String argumentName : split)
            getOrCreate(argumentName).setAsString("", false);
    }

    /**
     * Read a comma-separated string, adding the strings seen as argument names.
     * @param input the string to read
     */
    public static OptionalArguments parseCommaSeparatedNamedArguments(String input) {
        if (input == null)
            throw new NullPointerException("input");

        OptionalArguments arguments = new OptionalArguments();
        arguments.readCommaSeparatedNamedArguments(input);
        return arguments;
    }

    /**
     * Parses an optional argument input string formatted in the form of "stuff --flag1 --flag2 test flag value".
     * Results:
     * Arguments[0] = "stuff"
     * Map["flag0"] = null
     * Map["flag1"] = ""
     * Map["flag2"] = "test flag value"
     * @param input the string to parse
     * @return argumentMap
     */
    public static OptionalArguments parse(String input) {
        if (input == null)
            throw new NullPointerException("input");

        SequentialStringReader reader = new SequentialStringReader(input);
        List<StringNode> orderedArguments = new ArrayList<>();
        Map<String, StringNode> optionalArguments = new HashMap<>();
        List<String> optionalArgumentsOrder = new ArrayList<>();

        boolean noMoreOrderedArguments = false;
        StringBuilder builder = new StringBuilder();
        while (reader.hasNext()) {
            char tempChar = reader.read();
            if (Character.isWhitespace(tempChar))
                continue;

            if (tempChar == '-' && reader.hasNext() && reader.peek() == '-') {
                noMoreOrderedArguments = true;

                // Read name.
                reader.skip(1); // Skip the second '-'.
                while (reader.hasNext() && !Character.isWhitespace(reader.peek()))
                    builder.append(reader.read());

                String argName = builder.toString();
                builder.setLength(0);
                if (argName.isEmpty())
                    throw new RuntimeException("Cannot parse the next argument from '" + reader.getPreviouslyReadPartOfString() + "'.");

                StringNode newValue = parseValue(reader, builder, true);
                StringNode existingValue = optionalArguments.put(argName, newValue);
                if (existingValue != null)
                    throw new RuntimeException("Optional argument '--" + argName + "' had already been specified!");

                optionalArgumentsOrder.add(argName);
            } else if (noMoreOrderedArguments) {
                reader.setIndex(reader.getIndex() - 1);
                throw new RuntimeException("Unexpected value '" + parseValue(reader, builder, false) + "' in '" + reader.getPreviouslyReadPartOfString() + "'.");
            } else {
                reader.setIndex(reader.getIndex() - 1);
                orderedArguments.add(parseValue(reader, builder, false));
            }
        }

        return new OptionalArguments(orderedArguments, optionalArguments, optionalArgumentsOrder);
    }

    private static StringNode parseValue(SequentialStringReader reader, StringBuilder result, boolean includeWhitespace) {
        boolean isEscape = false;
        boolean isQuotationString = false;
        boolean isQuotationOpen = false;
        char lastChar = ' '; // The last character is usually whitespace.
        while (reader.hasNext()) {
            char tempChar = reader.read();

            if (result.length() == 0 && tempChar == '"') { // First character.
                isQuotationString = isQuotationOpen = true;
            } else if (isQuotationOpen) {
                if (isEscape) {
                    isEscape = false;
                    if (tempChar == '\\' || tempChar == '"') {
                        result.append(tempChar);
                    } else if (tempChar == 'n') {
                        result.append('\n');
                    } else if (tempChar == 'r') {
                        result.append('\r');
                    } else if (tempChar == 't') {
                        result.append('\t');
                    } else {
                        String displayStr = result.toString();
                        if (displayStr.length() > 16)
                            displayStr = displayStr.substring(0, 16) + "...";

                        throw new RuntimeException("The argument beginning with '" + displayStr + "' contains an invalid escape sequence '\\" + tempChar + "'.");
                    }
                } else if (tempChar == '"') {
                    isQuotationOpen = false;
                    break;
                } else if (tempChar == '\\') {
                    isEscape = true;
                } else {
                    result.append(tempChar);
                }
            } else if (includeWhitespace && Character.isWhitespace(lastChar) && tempChar == '-' && reader.hasNext() && reader.peek() == '-') {
                reader.setIndex(reader.getIndex() - 1);
                break;
            } else if (Character.isWhitespace(tempChar)) {
                if (result.length() > 0) // If there's whitespace and this is at the start, just skip it.
                    break;
            } else {
                result.append(tempChar);
            }

            lastChar = tempChar;
        }

        if (isQuotationOpen) {
            String displayStr = result.toString();
            if (displayStr.length() > 16)
                displayStr = displayStr.substring(0, 16) + "...";

            throw new RuntimeException("The argument beginning with '" + displayStr + "' is never terminated!");
        }

        if (isQuotationString || !"null".contentEquals(result)) {
            String resultStr = result.toString();
            result.setLength(0);
            return new StringNode(resultStr, isQuotationString);
        } else {
            result.setLength(0);
            return new StringNode(null);
        }
    }
}