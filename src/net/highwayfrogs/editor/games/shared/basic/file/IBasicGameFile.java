package net.highwayfrogs.editor.games.shared.basic.file;

import net.highwayfrogs.editor.games.generic.data.IBinarySerializable;
import net.highwayfrogs.editor.games.generic.data.IGameObject;
import net.highwayfrogs.editor.games.shared.basic.file.definition.IGameFileDefinition;
import net.highwayfrogs.editor.gui.GameUIController;
import net.highwayfrogs.editor.gui.components.CollectionViewComponent.ICollectionViewEntry;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.IPropertyListCreator;

import java.io.File;

/**
 * Defines what a basic game file looks like.
 * Created by Kneesnap on 8/12/2024.
 */
public interface IBasicGameFile extends IGameObject, ICollectionViewEntry, IPropertyListCreator, IBinarySerializable {
    /**
     * Gets the file definition for this file.
     */
    IGameFileDefinition getFileDefinition();

    /**
     * Gets the displayed name of the file.
     * @return fileDisplayName
     */
    String getDisplayName();

    /**
     * Gets the full name of the file.
     * @return fullFileDisplayName
     */
    String getFullDisplayName();

    /**
     * Creates an editor UI for the file.
     * If null is returned, no editor UI will be appleid.
     */
    GameUIController<?> makeEditorUI();

    /**
     * Handles when the file is double-clicked in many UI situations.
     */
    void handleDoubleClick();

    /**
     * Exports the file to the given folder. This export is usually not the form in which the data is originally stored.
     * This is often used for debugging purposes, or to get files in some standard format like .bmp.
     * @param exportFolder the root export folder
     */
    void export(File exportFolder);
}