package net.highwayfrogs.editor.utils;

/**
 * A consumer that accepts five values.
 * Created by Kneesnap on 10/22/2024.
 */
@FunctionalInterface
public interface Consumer3<T, U, V> {
    /**
     * Performs this operation on the given arguments.
     * @param t the first input argument
     * @param u the second input argument
     * @param v the third input argument
     */
    void accept(T t, U u, V v);
}