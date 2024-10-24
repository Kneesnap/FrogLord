package net.highwayfrogs.editor.scripting.runtime.templates;

import java.util.HashMap;
import java.util.Map;

/**
 * This is a very simple object template which wraps around a Java class.
 * Created by Kneesnap on 10/24/2024.
 */
public class NoodleWrapperTemplate<TWrapped> extends NoodleObjectTemplate<TWrapped> {
    private final String[] excludedMethods;
    private static final Map<Class<?>, NoodleWrapperTemplate<?>> cachedWrappers = new HashMap<>();

    public NoodleWrapperTemplate(Class<TWrapped> wrappedClass, String... excludedMethods) {
        super(wrappedClass, wrappedClass.getSimpleName());
        this.excludedMethods = excludedMethods;
    }

    @Override
    protected void onSetup() {
        // No methods are explicitly defined.
    }

    @Override
    protected void onSetupJvmWrapper() {
        getJvmWrapper().makeFullyAccessible(this.excludedMethods);
    }

    /**
     * Gets or creates a new cached wrapper template for the given class without any excluded methods.
     * @param wrappedClass the class to get/create the cached wrapper template for
     * @return cachedTemplate
     * @param <TWrapped> the wrapped type
     */
    @SuppressWarnings("unchecked")
    public static <TWrapped> NoodleWrapperTemplate<TWrapped> getCachedTemplate(Class<TWrapped> wrappedClass) {
        return (NoodleWrapperTemplate<TWrapped>) cachedWrappers.computeIfAbsent(wrappedClass, NoodleWrapperTemplate::new);
    }
}
