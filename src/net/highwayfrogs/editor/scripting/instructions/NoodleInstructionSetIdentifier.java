package net.highwayfrogs.editor.scripting.instructions;

import net.highwayfrogs.editor.scripting.NoodleScript;
import net.highwayfrogs.editor.scripting.runtime.NoodlePrimitive;
import net.highwayfrogs.editor.scripting.runtime.NoodleRuntimeException;
import net.highwayfrogs.editor.scripting.runtime.NoodleThread;
import net.highwayfrogs.editor.scripting.runtime.templates.NoodleObjectTemplate;
import net.highwayfrogs.editor.scripting.tracking.NoodleCodeLocation;

import java.util.function.BiConsumer;

/**
 * This instruction sets the value of an identifier to a value popped from the stack.
 */
public class NoodleInstructionSetIdentifier extends NoodleInstruction {
    private String variableName;

    public NoodleInstructionSetIdentifier() {
        super(NoodleInstructionType.SETVAR);
    }

    public NoodleInstructionSetIdentifier(NoodleCodeLocation codeLocation, String variableName) {
        super(NoodleInstructionType.SETVAR, codeLocation);
        this.variableName = variableName;
    }

    @Override
    public void toString(StringBuilder builder, NoodleScript script) {
        super.toString(builder, script);
        builder.append(" ").append(this.variableName);
    }

    @Override
    public void execute(NoodleThread<? extends NoodleScript> thread) {
        NoodlePrimitive value = thread.getStack().popWithoutGC();

        // Determine static/non-static.
        int firstPeriodIndex = this.variableName.indexOf('.');
        int lastPeriodIndex = firstPeriodIndex >= 0 ? this.variableName.lastIndexOf('.') : -1;
        if (firstPeriodIndex != lastPeriodIndex)
            throw new NoodleRuntimeException("Illegal variableName: '%s'.", this.variableName);

        if (firstPeriodIndex >= 0) { // Static field accessor.
            String templateName = this.variableName.substring(0, firstPeriodIndex);
            String fieldName = this.variableName.substring(firstPeriodIndex + 1);
            NoodleObjectTemplate<?> template = thread.getEngine().getTemplateByName(templateName);
            if (template == null)
                throw new NoodleRuntimeException("Failed to find template named '%s'.", template);

            BiConsumer<NoodleThread<?>, NoodlePrimitive> setter = template.getStaticSetter(fieldName);
            if (setter == null)
                throw new NoodleRuntimeException("No setter for %s exists.", this.variableName);

            setter.accept(thread, value);
        } else {
            thread.getHeap().setVariable(this.variableName, value);
        }

        if (value != null) // Remove stack reference (Must run after we call set, so we don't garbage collect it before setting the variable, which should cause it to not GC.)
            value.tryDecreaseRefCount();
    }
}
