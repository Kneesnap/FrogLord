package net.highwayfrogs.editor.scripting.runtime.templates.functions;

import net.highwayfrogs.editor.scripting.runtime.NoodlePrimitive;
import net.highwayfrogs.editor.scripting.runtime.NoodleThread;
import net.highwayfrogs.editor.scripting.runtime.templates.NoodleTemplateFunction;

import java.util.function.BiFunction;

/**
 * Represents a static function within a class template.
 * Created by Kneesnap on 10/23/2024.
 */
public abstract class NoodleStaticFunction<TObject> extends NoodleTemplateFunction<TObject> {
    public NoodleStaticFunction(String name, Class<TObject> objectClass, String... argumentNames) {
        super(name, objectClass, argumentNames);
    }

    @Override
    protected final NoodlePrimitive executeImpl(NoodleThread<?> thread, TObject thisRef, NoodlePrimitive[] args) {
        return this.executeImpl(thread, args);
    }

    /**
     * Executes the static template function.
     * @param thread The thread to execute under.
     * @param args The arguments to the function.
     * @return returnValue
     */
    protected abstract NoodlePrimitive executeImpl(NoodleThread<?> thread, NoodlePrimitive[] args);

    /**
     * Represents a static function which accepts a callback for its behavior, making it very easy to add them.
     * @param <TObject> the type of object created
     */
    public static class LazyNoodleStaticFunction<TObject> extends NoodleStaticFunction<TObject> {
        private final BiFunction<NoodleThread<?>, NoodlePrimitive[], NoodlePrimitive> handler;

        public LazyNoodleStaticFunction(String name, Class<TObject> objectClass, BiFunction<NoodleThread<?>, NoodlePrimitive[], NoodlePrimitive> handler, String... argumentNames) {
            super(name, objectClass, argumentNames);
            this.handler = handler;
        }

        @Override
        protected NoodlePrimitive executeImpl(NoodleThread<?> thread, NoodlePrimitive[] args) {
            return thread.getStack().pushPrimitive(this.handler.apply(thread, args));
        }
    }
}