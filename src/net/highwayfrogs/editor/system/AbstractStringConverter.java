package net.highwayfrogs.editor.system;

import javafx.util.StringConverter;
import lombok.AllArgsConstructor;

import java.util.function.Function;

/**
 * A StringConverter.
 * Created by Kneesnap on 1/26/2019.
 */
@AllArgsConstructor
public class AbstractStringConverter<T> extends StringConverter<T> {
    private Function<T, String> function;

    @Override
    public String toString(T object) {
        return function.apply(object);
    }

    @Override
    public T fromString(String string) {
        return null;
    }
}
