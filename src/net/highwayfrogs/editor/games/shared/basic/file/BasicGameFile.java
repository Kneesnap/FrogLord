package net.highwayfrogs.editor.games.shared.basic.file;

import javafx.fxml.FXMLLoader;
import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.games.generic.GameData;
import net.highwayfrogs.editor.games.shared.basic.BasicGameInstance;
import net.highwayfrogs.editor.games.shared.basic.file.definition.IGameFileDefinition;
import net.highwayfrogs.editor.games.shared.basic.ui.BasicFileEditorUIController;
import net.highwayfrogs.editor.gui.DefaultFileUIController;
import net.highwayfrogs.editor.gui.GameUIController;
import net.highwayfrogs.editor.gui.components.CollectionViewComponent.ICollectionViewEntry;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;
import net.highwayfrogs.editor.utils.DataSizeUnit;
import net.highwayfrogs.editor.utils.Utils;

import java.io.File;
import java.util.logging.Logger;

/**
 * Represents a file as used in a basic game instance.
 * Created by Kneesnap on 8/12/2024.
 */
public abstract class BasicGameFile<TGameInstance extends BasicGameInstance> extends GameData<TGameInstance> implements IBasicGameFile {
    @Getter private final IGameFileDefinition fileDefinition;
    @Getter @Setter private byte[] rawData;
    private Logger cachedLogger;


    @SuppressWarnings("unchecked")
    public BasicGameFile(IGameFileDefinition fileDefinition) {
        super((TGameInstance) fileDefinition.getGameInstance());
        this.fileDefinition = fileDefinition;
    }

    @Override
    public Logger getLogger() {
        if (this.cachedLogger == null)
            this.cachedLogger = Logger.getLogger(getLoggerString());

        return this.cachedLogger;
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
    public ICollectionViewEntry getCollectionViewParentEntry() {
        return null;
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
    public PropertyList addToPropertyList(PropertyList propertyList) {
        if (this.rawData != null)
            propertyList.add("Loaded File Size", DataSizeUnit.formatSize(this.rawData.length) + " (" + this.rawData.length + " bytes)");

        return propertyList;
    }

    /**
     * Creates an editor UI for the file.
     */
    public GameUIController<?> makeEditorUI() {
        return DefaultFileUIController.loadEditor(getGameInstance(), new DefaultFileUIController<>(getGameInstance(), Utils.getSimpleName(this), getCollectionViewIcon()), this);
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
            FXMLLoader templateLoader = Utils.getFXMLTemplateLoader(gameInstance, template);
            GameUIController.loadController(gameInstance, templateLoader, controller);
            controller.setTargetFile(fileToEdit);
        } catch (Throwable th) {
            Utils.handleError(fileToEdit.getLogger(), th, true, "Failed to create editor UI.");
        }

        return controller;
    }
}