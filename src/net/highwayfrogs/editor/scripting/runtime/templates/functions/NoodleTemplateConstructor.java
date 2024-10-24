package net.highwayfrogs.editor.scripting.runtime.templates.functions;

import net.highwayfrogs.editor.scripting.runtime.NoodlePrimitive;
import net.highwayfrogs.editor.scripting.runtime.NoodleThread;
import net.highwayfrogs.editor.scripting.runtime.templates.NoodleObjectTemplate;

import java.util.function.BiFunction;

/**
 * Represents a template constructor.
 * Created by Kneesnap on 10/23/2024.
 */
public abstract class NoodleTemplateConstructor<TObject> extends NoodleStaticTemplateFunction<TObject> {
    public NoodleTemplateConstructor(Class<TObject> objectClass, String... argumentNames) {
        super(NoodleObjectTemplate.CONSTRUCTOR_FUNCTION_NAME, objectClass, argumentNames);
    }

    /**
     * Represents a template constructor which accepts a callback for object creation, leading to very easy object creation.
     * @param <TObject> the type of object created
     */
    public static class LazyNoodleTemplateConstructor<TObject> extends NoodleTemplateConstructor<TObject> {
        private final BiFunction<NoodleThread<?>, NoodlePrimitive[], TObject> lazyObjectCreator;

        public LazyNoodleTemplateConstructor(Class<TObject> objectClass, BiFunction<NoodleThread<?>, NoodlePrimitive[], TObject> lazyObjectCreator, String... argumentNames) {
            super(objectClass, argumentNames);
            this.lazyObjectCreator = lazyObjectCreator;
        }

        @Override
        protected NoodlePrimitive executeImpl(NoodleThread<?> thread, NoodlePrimitive[] args) {
            return thread.getStack().pushObject(this.lazyObjectCreator.apply(thread, args));
        }
    }
}
