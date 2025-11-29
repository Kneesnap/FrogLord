package net.highwayfrogs.editor.games.shared.basic.file;

import javafx.fxml.FXMLLoader;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.games.generic.data.GameData;
import net.highwayfrogs.editor.games.shared.basic.BasicGameInstance;
import net.highwayfrogs.editor.games.shared.basic.file.definition.IGameFileDefinition;
import net.highwayfrogs.editor.games.shared.basic.ui.BasicFileEditorUIController;
import net.highwayfrogs.editor.gui.DefaultFileUIController;
import net.highwayfrogs.editor.gui.GameUIController;
import net.highwayfrogs.editor.gui.components.propertylist.PropertyListNode;
import net.highwayfrogs.editor.utils.DataSizeUnit;
import net.highwayfrogs.editor.utils.FXUtils;
import net.highwayfrogs.editor.utils.FileUtils;
import net.highwayfrogs.editor.utils.FileUtils.BrowserFileType;
import net.highwayfrogs.editor.utils.FileUtils.SavedFilePath;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.logging.ILogger;
import net.highwayfrogs.editor.utils.logging.InstanceLogger.LazyInstanceLogger;

import java.io.File;

/**
 * Represents a file as used in a basic game instance.
 * Created by Kneesnap on 8/12/2024.
 */
@Getter
public abstract class BasicGameFile<TGameInstance extends BasicGameInstance> extends GameData<TGameInstance> implements IBasicGameFile {
    private final IGameFileDefinition fileDefinition;
    @Setter private byte[] rawData;

    private static final SavedFilePath CHUNK_FILE_PATH = new SavedFilePath("rawFileDataPath", "Please select the file to export the raw data as.", BrowserFileType.ALL_FILES);


    @SuppressWarnings("unchecked")
    public BasicGameFile(IGameFileDefinition fileDefinition) {
        super((TGameInstance) fileDefinition.getGameInstance());
        this.fileDefinition = fileDefinition;
    }

    @Override
    public ILogger getLogger() {
        return new LazyInstanceLogger(getGameInstance(), BasicGameFile::getLoggerString, this);
    }

    /**
     * Gets the logger string used to build the logger object.
     */
    public String getLoggerString() {
        return getFullDisplayName();
    }

    /**
     * Gets the displayed name of the file.
     * @return fileDisplayName
     */
    public String getDisplayName() {
        if (this.fileDefinition == null)
            throw new RuntimeException("Cannot getFileDisplayName() as the fileDefinition is null!");

        return this.fileDefinition.getFileName();
    }

    /**
     * Gets the full name of the file.
     * @return fullFileDisplayName
     */
    public String getFullDisplayName() {
        if (this.fileDefinition == null)
            throw new RuntimeException("Cannot getFullFilePath() as the fileDefinition is null!");

        return this.fileDefinition.getFullFilePath();
    }

    /**
     * Exports the file to the given folder. This export is usually not the form in which the data is originally stored.
     * This is often used for debugging purposes, or to get files in some standard format like .bmp.
     * @param exportFolder the root export folder
     */
    public void export(File exportFolder) {
        // Skip export by default.
    }

    @Override
    public String getCollectionViewDisplayName() {
        return getFileDefinition().getFileName();
    }

    @Override
    public String getCollectionViewDisplayStyle() {
        return null;
    }

    @Override
    public void setupRightClickMenuItems(ContextMenu contextMenu) {
        MenuItem exportChunkItem = new MenuItem("Export Raw Data");
        contextMenu.getItems().add(exportChunkItem);
        exportChunkItem.setOnAction(event -> tryExport());
    }

    private void tryExport() {
        File outputFile = FileUtils.askUserToSaveFile(getGameInstance(), CHUNK_FILE_PATH, getFileDefinition().getFileName(), true);
        if (outputFile != null && this.rawData != null)
            FileUtils.writeBytesToFile(getLogger(), outputFile, this.rawData, true);
    }

    @Override
    public void addToPropertyList(PropertyListNode propertyList) {
        if (this.rawData != null)
            propertyList.add("Loaded File Size", DataSizeUnit.formatSize(this.rawData.length) + " (" + this.rawData.length + " bytes)");
    }

    /**
     * Creates an editor UI for the file.
     */
    public GameUIController<?> makeEditorUI() {
        return DefaultFileUIController.loadEditor(getGameInstance(), new DefaultFileUIController<>(getGameInstance(), Utils.getSimpleName(this), getCollectionViewIcon()), this);
    }

    /**
     * Handles when the file is double-clicked in most UI situations.
     * The default behavior is to do nothing.
     */
    public void handleDoubleClick() {
        tryExport();
    }

    /**
     * Loads a GameFile editor.
     * @param gameInstance the game instance to create the editor for
     * @param controller the controller to control the GUI
     * @param template the gui layout template
     * @param fileToEdit the file to edit
     * @return editor
     */
    public static <TGameFile extends BasicGameFile<?>, TGameInstance extends BasicGameInstance, TUIController extends BasicFileEditorUIController<TGameFile, TGameInstance>> TUIController loadEditor(TGameInstance gameInstance, String template, TUIController controller, TGameFile fileToEdit) {
        try {
            FXMLLoader templateLoader = FXUtils.getFXMLTemplateLoader(gameInstance, template);
            GameUIController.loadController(gameInstance, templateLoader, controller);
            controller.setTargetFile(fileToEdit);
        } catch (Throwable th) {
            Utils.handleError(fileToEdit.getLogger(), th, true, "Failed to create editor UI.");
        }

        return controller;
    }
}