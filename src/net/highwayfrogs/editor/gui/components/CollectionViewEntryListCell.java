package net.highwayfrogs.editor.gui.components;

import javafx.scene.control.ListCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import net.highwayfrogs.editor.gui.components.CollectionViewComponent.ICollectionViewEntry;

/**
 * Applies collection view entry display properties to a JavaFX ListCell.
 * Created by Kneesnap on 8/9/2024.
 */
public class CollectionViewEntryListCell<TViewEntry extends ICollectionViewEntry> extends ListCell<TViewEntry> {
    @Override
    public void updateItem(TViewEntry view, boolean empty) {
        super.updateItem(view, empty);
        if (empty) {
            setGraphic(null);
            setStyle(null);
            setText(null);
            return;
        }

        // Apply icon.
        Image iconImage = view.getCollectionViewIcon();
        setGraphic(iconImage != null ? new ImageView(iconImage) : null);

        // Update text.
        setStyle(view.getCollectionViewDisplayStyle());
        setText(view.getCollectionViewDisplayName());
    }
}