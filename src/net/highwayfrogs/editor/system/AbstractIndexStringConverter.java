package net.highwayfrogs.editor.system;

import javafx.util.StringConverter;
import net.highwayfrogs.editor.utils.Utils;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;

/**
 * A StringConverter which gives the index too.
 * Created by Kneesnap on 1/26/2019.
 */
public class AbstractIndexStringConverter<T> extends StringConverter<T> {
    private final List<T> list;
    private final BiFunction<Integer, T, String> function;
    private final String nullDisplay;
    private final boolean functionHandlesNull;

    private AbstractIndexStringConverter(List<T> values, BiFunction<Integer, T, String> function, String nullDisplay, boolean functionHandlesNull) {
        this.list = values;
        this.function = function;
        this.nullDisplay = nullDisplay;
        this.functionHandlesNull = functionHandlesNull;
    }

    public AbstractIndexStringConverter(List<T> values, BiFunction<Integer, T, String> function) {
        this(values, function, "", false);
    }

    public AbstractIndexStringConverter(List<T> values,  BiFunction<Integer, T, String> function, String nullDisplay) {
        this(values, function, nullDisplay, false);
    }

    public AbstractIndexStringConverter(T[] values, BiFunction<Integer, T, String> function) {
        this(Arrays.asList(values), function, "", false);
    }

    public AbstractIndexStringConverter(T[] values,  BiFunction<Integer, T, String> function, String nullDisplay) {
        this(Arrays.asList(values), function, nullDisplay, false);
    }

    @Override
    public String toString(T object) {
        if (!this.functionHandlesNull && object == null)
            return this.nullDisplay;

        int index = this.list.indexOf(object);
        try {
            return this.function.apply(index, object);
        } catch (Throwable th) {
            Utils.handleError(null, th, false);
            return "<ERROR: " + Utils.getSimpleName(th) + "/" + index + "/" + object + ">";
        }
    }

    @Override
    public T fromString(String string) {
        return null;
    }
}