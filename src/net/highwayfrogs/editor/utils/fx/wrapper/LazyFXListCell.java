package net.highwayfrogs.editor.utils.fx.wrapper;

import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.ListCell;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ContextMenuEvent;
import net.highwayfrogs.editor.utils.FXUtils;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.lambda.TriConsumer;

import java.util.function.BiConsumer;
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
    private final EventHandler<? super ContextMenuEvent> contextMenuListener = this::setupContextMenu;
    private final String nullDisplay;
    private Function<T, Tooltip> withoutIndexTooltipHandler;
    private BiFunction<T, Integer, Tooltip> withIndexTooltipHandler;
    private Tooltip nullTooltip;
    private TriConsumer<ContextMenu, T, Integer> withIndexContextMenuHandler;
    private BiConsumer<ContextMenu, T> withoutIndexContextMenuHandler;
    private int forcedGraphicSize = DEFAULT_GRAPHIC_SIZE;

    private static final int DEFAULT_GRAPHIC_SIZE = 25;

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

    /**
     * Sets the handler for creating a toolTip without using the list item index.
     * @param withoutIndexTooltipHandler the handler to apply
     * @return this
     */
    public LazyFXListCell<T> setWithoutIndexTooltipHandler(Function<T, Tooltip> withoutIndexTooltipHandler) {
        this.withoutIndexTooltipHandler = withoutIndexTooltipHandler;
        return this;
    }

    /**
     * Sets the handler for creating a toolTip using the list item index.
     * @param withIndexTooltipHandler the handler to apply
     * @return this
     */
    public LazyFXListCell<T> setWithIndexTooltipHandler(BiFunction<T, Integer, Tooltip> withIndexTooltipHandler) {
        this.withIndexTooltipHandler = withIndexTooltipHandler;
        return this;
    }

    /**
     * Sets the handler for creating a ContextMenu without using the list item index.
     * @param withoutIndexContextMenuHandler the handler to apply
     * @return this
     */
    public LazyFXListCell<T> setWithoutIndexContextMenuHandler(BiConsumer<ContextMenu, T> withoutIndexContextMenuHandler) {
        this.withoutIndexContextMenuHandler = withoutIndexContextMenuHandler;
        return this;
    }

    /**
     * Sets the handler for creating a ContextMenu using the list item index.
     * @param withIndexContextMenuHandler the handler to apply
     * @return this
     */
    public LazyFXListCell<T> setWithIndexContextMenuHandler(TriConsumer<ContextMenu, T, Integer> withIndexContextMenuHandler) {
        this.withIndexContextMenuHandler = withIndexContextMenuHandler;
        return this;
    }

    /**
     * Sets the tooltip displayed for a cell with a null value.
     * @param nullTooltip the tooltip to apply
     * @return this
     */
    public LazyFXListCell<T> setNullTooltip(Tooltip nullTooltip) {
        this.nullTooltip = nullTooltip;
        return this;
    }

    /**
     * Sets the image dimensions displayed for a cell with a graphic.
     * A value less than zero will let JavaFX determine the size.
     * @param forcedGraphicSize the image dimensions to apply
     * @return this
     */
    public LazyFXListCell<T> setNullTooltip(int forcedGraphicSize) {
        this.forcedGraphicSize = forcedGraphicSize;
        return this;
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

        ImageView imageView = null;
        if (iconImage != null) {
            imageView = new ImageView(iconImage);
            if (this.forcedGraphicSize > 0) {
                imageView.setFitWidth(this.forcedGraphicSize);
                imageView.setFitHeight(this.forcedGraphicSize);
            }
        }

        setGraphic(imageView);

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

        // Update tooltip.
        Tooltip tooltip;
        try {
            if (selection == null) {
                tooltip = this.nullTooltip;
            } else if (this.withIndexTooltipHandler != null) {
                tooltip = this.withIndexTooltipHandler.apply(selection, getIndex());
            } else if (this.withoutIndexTooltipHandler != null) {
                tooltip = this.withoutIndexTooltipHandler.apply(selection);
            } else {
                tooltip = null;
            }
        } catch (Throwable th) {
            Utils.handleError(null, th, false);
            tooltip = FXUtils.createTooltip("<ERROR: " + Utils.getSimpleName(th) + "/" + getIndex() + "/" + selection + ">");
        }

        setTooltip(tooltip);

        // Setup the context menu right-click handler.
        if (this.withIndexContextMenuHandler != null || this.withoutIndexContextMenuHandler != null) {
            setOnContextMenuRequested(this.contextMenuListener);
        } else {
            setOnContextMenuRequested(null);
        }
    }

    private void setupContextMenu(ContextMenuEvent event) {
        ContextMenu contextMenu = new ContextMenu();

        try {
            if (this.withIndexContextMenuHandler != null) {
                this.withIndexContextMenuHandler.accept(contextMenu, getItem(), getIndex());
            } else if (this.withoutIndexContextMenuHandler != null) {
                this.withoutIndexContextMenuHandler.accept(contextMenu, getItem());
            } else {
                return;
            }
        } catch (Throwable th) {
            Utils.handleError(null, th, true, "Failed to setup right-click ContextMenu.");
            return;
        }

        FXUtils.disableMnemonicParsing(contextMenu);
        if (!contextMenu.getItems().isEmpty())
            contextMenu.show((Node) event.getSource(), event.getScreenX(), event.getScreenY());
    }
}
