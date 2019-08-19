package net.highwayfrogs.editor.utils;

/**
 * A consumer that accepts three values.
 * Created by Kneesnap on 8/16/2019.
 */
@FunctionalInterface
public interface TriConsumer<T, U, V> {

    /**
     * Performs this operation on the given arguments.
     * @param t the first input argument
     * @param u the second input argument
     * @param v the third input argument
     */
    void accept(T t, U u, V v);
}