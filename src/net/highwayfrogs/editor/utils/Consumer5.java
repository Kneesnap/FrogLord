package net.highwayfrogs.editor.utils;

/**
 * A consumer that accepts five values.
 * Created by Kneesnap on 9/28/2024.
 */
@FunctionalInterface
public interface Consumer5<T, U, V, W, X> {
    /**
     * Performs this operation on the given arguments.
     * @param t the first input argument
     * @param u the second input argument
     * @param v the third input argument
     * @param w the fourth input argument
     * @param x the fifth input argument
     */
    void accept(T t, U u, V v, W w, X x);
}