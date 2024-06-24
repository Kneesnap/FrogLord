package net.highwayfrogs.editor.games.konami.greatquest.ui;

import javafx.scene.Node;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.file.writer.LargeFileReceiver;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestArchiveFile;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestAssetBinFile;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestInstance;
import net.highwayfrogs.editor.gui.MainMenuController;
import net.highwayfrogs.editor.gui.components.CollectionEditorComponent;
import net.highwayfrogs.editor.utils.Utils;

import java.io.File;
import java.io.IOException;

/**
 * Represents the editor main menu for The Great Quest.
 * Created by Kneesnap on 4/14/2024.
 */
public class GreatQuestMainMenuUIController extends MainMenuController<GreatQuestInstance, GreatQuestArchiveFile> {
    public GreatQuestMainMenuUIController(GreatQuestInstance instance) {
        super(instance);
    }

    @Override
    protected void onControllerLoad(Node rootNode) {
        super.onControllerLoad(rootNode);

        // Allow exporting MWI.
        addMenuItem(this.menuBarFile, "Export Files", () -> { // TODO: Make this use the progress bar later.
            File exportFolder = Utils.promptChooseDirectory(getGameInstance(), "Choose the folder to export files into...", true);
            if (exportFolder == null)
                return; // Cancel.

            File exportDir = new File(exportFolder, "Export");
            Utils.makeDirectory(exportDir);

            try {
                GreatQuestAssetBinFile.exportFileList(exportDir, getMainArchive());
            } catch (IOException ex) {
                handleError(ex, true, "Failed to save file list text file.");
            }

            int exportedFileCount = 0;
            for (GreatQuestArchiveFile file : getMainArchive().getFiles()) {
                getLogger().info("Exporting '" + file.getDebugName() + "'...  (" + (++exportedFileCount) + "/" + getMainArchive().getFiles().size() + ")");
                try {
                    file.export(exportDir);
                } catch (Exception ex) {
                    throw new RuntimeException("Failed to export the file '" + file.getDebugName() + "'.", ex);
                }
            }
        });

        /*
        TODO: Implement.
        addMenuItem(this.menuBarFile, "Import File", this::importFile); // Ctrl + I
        addMenuItem(this.menuBarFile, "Export File", this::exportFile); // Ctrl + O
        addMenuItem(this.menuBarFile, "Export File (Alternate Format)", () -> getSelectedFileEntry().exportAlternateFormat(getFileEntry())); // Ctrl + E
        addMenuItem(this.menuBarFile, "Export All Textures", this::exportBulkTextures);
        addMenuItem(this.menuBarEdit, "Open Hash Playground", () -> HashPlaygroundController.openEditor(getGameInstance()));
         */
    }

    @Override
    protected void saveMainGameData() {
        DataWriter writer = new DataWriter(new LargeFileReceiver(new File(new File(getGameInstance().getMainGameFolder(), "ModdedOutput/"), getGameInstance().getMainArchiveBinFile().getName())));
        getMainArchive().save(writer);
        writer.closeReceiver();
        // TODO: Progress bar.
    }

    @Override
    protected CollectionEditorComponent<GreatQuestInstance, GreatQuestArchiveFile> createFileListEditor() {
        return new CollectionEditorComponent<>(getGameInstance(), new GreatQuestFileBasicListViewComponent(getGameInstance()));
    }

    protected static MenuItem addMenuItem(Menu menuBar, String title, Runnable action) {
        MenuItem menuItem = new MenuItem(title);
        menuItem.setOnAction(event -> Utils.reportErrorIfFails(action));
        menuBar.getItems().add(menuItem);
        return menuItem;
    }

    /**
     * Gets the main file archive.
     */
    public GreatQuestAssetBinFile getMainArchive() {
        return getGameInstance() != null ? getGameInstance().getMainArchive() : null;
    }
}