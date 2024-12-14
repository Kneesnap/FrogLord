package net.highwayfrogs.editor.gui.components;

import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.TreeCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import net.highwayfrogs.editor.gui.components.CollectionViewComponent.ICollectionViewEntry;

/**
 * Applies collection view entry display properties to a JavaFX TreeCell.
 * Created by Kneesnap on 8/9/2024.
 */
public class CollectionViewEntryTreeCell<TViewEntry extends ICollectionViewEntry> extends TreeCell<TViewEntry> {
    @Override
    public void updateItem(TViewEntry viewEntry, boolean empty) {
        super.updateItem(viewEntry, empty);
        if (empty) {
            setGraphic(null);
            setStyle(null);
            setText(null);
            setOnContextMenuRequested(null);
            return;
        }

        // Apply icon.
        Image iconImage = viewEntry.getCollectionViewIcon();
        setGraphic(iconImage != null ? new ImageView(iconImage) : null);

        // Update text.
        setStyle(viewEntry.getCollectionViewDisplayStyle());
        setText(viewEntry.getCollectionViewDisplayName());

        // Setup the context menu right-click handler.
        setOnContextMenuRequested(evt -> {
            ContextMenu contextMenu = new ContextMenu();
            viewEntry.setupRightClickMenuItems(contextMenu);
            if (!contextMenu.getItems().isEmpty())
                contextMenu.show((Node) evt.getSource(), evt.getScreenX(), evt.getScreenY());
        });
    }
}