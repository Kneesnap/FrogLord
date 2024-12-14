package net.highwayfrogs.editor.scripting.instructions;

import net.highwayfrogs.editor.scripting.NoodleScript;
import net.highwayfrogs.editor.scripting.runtime.NoodlePrimitive;
import net.highwayfrogs.editor.scripting.runtime.NoodleRuntimeException;
import net.highwayfrogs.editor.scripting.runtime.NoodleThread;
import net.highwayfrogs.editor.scripting.runtime.NoodleThreadStatus;
import net.highwayfrogs.editor.scripting.runtime.templates.NoodleObjectTemplate;
import net.highwayfrogs.editor.scripting.tracking.NoodleCodeLocation;
import net.highwayfrogs.editor.utils.Utils;

import java.util.function.BiFunction;

/**
 * Allows getting the value of a field on a template instance.
 */
public class NoodleInstructionPushField extends NoodleInstruction {
    private String fieldName;

    public NoodleInstructionPushField() {
        super(NoodleInstructionType.PUSHFIELD);
    }

    public NoodleInstructionPushField(NoodleCodeLocation codeLocation, String fieldName) {
        super(NoodleInstructionType.PUSHFIELD, codeLocation);
        this.fieldName = fieldName;
    }

    @Override
    public void toString(StringBuilder builder, NoodleScript script) {
        super.toString(builder, script);
        builder.append(" ").append(this.fieldName);
    }

    @Override
    public void execute(NoodleThread<? extends NoodleScript> thread) {
        NoodlePrimitive instanceObject = thread.getStack().popWithoutGC();
        if (instanceObject == null || instanceObject.isNull())
            throw new NoodleRuntimeException("The object is null. Cannot get field '%s' on a null object.", this.fieldName);
        if (!instanceObject.isObjectReference())
            throw new NoodleRuntimeException("The popped value was not an object. Cannot get field '%s' on [%s].", this.fieldName, instanceObject);

        Object objectRef = instanceObject.getObjectReference().getObject();

        // Find instance field.
        NoodleObjectTemplate<Object> template = thread.getHeap().getObjectTemplate(objectRef);
        if (template == null)
            throw new NoodleRuntimeException("The object '%s' did not have a Noodle template. Noodle tried to get the field %s on it.", Utils.getSimpleName(objectRef), this.fieldName);

        BiFunction<NoodleThread<?>, Object, NoodlePrimitive> getter = template.getGetter(this.fieldName);
        if (getter == null)
            throw new NoodleRuntimeException("Noodle object '%s' does not support getting the field '%s'.", template.getName(), this.fieldName);

        int oldStackSize = thread.getStack().size();
        NoodlePrimitive result;
        try {
            result = getter.apply(thread, objectRef);
        } catch (Throwable th) {
            throw new NoodleRuntimeException(th, "Encountered error while running field getter '%s.%s'.", template.getName(), this.fieldName);
        }

        if (oldStackSize == thread.getStack().size() && thread.getStatus() == NoodleThreadStatus.RUNNING)
            thread.getStack().pushPrimitive(result);

        // Remove old stack reference.
        try {
            instanceObject.tryDecreaseRefCount();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
