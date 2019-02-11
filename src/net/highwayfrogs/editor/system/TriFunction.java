package net.highwayfrogs.editor.system;

/**
 * Like BiFunction, but a TriFunction.
 * Created by Kneesnap on 2/11/2019.
 */
@FunctionalInterface
public interface TriFunction<A, B, C, R> {
    R apply(A var1, B var2, C var3);
}

