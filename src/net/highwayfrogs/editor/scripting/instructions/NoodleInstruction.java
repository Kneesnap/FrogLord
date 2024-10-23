package net.highwayfrogs.editor.scripting.instructions;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.highwayfrogs.editor.scripting.NoodleScript;
import net.highwayfrogs.editor.scripting.runtime.NoodlePrimitive;
import net.highwayfrogs.editor.scripting.runtime.NoodleThread;
import net.highwayfrogs.editor.scripting.tracking.NoodleCodeLocation;

import java.util.Map.Entry;

/**
 * Represents a Noodle bytecode instruction.
 */
@Getter
@AllArgsConstructor
public abstract class NoodleInstruction {
    private final NoodleInstructionType instructionType;
    private NoodleCodeLocation codeLocation;

    public NoodleInstruction(NoodleInstructionType type) {
        this.instructionType = type;
    }

    /**
     * Gets this instruction in a textual form.
     */
    public void toString(StringBuilder builder, NoodleScript script) {
        builder.append(this.instructionType.name());
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        this.toString(builder, null);
        return builder.toString();
    }

    /**
     * Execute this instruction.
     * @param thread The thread to execute as.
     */
    public abstract void execute(NoodleThread<? extends NoodleScript> thread);

    /**
     * Gets the display string for the label at a given index.
     * @param script The script to get the label name from.
     * @param position The position to get the label name from.
     * @return labelName
     */
    protected static String getLabelName(NoodleScript script, int position) {
        if (script != null)
            for (Entry<String, Integer> labelEntry : script.getLabels().entrySet())
                if (labelEntry.getValue() == position)
                    return labelEntry.getKey();

        return "$" + position;
    }

    /**
     * Reduce the reference count of all supplied primitive object references.
     * The primary use-case is for reducing the ref counts of objects popped off the stack all at once.
     * This can cause the objects to become free'd.
     * @param primitives The primitives to free.
     */
    protected static void decreaseObjectRefs(NoodlePrimitive[] primitives) {
        if (primitives == null)
            return;

        for (int i = 0; i < primitives.length; i++)
            if (primitives[i] != null)
                primitives[i].tryDecreaseRefCount();
    }
}
