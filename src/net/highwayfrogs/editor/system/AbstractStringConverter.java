package net.highwayfrogs.editor.system;

import javafx.util.StringConverter;
import lombok.RequiredArgsConstructor;
import net.highwayfrogs.editor.utils.Utils;

import java.util.function.Function;

/**
 * A StringConverter.
 * @deprecated Use {@code LazyFXListCell} instead with {@code FXUtils.applyComboBoxDisplaySettings}.
 * Created by Kneesnap on 1/26/2019.
 */
@Deprecated
@RequiredArgsConstructor
public class AbstractStringConverter<T> extends StringConverter<T> {
    private final Function<T, String> function;
    private final String nullDisplay;
    private final boolean functionHandlesNull;

    public AbstractStringConverter(Function<T, String> function) {
        this(function, "", false);
    }

    public AbstractStringConverter(Function<T, String> function, String nullDisplay) {
        this(function, nullDisplay, false);
    }

    public AbstractStringConverter(Function<T, String> function, boolean functionHandlesNull) {
        this(function, "", functionHandlesNull);
    }

    @Override
    public String toString(T object) {
        if (!this.functionHandlesNull && object == null)
            return this.nullDisplay;

        try {
            return this.function.apply(object);
        } catch (Throwable th) {
            Utils.handleError(null, th, false);
            return "<ERROR: " + Utils.getSimpleName(th) + ">";
        }
    }

    @Override
    public T fromString(String string) {
        return null;
    }
}