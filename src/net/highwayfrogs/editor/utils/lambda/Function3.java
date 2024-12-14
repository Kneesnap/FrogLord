package net.highwayfrogs.editor.utils.lambda;

/**
 * Represents a function interface accepting three arguments.
 * Created by Kneesnap on 10/22/2024.
 */
@FunctionalInterface
public interface Function3<T, U, V, R> {

    /**
     * Applies this function to the given arguments.
     *
     * @param t the first function argument
     * @param u the second function argument
     * @param v the third function argument
     * @return the function result
     */
    R apply(T t, U u, V v);
}
