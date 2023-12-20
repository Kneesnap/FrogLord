package net.highwayfrogs.editor.system;

/**
 * A consumer that accepts four values.
 * Created by Kneesnap on 9/23/2023.
 */
@FunctionalInterface
public interface QuadConsumer<T, U, V, W> {
    /**
     * Performs this operation on the given arguments.
     * @param t the first input argument
     * @param u the second input argument
     * @param v the third input argument
     * @param w the fourth input argument
     */
    void accept(T t, U u, V v, W w);
}