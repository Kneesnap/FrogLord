package net.highwayfrogs.editor.scripting;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.scripting.compiler.NoodleCallHolder.INoodleCallable;

import java.util.List;

/**
 * Represents a function defined in a script.
 */
@Getter
public class NoodleScriptFunction implements INoodleCallable {
    private final NoodleScript script;
    private final String name;
    @Setter private int startAddress;
    @Setter private int endAddress;
    private final List<String> argumentNames;

    public NoodleScriptFunction(NoodleScript script, String functionName, List<String> argumentNames) {
        this.script = script;
        this.name = functionName;
        this.argumentNames = argumentNames;
    }

    /**
     * Test if an instruction's address is part of this function.
     * @param instructionAddress The instruction address to test.
     * @return functionContainsInstruction
     */
    public boolean contains(int instructionAddress) {
        return instructionAddress >= this.startAddress && instructionAddress <= this.endAddress;
    }

    /**
     * Gets the number of arguments this function accepts.
     */
    public int getArgumentCount() {
        return this.argumentNames != null ? this.argumentNames.size() : 0;
    }

    /**
     * Append the function signature to a string builder.
     * @param builder The builder to append the signature to.
     */
    public void writeSignature(StringBuilder builder, boolean includeSpaces) {
        builder.append(this.name).append("(");
        for (int i = 0; i < getArgumentCount(); i++) {
            if (i > 0)
                builder.append(includeSpaces ? ", " : ",");
            builder.append(this.argumentNames.get(i));
        }

        builder.append(")");
    }

    /**
     * Gets the function signature as a string.
     */
    public String getSignature(boolean includeSpaces) {
        StringBuilder builder = new StringBuilder();
        this.writeSignature(builder, includeSpaces);
        return builder.toString();
    }

    @Override
    public String toString() {
        return this.getSignature(true);
    }
}