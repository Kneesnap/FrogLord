package net.highwayfrogs.editor.games.konami.greatquest.ui;

import javafx.scene.Node;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestInstance;
import net.highwayfrogs.editor.games.konami.greatquest.file.GreatQuestArchiveFile;
import net.highwayfrogs.editor.games.konami.greatquest.file.GreatQuestAssetBinFile;
import net.highwayfrogs.editor.games.konami.greatquest.file.GreatQuestGameFile;
import net.highwayfrogs.editor.gui.GameUIController;
import net.highwayfrogs.editor.gui.MainMenuController;
import net.highwayfrogs.editor.gui.components.CollectionEditorComponent;
import net.highwayfrogs.editor.gui.components.ProgressBarComponent;
import net.highwayfrogs.editor.utils.FXUtils;
import net.highwayfrogs.editor.utils.FileUtils;
import net.highwayfrogs.editor.utils.FileUtils.BrowserFileType;
import net.highwayfrogs.editor.utils.FileUtils.SavedFilePath;

import java.io.File;
import java.io.IOException;

/**
 * Represents the editor main menu for The Great Quest.
 * Created by Kneesnap on 4/14/2024.
 */
public class GreatQuestMainMenuUIController extends MainMenuController<GreatQuestInstance, GreatQuestGameFile> {
    public static final BrowserFileType ARCHIVE_FILE_TYPE = new BrowserFileType("Frogger The Great Quest Archive", "bin");
    public static final SavedFilePath OUTPUT_BIN_FILE_PATH = new SavedFilePath("outputDataBinFilePath", "Please select the file to save 'data.bin' as...", ARCHIVE_FILE_TYPE);
    public static final SavedFilePath FILE_EXPORT_FOLDER = new SavedFilePath("fullExportFolderPath", "Please select the folder to export game files into...");

    public GreatQuestMainMenuUIController(GreatQuestInstance instance) {
        super(instance);
    }

    @Override
    protected void onControllerLoad(Node rootNode) {
        super.onControllerLoad(rootNode);

        // Allow exporting MWI.
        addMenuItem(this.menuBarFile, "Export Files", () -> {
            File exportFolder = FileUtils.askUserToSelectFolder(getGameInstance(), FILE_EXPORT_FOLDER);
            if (exportFolder == null)
                return; // Cancel.

            File exportDir = new File(exportFolder, "Export");
            FileUtils.makeDirectory(exportDir);

            try {
                GreatQuestAssetBinFile.exportFileList(exportDir, getMainArchive());
            } catch (IOException ex) {
                handleError(ex, true, "Failed to save file list text file.");
            }

            getLogger().info("Attempting to export game files.");
            ProgressBarComponent.openProgressBarWindow(getGameInstance(), "File Export", progressBar -> {
                progressBar.setTotalProgress(getMainArchive().getFiles().size());

                for (GreatQuestArchiveFile file : getMainArchive().getFiles()) {
                    progressBar.setStatusMessage("Exporting '" + file.getDebugName() + "'");

                    try {
                        file.export(exportDir);
                    } catch (Exception ex) {
                        throw new RuntimeException("Failed to export the file '" + file.getDebugName() + "'.", ex);
                    }

                    progressBar.addCompletedProgress(1);
                }

                getLogger().info("Successfully exported all game files.");
            });
        });
    }

    @Override
    protected void saveMainGameData() {
        File outputFile = FileUtils.askUserToSaveFile(getGameInstance(), OUTPUT_BIN_FILE_PATH, "data.bin");
        if (outputFile != null)
            ProgressBarComponent.openProgressBarWindow(getGameInstance(), "Saving Files", progressBar -> getGameInstance().saveGame(outputFile, progressBar));
    }

    @Override
    protected CollectionEditorComponent<GreatQuestInstance, GreatQuestGameFile> createFileListEditor() {
        return new CollectionEditorComponent<>(getGameInstance(), new GreatQuestFileBasicListViewComponent(getGameInstance()), false);
    }

    protected static MenuItem addMenuItem(Menu menuBar, String title, Runnable action) {
        MenuItem menuItem = new MenuItem(title);
        menuItem.setOnAction(event -> FXUtils.reportErrorIfFails(action));
        menuBar.getItems().add(menuItem);
        return menuItem;
    }

    /**
     * Gets the main file archive.
     */
    public GreatQuestAssetBinFile getMainArchive() {
        return getGameInstance() != null ? getGameInstance().getMainArchive() : null;
    }

    /**
     * Shows the editor for the given file on the main menu.
     * @param file the file to show
     */
    public void showEditor(GreatQuestGameFile file) {
        GameUIController<?> controller = getCurrentEditor();
        if (controller != null && controller.trySetTargetFile(file))
            return;

        showEditor(file != null ? file.makeEditorUI() : null);
    }
}