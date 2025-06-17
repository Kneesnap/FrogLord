package net.highwayfrogs.editor.games.sony;

import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.games.sony.shared.mwd.WADFile;
import net.highwayfrogs.editor.games.sony.shared.mwd.WADFile.WADEntry;
import net.highwayfrogs.editor.games.sony.shared.mwd.mwi.ISCFileDefinition;
import net.highwayfrogs.editor.games.sony.shared.mwd.mwi.MWIResourceEntry;
import net.highwayfrogs.editor.games.sony.shared.pp20.PP20Packer;
import net.highwayfrogs.editor.games.sony.shared.pp20.PP20Packer.PackResult;
import net.highwayfrogs.editor.games.sony.shared.ui.SCFileEditorUIController;
import net.highwayfrogs.editor.gui.DefaultFileUIController;
import net.highwayfrogs.editor.gui.GameUIController;
import net.highwayfrogs.editor.gui.components.CollectionViewComponent.ICollectionViewEntry;
import net.highwayfrogs.editor.gui.components.ProgressBarComponent;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.IPropertyListCreator;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;
import net.highwayfrogs.editor.utils.FXUtils;
import net.highwayfrogs.editor.utils.FileUtils;
import net.highwayfrogs.editor.utils.FileUtils.BrowserFileType;
import net.highwayfrogs.editor.utils.FileUtils.SavedFilePath;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.data.writer.ArrayReceiver;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.utils.logging.ILogger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

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
    private WADEntry wadFileEntry;

    public static final SavedFilePath SINGLE_FILE_IMPORT_PATH = new SavedFilePath("singleFileImportPath", "Choose the file to import.", BrowserFileType.ALL_FILES);
    public static final SavedFilePath SINGLE_FILE_EXPORT_PATH = new SavedFilePath("singleFileExportPath", "Choose the file to save the data as.", BrowserFileType.ALL_FILES);

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

    @Override
    public void setupRightClickMenuItems(ContextMenu contextMenu) {
        String fileName = getFileDisplayName();

        if (this.rawFileData != null) {
            MenuItem exportOriginalFile = new MenuItem("Export Original " + fileName);
            contextMenu.getItems().add(exportOriginalFile);
            exportOriginalFile.setOnAction(event -> askUserToSaveToFile(true));
        }

        MenuItem exportFile = new MenuItem("Export " + fileName);
        contextMenu.getItems().add(exportFile);
        exportFile.setOnAction(event -> askUserToSaveToFile(false));

        MenuItem importFile = new MenuItem("Import " + fileName);
        contextMenu.getItems().add(importFile);
        importFile.setOnAction(event -> askUserToImportFile());
    }

    /**
     * Ask the user where they would like to save this file, then saves it if the user gives a valid path.
     * @param original If true, the rawFileBytes will be exported instead of the current file contents
     */
    public void askUserToSaveToFile(boolean original) {
        File outputFile = FileUtils.askUserToSaveFile(getGameInstance(), SINGLE_FILE_EXPORT_PATH, getFileDisplayName(), true);
        if (outputFile != null)
            saveToFile(outputFile, original, true);
    }

    /**
     * Save the contents of the file to the file-system.
     * @param outputFile The file to save to
     * @param original If true, the rawFileBytes will be exported instead of the current file contents
     */
    public void saveToFile(File outputFile, boolean original, boolean showPopupOnError) {
        boolean success;
        if (original) {
            success = FileUtils.writeBytesToFile(getLogger(), outputFile, this.rawFileData, showPopupOnError);
        } else {
            success = writeDataToFile(getLogger(), outputFile, showPopupOnError);
        }

        if (success)
            getLogger().info("Exported '%s'. (%s Version)", getFileDisplayName(), original ? "Original Raw" : "Current");
    }

    /**
     * Gets the wad file which contains this file, if there is one.
     */
    public WADFile getParentWadFile() {
        return this.wadFileEntry != null ? this.wadFileEntry.getWadFile() : null;
    }

    /**
     * Performs the default UI action for interacting with this file.
     * Generally this means opening more advanced UI such as a 3D preview.
     */
    public void performDefaultUIAction() {
        GameUIController<?> uiController = makeEditorUI();
        if (uiController != null) {
            getGameInstance().getMainMenuController().showEditor(uiController);
            if (uiController instanceof SCFileEditorUIController<?, ?>)
                ((SCFileEditorUIController<?, ?>) uiController).setParentWadFile(getParentWadFile());
        } else {
            FXUtils.makePopUp("There is no editor available for " + Utils.getSimpleName(this), AlertType.ERROR);
        }
    }

    /**
     * Ask the user to provide a file they'd like to replace the selected file with, then imports it if valid.
     */
    public void askUserToImportFile() {
        File inputFile = FileUtils.askUserToOpenFile(getGameInstance(), SINGLE_FILE_IMPORT_PATH);
        if (inputFile != null)
            importFile(inputFile);
    }

    /**
     * Imports the given file.
     * @param inputFile the file to import over the existing file.
     */
    public void importFile(File inputFile) {
        if (inputFile == null)
            throw new NullPointerException("inputFile");

        byte[] rawFileBytes;
        try {
            rawFileBytes = Files.readAllBytes(inputFile.toPath());
        } catch (IOException ex) {
            Utils.handleError(getLogger(), ex, true, "Failed to read contents of '%s'.", inputFile.getName());
            return;
        }

        if (getArchive().replaceFile(inputFile.getName(), rawFileBytes, getIndexEntry(), this, true) == null)
            FXUtils.makePopUp("An error was encountered while reading the replacement file.", AlertType.ERROR);
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
        if (mwiEntry != null) {
            propertyList.add("File Type ID", mwiEntry.getTypeId());
            propertyList.add("Compressed? (PP20)", mwiEntry.isCompressed());
        }

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
     * Writes the file contents as a binary data stream to the provided DataWriter.
     * @param writer The data writer to write the data to.
     * @param progressBar The progress bar to update, if there is one.
     */
    public void saveFile(DataWriter writer, ProgressBarComponent progressBar) {
        if (progressBar != null)
            progressBar.setStatusMessage("Saving '" + getFileDisplayName() + "'");
        long startTime = System.currentTimeMillis();
        long compressionTime;

        try {
            // Save the file contents to a byte array.
            ArrayReceiver receiver = new ArrayReceiver();
            this.save(new DataWriter(receiver));

            // Potentially compress the saved byte array.
            byte[] fileBytes = receiver.toArray();

            PackResult packResult;
            if (getFileDefinition().isCompressed()) {
                long compressionStartTime = System.currentTimeMillis();
                packResult = PP20Packer.packData(fileBytes);
                compressionTime = System.currentTimeMillis() - compressionStartTime;
            } else {
                packResult = null;
                compressionTime = 0;
            }

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
        if (timeTaken >= 10) {
            // No need to include the file name, as that's already part of the logger info.
            if (compressionTime > 0) {
                getLogger().warning("Saving took %d ms. (Save Logic: %d ms, Compression: %d ms)", timeTaken, timeTaken - compressionTime, compressionTime);
            } else {
                getLogger().warning("Saving took %d ms.", timeTaken);
            }
        }
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