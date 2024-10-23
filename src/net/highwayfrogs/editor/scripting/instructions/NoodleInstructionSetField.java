package net.highwayfrogs.editor.scripting.instructions;

import net.highwayfrogs.editor.scripting.NoodleScript;
import net.highwayfrogs.editor.scripting.runtime.NoodlePrimitive;
import net.highwayfrogs.editor.scripting.runtime.NoodleRuntimeException;
import net.highwayfrogs.editor.scripting.runtime.NoodleThread;
import net.highwayfrogs.editor.scripting.runtime.templates.NoodleObjectTemplate;
import net.highwayfrogs.editor.scripting.tracking.NoodleCodeLocation;
import net.highwayfrogs.editor.utils.Consumer3;
import net.highwayfrogs.editor.utils.Utils;

/**
 * Sets a field on an instanced template.
 */
public class NoodleInstructionSetField extends NoodleInstruction {
    private String fieldName;

    public NoodleInstructionSetField() {
        super(NoodleInstructionType.SETFIELD);
    }

    public NoodleInstructionSetField(NoodleCodeLocation codeLocation, String fieldName) {
        super(NoodleInstructionType.SETFIELD, codeLocation);
        this.fieldName = fieldName;
    }

    @Override
    public void toString(StringBuilder builder, NoodleScript script) {
        super.toString(builder, script);
        builder.append(" ").append(this.fieldName);
    }

    @Override
    public void execute(NoodleThread<? extends NoodleScript> thread) {
        NoodlePrimitive value = thread.getStack().popWithoutGC(); // The value is popped first since it's pushed after the object.
        NoodlePrimitive instanceObject = thread.getStack().popWithoutGC(); // The object is pushed before the value, so we pop it second.
        if (instanceObject == null || instanceObject.isNull())
            throw new NoodleRuntimeException("The object is null. Cannot set field '%s' on a null object.", this.fieldName);
        if (!instanceObject.isObjectReference())
            throw new NoodleRuntimeException("The popped value was not an object. Cannot set field '%s' on [%s].", this.fieldName, instanceObject);

        Object objectRef = instanceObject.getObjectReference().getObject();

        // Find instance field.
        NoodleObjectTemplate<Object> template = thread.getHeap().getObjectTemplate(objectRef);
        if (template == null)
            throw new NoodleRuntimeException("The object '%s' did not have a Noodle template. Noodle tried to set the field %s on it.", Utils.getSimpleName(objectRef), this.fieldName);

        Consumer3<NoodleThread<?>, Object, NoodlePrimitive> setter = template.getSetter(this.fieldName);
        if (setter == null)
            throw new NoodleRuntimeException("Noodle object '%s' does not support setting the field '%s'.", template.getName(), this.fieldName);

        try {
            setter.accept(thread, objectRef, value);
        } catch (Throwable th) {
            throw new NoodleRuntimeException(th, "Encountered error while running field setter '%s.%s' to '%s'.", template.getName(), this.fieldName, value);
        }

        // Remove references to the values popped from the stack.
        instanceObject.tryDecreaseRefCount();
        if (value != null)
            value.tryDecreaseRefCount();
    }
}
