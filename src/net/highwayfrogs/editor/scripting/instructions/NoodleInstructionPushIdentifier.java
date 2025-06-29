package net.highwayfrogs.editor.scripting.instructions;

import lombok.Getter;
import net.highwayfrogs.editor.scripting.NoodleScript;
import net.highwayfrogs.editor.scripting.runtime.NoodlePrimitive;
import net.highwayfrogs.editor.scripting.runtime.NoodleRuntimeException;
import net.highwayfrogs.editor.scripting.runtime.NoodleThread;
import net.highwayfrogs.editor.scripting.runtime.templates.NoodleObjectTemplate;
import net.highwayfrogs.editor.scripting.tracking.NoodleCodeLocation;

import java.util.function.Function;

/**
 * This instruction pushes a named identifier to the stack.
 */
@Getter
public class NoodleInstructionPushIdentifier extends NoodleInstruction {
    private String variableName;

    public NoodleInstructionPushIdentifier() {
        super(NoodleInstructionType.PUSHVAR);
    }

    public NoodleInstructionPushIdentifier(NoodleCodeLocation codeLocation, String variableName) {
        super(NoodleInstructionType.PUSHVAR, codeLocation);
        this.variableName = variableName;
    }

    @Override
    public void toString(StringBuilder builder, NoodleScript script) {
        super.toString(builder, script);
        builder.append(" ").append(this.variableName);
    }

    @Override
    public void execute(NoodleThread<? extends NoodleScript> thread) {
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

            Function<NoodleThread<?>, NoodlePrimitive> getter = template.getStaticGetter(fieldName);
            if (getter == null)
                throw new NoodleRuntimeException("No getter for %s exists.", this.variableName);

            int oldStackSize = thread.getStack().size();
            NoodlePrimitive newPrimitive = getter.apply(thread);
            if (oldStackSize == thread.getStack().size())
                thread.getStack().pushPrimitive(newPrimitive);
        } else {
            thread.getStack().pushPrimitive(thread.getHeap().getVariable(this.variableName));
        }
    }
}
