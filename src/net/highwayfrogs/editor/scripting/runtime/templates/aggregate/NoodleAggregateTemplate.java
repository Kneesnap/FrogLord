package net.highwayfrogs.editor.scripting.runtime.templates.aggregate;

import net.highwayfrogs.editor.scripting.NoodleScriptEngine;
import net.highwayfrogs.editor.scripting.runtime.NoodleObjectInstance;
import net.highwayfrogs.editor.scripting.runtime.NoodleThread;

/**
 * An aggregate template is a special kind of template.
 * It operates like an array, but it allows calling the methods of each as if they are a single unit.
 * For example, when we create NPC clones of all players in the cutscene, each player gets their own NPC.
 * However, this would be very annoying for chroniclers to have to iterate through / know they need to iterate through.
 * So, this system allows a single object to be used as an aggregate proxy for multiple underlying objects.
 * In the future, if we ever support arrays, we should allow the array accessor on this template, to handle conflict situations.
 * For example, if you have clones with different locations, using the "location" getter will result in an error.
 */
public class NoodleAggregateTemplate<TWrappedType> extends UnsavedAggregateTemplate<TWrappedType> {
    public static final NoodleAggregateTemplate<?> INSTANCE = new NoodleAggregateTemplate<>();

    public NoodleAggregateTemplate(NoodleScriptEngine engine) {
        super(engine, NoodleAggregateWrapper.class, "AggregateWrapper");
    }

    @Override
    public void onObjectAddToHeap(NoodleThread<?> thread, NoodleAggregateWrapper<TWrappedType> wrapper, NoodleObjectInstance instance) {
        super.onObjectAddToHeap(thread, wrapper, instance);
        wrapper.addValueHeapReferences();
    }

    @Override
    public void onObjectFree(NoodleThread<?> thread, NoodleAggregateWrapper<TWrappedType> wrapper, NoodleObjectInstance instance) {
        super.onObjectFree(thread, wrapper, instance);
        wrapper.removeValueHeapReferences();
    }
}