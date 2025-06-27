package net.highwayfrogs.editor.scripting.instructions;

import lombok.Getter;
import net.highwayfrogs.editor.scripting.NoodleScript;
import net.highwayfrogs.editor.scripting.runtime.NoodlePrimitive;
import net.highwayfrogs.editor.scripting.runtime.NoodleThread;
import net.highwayfrogs.editor.scripting.tracking.NoodleCodeLocation;

/**
 * A Noodle instruction for creating an array instance.
 * Created 6/27/2025 by Kneesnap.
 */
public class NoodleInstructionCreateArray extends NoodleInstruction{
    @Getter private int elementCount;

    private static final NoodlePrimitive[] EMPTY_ARGS = new NoodlePrimitive[0];

    public NoodleInstructionCreateArray() {
        super(NoodleInstructionType.CREATE_ARRAY);
    }

    public NoodleInstructionCreateArray(NoodleCodeLocation codeLocation, int elementCount) {
        super(NoodleInstructionType.CREATE_ARRAY, codeLocation);
        this.elementCount = elementCount;
    }

    @Override
    public void toString(StringBuilder builder, NoodleScript script) {
        super.toString(builder, script);
        builder.append("(").append(this.elementCount).append(" elements)");
    }

    @Override
    public void execute(NoodleThread<? extends NoodleScript> thread) {
        NoodlePrimitive[] arguments = this.elementCount > 0 ? new NoodlePrimitive[this.elementCount] : EMPTY_ARGS;
        for (int i = arguments.length - 1; i >= 0; i--)
            arguments[i] = thread.getStack().popWithoutGC();

        thread.getStack().pushObject(arguments);
        decreaseObjectRefs(arguments);
    }
}
