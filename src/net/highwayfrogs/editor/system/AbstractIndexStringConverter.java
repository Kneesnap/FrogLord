package net.highwayfrogs.editor.system;

import javafx.util.StringConverter;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.function.BiFunction;

/**
 * A StringConverter which gives the index too.
 * Created by Kneesnap on 1/26/2019.
 */
@AllArgsConstructor
public class AbstractIndexStringConverter<T> extends StringConverter<T> {
    private List<T> list;
    private BiFunction<Integer, T, String> function;

    @Override
    public String toString(T object) {
        return function.apply(list.indexOf(object), object);
    }

    @Override
    public T fromString(String string) {
        return null;
    }
}
