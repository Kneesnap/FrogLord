package net.highwayfrogs.editor.games.sony;

import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.MWIFile.FileEntry;
import net.highwayfrogs.editor.file.WADFile;
import net.highwayfrogs.editor.games.sony.shared.ui.SCFileEditorUIController;
import net.highwayfrogs.editor.gui.DefaultFileUIController;
import net.highwayfrogs.editor.gui.GameUIController;
import net.highwayfrogs.editor.gui.components.CollectionViewComponent.ICollectionViewEntry;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.IPropertyListCreator;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;
import net.highwayfrogs.editor.utils.Utils;

import java.net.URL;
import java.util.logging.Logger;

/**
 * Represents a file (data corresponding to MWI entry or contents of a filesystem entity).
 * @param <TGameInstance> The type of game instance this file can be used in.
 * Created by Kneesnap on 9/8/2023.
 */
public abstract class SCGameFile<TGameInstance extends SCGameInstance> extends SCGameData<TGameInstance> implements ICollectionViewEntry, IPropertyListCreator {
    @Setter @Getter private byte[] rawFileData;
    public SCGameFile(TGameInstance instance) {
        super(instance);
    }

    /**
     * Warn if the end of the file is not reached.
     */
    public boolean warnIfEndNotReached() {
        return true;
    }

    @Override
    public Logger getLogger() {
        return getIndexEntry().getLogger();
    }

    /**
     * Get the icon which should appear for this file in the file list.
     */
    public abstract Image getCollectionViewIcon();

    /**
     * Makes a JavaFX UI Node which will be put into the preview pane for this file. Commonly used for editing.
     * @return editor
     */
    public abstract GameUIController<?> makeEditorUI();

    @Override
    public String getCollectionViewDisplayName() {
        return getFileDisplayName() + " [" + getIndexEntry().getResourceId() + "]";
    }

    @Override
    public ICollectionViewEntry getCollectionViewParentEntry() {
        return null;
    }

    @Override
    public String getCollectionViewDisplayStyle() {
        return null;
    }

    /**
     * WAD files are capable of containing any file.
     * This method is called when this file has the "Edit" button pressed from inside a WAD file.
     * Examples of how this can be used include: Opening a new UI area (Such as for viewing a 3D model) or replacing the WAD UI with the normal UI of the file created by makeEditor().
     * @param parent The wad file this is edited from.
     */
    public void handleWadEdit(WADFile parent) {
        GameUIController<?> uiController = makeEditorUI();
        if (uiController != null) {
            getGameInstance().getMainMenuController().showEditor(uiController);
            if (uiController instanceof SCFileEditorUIController<?, ?>)
                ((SCFileEditorUIController<?, ?>) uiController).setParentWadFile(parent);
        } else {
            Utils.makePopUp("There is no editor available for " + Utils.getSimpleName(this), AlertType.ERROR);
        }
    }

    /**
     * Called when this file is imported.
     * @param oldFile          The old file.
     * @param oldFileName      The old file's name.
     * @param importedFileName The name of the file just imported.
     */
    public void onImport(SCGameFile<?> oldFile, String oldFileName, String importedFileName) {

    }

    @Override
    public PropertyList addToPropertyList(PropertyList propertyList) {
        FileEntry fileEntry = getIndexEntry();
        propertyList.add("File Type ID", fileEntry.getTypeId());
        if (fileEntry.hasFilePath()) // Show path from MWI, not faked one.
            propertyList.add("File Path", fileEntry.getFilePath());

        return propertyList;
    }

    /**
     * Gets the file entry of this file from the MWI.
     * This is designed to always work, even during the load process.
     * @return fileEntry
     */
    public FileEntry getIndexEntry() {
        TGameInstance instance = getGameInstance();
        if (instance == null)
            throw new RuntimeException("The game instance is null.");

        FileEntry entry = instance.getFileEntriesByFileObjects().get(this);
        if (entry == null) // Shouldn't occur.
            throw new RuntimeException("The SCGameFile was not registered in the MWI entry mapping.");

        return entry;
    }

    /**
     * Gets the display name of this file.
     */
    public String getFileDisplayName() {
        return getIndexEntry().getDisplayName();
    }

    /**
     * Export this file in a non-Frogger format.
     */
    public void exportAlternateFormat(FileEntry entry) {
        getLogger().warning("The file (" + getClass().getSimpleName() + ") does not have an alternate file-type it can export as.");
    }

    /**
     * Loads a GameFile editor.
     * @param gameInstance the game instance to create the editor for
     * @param controller the controller to control the GUI
     * @param template the gui layout template
     * @param fileToEdit the file to edit
     * @return editor
     */
    public static <TGameInstance extends SCGameInstance, TGameFile extends SCGameFile<?>, TUIController extends SCFileEditorUIController<TGameInstance, TGameFile>> TUIController loadEditor(TGameInstance gameInstance, String template, TUIController controller, TGameFile fileToEdit) {
        try {
            URL templateUrl = Utils.getFXMLTemplateURL(gameInstance, template);
            GameUIController.loadController(gameInstance, templateUrl, controller);
            controller.setTargetFile(fileToEdit);
        } catch (Throwable th) {
            Utils.handleError(fileToEdit.getLogger(), th, true, "Failed to create editor UI.");
        }

        return controller;
    }

    /**
     * Loads a GameFile editor.
     * @param gameInstance the game instance to create the editor for
     * @param controller the controller to control the GUI
     * @param fileToEdit the file to edit
     * @return editor
     */
    public static <TGameInstance extends SCGameInstance, TGameFile extends SCGameFile<?>, TUIController extends DefaultFileUIController<TGameInstance, TGameFile>> TUIController loadEditor(TGameInstance gameInstance, TUIController controller, TGameFile fileToEdit) {
        return DefaultFileUIController.loadEditor(gameInstance, controller, fileToEdit);
    }

    /**
     * Loads a GameFile editor.
     * @param gameInstance the game instance to create the editor for
     * @param controller the controller to control the GUI
     * @param template the gui layout template
     * @param fileToEdit the file to edit
     * @return editor
     */
    public static <TGameInstance extends SCGameInstance, TGameFile extends SCGameFile<?>, TUIController extends DefaultFileUIController<TGameInstance, TGameFile>> TUIController loadEditor(TGameInstance gameInstance, String template, TUIController controller, TGameFile fileToEdit) {
        return DefaultFileUIController.loadEditor(gameInstance, template, controller, fileToEdit);
    }

    /**
     * Represents an SCGameFile which can be used by any SCGameInstance.
     */
    public static abstract class SCSharedGameFile extends SCGameFile<SCGameInstance> {
        public SCSharedGameFile(SCGameInstance instance) {
            super(instance);
        }
    }
}