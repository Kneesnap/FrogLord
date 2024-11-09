package net.highwayfrogs.editor.utils.fx.wrapper;

import javafx.scene.control.ListCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import net.highwayfrogs.editor.utils.Utils;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * A ListCell which gives the index too.
 * Created by Kneesnap on 11/8/2024.
 */
public class LazyFXListCell<T> extends ListCell<T> {
    private final Function<T, String> withoutIndexTextHandler;
    private final BiFunction<T, Integer, String> withIndexTextHandler;
    private final Function<T, Image> withoutIndexGraphicHandler;
    private final BiFunction<T, Integer, Image> withIndexGraphicHandler;
    private final String nullDisplay;

    private LazyFXListCell(Function<T, String> withoutIndexTextHandler,
                           BiFunction<T, Integer, String> withIndexTextHandler,
                           Function<T, Image> withoutIndexGraphicHandler,
                           BiFunction<T, Integer, Image> withIndexGraphicHandler,
                           String nullDisplay) {
        this.withoutIndexTextHandler = withoutIndexTextHandler;
        this.withIndexTextHandler = withIndexTextHandler;
        this.withoutIndexGraphicHandler = withoutIndexGraphicHandler;
        this.withIndexGraphicHandler = withIndexGraphicHandler;
        this.nullDisplay = nullDisplay;
    }

    public LazyFXListCell(Function<T, String> textResolver) {
        this(textResolver, (String) null);
    }

    public LazyFXListCell(Function<T, String> textResolver, String nullDisplay) {
        this(textResolver, null, null, null, nullDisplay);
    }

    public LazyFXListCell(BiFunction<T, Integer, String> textResolver) {
        this(textResolver, (String) null);
    }

    public LazyFXListCell(BiFunction<T, Integer, String> textResolver, String nullDisplay) {
        this(null, textResolver, null, null, nullDisplay);
    }

    public LazyFXListCell(Function<T, String> textResolver, Function<T, Image> imageResolver) {
        this(textResolver, imageResolver, null);
    }

    public LazyFXListCell(Function<T, String> textResolver, Function<T, Image> imageResolver, String nullDisplay) {
        this(textResolver, null, imageResolver, null, nullDisplay);
    }

    public LazyFXListCell(BiFunction<T, Integer, String> textResolver, BiFunction<T, Integer, Image> imageResolver) {
        this(textResolver, imageResolver, null);
    }

    public LazyFXListCell(BiFunction<T, Integer, String> textResolver, BiFunction<T, Integer, Image> imageResolver, String nullDisplay) {
        this(null, textResolver, null, imageResolver, nullDisplay);
    }

    @Override
    public void updateItem(T selection, boolean empty) {
        super.updateItem(selection, empty);

        // Apply icon.
        Image iconImage;
        if (this.withIndexGraphicHandler != null && selection != null) {
            iconImage = this.withIndexGraphicHandler.apply(selection, getIndex());
        } else if (this.withoutIndexGraphicHandler != null && selection != null) {
            iconImage = this.withoutIndexGraphicHandler.apply(selection);
        } else {
            iconImage = null;
        }
        setGraphic(iconImage != null ? new ImageView(iconImage) : null);

        // Update text.
        String applyText;
        try {
            if (selection == null && this.nullDisplay != null) {
                applyText = this.nullDisplay;
            } else if (this.withIndexTextHandler != null) {
                applyText = this.withIndexTextHandler.apply(selection, getIndex());
            } else if (this.withoutIndexTextHandler != null) {
                applyText = this.withoutIndexTextHandler.apply(selection);
            } else {
                applyText = "null";
            }
        } catch (Throwable th) {
            Utils.handleError(null, th, false);
            applyText = "<ERROR: " + Utils.getSimpleName(th) + "/" + getIndex() + "/" + selection + ">";
        }
        
        setText(applyText);
    }
}
