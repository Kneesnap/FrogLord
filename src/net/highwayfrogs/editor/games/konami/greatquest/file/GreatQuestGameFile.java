package net.highwayfrogs.editor.games.konami.greatquest.file;

import javafx.fxml.FXMLLoader;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.reader.FileSource;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.file.writer.FileReceiver;
import net.highwayfrogs.editor.games.generic.data.GameData;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestInstance;
import net.highwayfrogs.editor.games.konami.greatquest.loading.kcLoadContext;
import net.highwayfrogs.editor.games.konami.greatquest.ui.GreatQuestFileEditorUIController;
import net.highwayfrogs.editor.gui.GameUIController;
import net.highwayfrogs.editor.gui.components.CollectionViewComponent.ICollectionViewEntry;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.IPropertyListCreator;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;
import net.highwayfrogs.editor.utils.FXUtils;
import net.highwayfrogs.editor.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Represents a game file in Frogger: The Great Quest.
 * Created by Kneesnap on 9/9/2024.
 */
public abstract class GreatQuestGameFile extends GameData<GreatQuestInstance> implements ICollectionViewEntry, IPropertyListCreator {
    private Logger cachedLogger;

    public GreatQuestGameFile(GreatQuestInstance instance) {
        super(instance);
    }

    @Override
    public Logger getLogger() {
        if (this.cachedLogger == null)
            this.cachedLogger = Logger.getLogger(getClass().getSimpleName() + "{" + getFileName() + "}");

        return this.cachedLogger;
    }

    /**
     * Gets the file name for this game file.
     */
    public abstract String getFileName();

    /**
     * Gets the file path for this file.
     */
    public abstract String getFilePath();

    @Override
    public String getCollectionViewDisplayName() {
        return getFileName();
    }

    @Override
    public String getCollectionViewDisplayStyle() {
        return null;
    }

    @Override
    public void setupRightClickMenuItems(ContextMenu contextMenu) {
        MenuItem exportFileData = new MenuItem("Export File");
        contextMenu.getItems().add(exportFileData);
        exportFileData.setOnAction(event -> {
            File outputFile = FXUtils.promptFileSave(getGameInstance(), "Please select the file to save '" + getFileName() + "' as...", getFileName(), "All Files", "*");
            if (outputFile != null)
                saveToFile(outputFile);
        });

        MenuItem importFileData = new MenuItem("Import File");
        contextMenu.getItems().add(importFileData);
        importFileData.setOnAction(event -> {
            File inputFile = FXUtils.promptFileOpen(getGameInstance(), "Please select the file to import as '" + getFileName() + "'...", "All Files", "*");
            if (inputFile != null) {
                try {
                    loadFromFile(inputFile);
                } catch (Throwable th) {
                    Utils.handleError(getLogger(), th, true, "Failed to load file '%s' as '%s'.", inputFile.getName(), getFileName());
                }
            }
        });
    }

    /**
     * Loads the file contents from the file.
     * NOTE: Importing files may break references to assets between files!
     * @param file the file to load the data from
     * @throws IOException thrown if the file cannot be read
     */
    public void loadFromFile(File file) throws IOException {
        if (file == null)
            throw new NullPointerException("file");

        DataReader reader = new DataReader(new FileSource(file));

        try {
            // TODO: PROBLEM!!! I think we need to clear tracking so afterLoad() can be valid to run.
            load(reader);
            // TODO: PROBLEM!!! We need to run the afterLoad hooks
        } catch (Throwable th) {
            throw new RuntimeException("Failed to load contents of file from '" + file.getName() + "'. This has the potential to break things in weird ways, so be careful!", th);
        }
    }

    /**
     * Saves the file contents to the file.
     * @param file the file to save the data to
     */
    public void saveToFile(File file) {
        if (file == null)
            throw new NullPointerException("file");

        DataWriter writer = new DataWriter(new FileReceiver(file));

        try {
            save(writer);
            writer.closeReceiver();
        } catch (Throwable th) {
            throw new RuntimeException("Failed to save contents of file '" + getFileName() + "' to '" + file.getName() + "'.", th);
        }
    }

    @Override
    public PropertyList addToPropertyList(PropertyList propertyList) {
        String fileName = getFileName();
        if (fileName != null)
            propertyList.add("File Name", fileName);

        String filePath = getFilePath();
        if (filePath != null)
            propertyList.add("File Path", filePath);

        return propertyList;
    }

    /**
     * The first method called after all files have been loaded.
     */
    public void afterLoad1(kcLoadContext context) {
        // Do nothing.
    }

    /**
     * The second method called after all files have been loaded.
     */
    public void afterLoad2(kcLoadContext context) {
        // Do nothing.
    }

    /**
     * Creates an editor UI for the file.
     */
    public abstract GameUIController<?> makeEditorUI();

    /**
     * Called when the file is double-clicked in the file list.
     */
    public void handleDoubleClick() {
        // Do nothing by default.
    }

    /**
     * Tests if this file has a file path assigned.
     * @return hasFilePath
     */
    public boolean hasFilePath() {
        return getFilePath() != null;
    }

    /**
     * Loads a GameFile editor.
     * @param gameInstance the game instance to create the editor for
     * @param controller the controller to control the GUI
     * @param template the gui layout template
     * @param fileToEdit the file to edit
     * @return editor
     */
    public static <TGameFile extends GreatQuestGameFile, TUIController extends GreatQuestFileEditorUIController<TGameFile>> TUIController loadEditor(GreatQuestInstance gameInstance, String template, TUIController controller, TGameFile fileToEdit) {
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
