package net.highwayfrogs.editor.games.sony;

import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.packers.PP20Packer;
import net.highwayfrogs.editor.file.packers.PP20Packer.PackResult;
import net.highwayfrogs.editor.file.writer.ArrayReceiver;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.shared.mwd.WADFile;
import net.highwayfrogs.editor.games.sony.shared.mwd.mwi.ISCFileDefinition;
import net.highwayfrogs.editor.games.sony.shared.mwd.mwi.MWIResourceEntry;
import net.highwayfrogs.editor.games.sony.shared.ui.SCFileEditorUIController;
import net.highwayfrogs.editor.gui.DefaultFileUIController;
import net.highwayfrogs.editor.gui.GameUIController;
import net.highwayfrogs.editor.gui.components.CollectionViewComponent.ICollectionViewEntry;
import net.highwayfrogs.editor.gui.components.ProgressBarComponent;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.IPropertyListCreator;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;
import net.highwayfrogs.editor.utils.FXUtils;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.logging.ILogger;

/**
 * Represents a file (data corresponding to MWI entry or contents of a filesystem entity).
 * @param <TGameInstance> The type of game instance this file can be used in.
 * Created by Kneesnap on 9/8/2023.
 */
@Setter
@Getter
public abstract class SCGameFile<TGameInstance extends SCGameInstance> extends SCGameData<TGameInstance> implements ICollectionViewEntry, IPropertyListCreator {
    private byte[] rawFileData;
    private ISCFileDefinition fileDefinition;

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
    public ILogger getLogger() {
        return getFileDefinition().getLogger();
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
        MWIResourceEntry entry = getIndexEntry();
        if (entry != null) {
            return getFileDisplayName() + " [" + entry.getResourceId() + "]";
        } else {
            return getFileDisplayName();
        }
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
            FXUtils.makePopUp("There is no editor available for " + Utils.getSimpleName(this), AlertType.ERROR);
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
        MWIResourceEntry mwiEntry = getIndexEntry();
        if (mwiEntry != null)
            propertyList.add("File Type ID", mwiEntry.getTypeId());
        if (getFileDefinition().hasFullFilePath())
            propertyList.add("File Path", getFileDefinition().getFullFilePath());

        return propertyList;
    }

    /**
     * Gets the file entry of this file from the MWI.
     * This is designed to always work, even during the load process.
     * @return fileEntry
     */
    public MWIResourceEntry getIndexEntry() {
        return getFileDefinition().getIndexEntry();
    }

    /**
     * Gets the file resource ID, if it has one.
     */
    public int getFileResourceId() {
        MWIResourceEntry mwiEntry = getIndexEntry();
        if (mwiEntry == null)
            throw new RuntimeException("'" + getFileDisplayName() + "' does not have an MWI Entry, therefore it has no resource ID!");

        return mwiEntry.getResourceId();
    }

    /**
     * Gets the display name of this file.
     */
    public String getFileDisplayName() {
        return getFileDefinition().getDisplayName();
    }

    /**
     * Export this file in a non-Frogger format.
     */
    public void exportAlternateFormat() {
        getLogger().warning("The file (" + getClass().getSimpleName() + ") does not have an alternate file-type it can export as.");
    }

    /**
     * Writes the file contents as a binary data stream to the provided DataWriter.
     * @param writer The data writer to write the data to.
     * @param progressBar The progress bar to update, if there is one.
     */
    public void saveFile(DataWriter writer, ProgressBarComponent progressBar) {
        if (progressBar != null)
            progressBar.setStatusMessage("Saving '" + getFileDisplayName() + "'");
        long startTime = System.currentTimeMillis();

        try {
            // Save the file contents to a byte array.
            ArrayReceiver receiver = new ArrayReceiver();
            this.save(new DataWriter(receiver));

            // Potentially compress the saved byte array.
            byte[] fileBytes = receiver.toArray();
            PackResult packResult = getFileDefinition().isCompressed() ? PP20Packer.packData(fileBytes) : null;

            MWIResourceEntry mwiEntry = getIndexEntry();
            if (mwiEntry != null)
                mwiEntry.onSaveData(fileBytes, packResult);

            writer.writeBytes(packResult != null ? packResult.getPackedBytes() : fileBytes);
        } catch (Throwable th) {
            Utils.handleError(getLogger(), th, true, "Failed to save file '%s' to MWD.", getFileDisplayName());
            return;
        }

        // Report timing.
        long endTime = System.currentTimeMillis();
        if (progressBar != null)
            progressBar.addCompletedProgress(1);
        long timeTaken = (endTime - startTime);
        if (timeTaken >= 10)
            getLogger().warning("Saving the file '" + getFileDisplayName() + "' took " + timeTaken + " ms.");
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
            FXMLLoader templateLoader = FXUtils.getFXMLTemplateLoader(gameInstance, template);
            GameUIController.loadController(gameInstance, templateLoader, controller);
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