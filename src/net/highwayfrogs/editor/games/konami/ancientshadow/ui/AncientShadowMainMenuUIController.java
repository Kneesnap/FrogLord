package net.highwayfrogs.editor.games.konami.ancientshadow.ui;

import javafx.scene.Node;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.file.writer.LargeFileReceiver;
import net.highwayfrogs.editor.games.konami.ancientshadow.AncientShadowGameFile;
import net.highwayfrogs.editor.games.konami.ancientshadow.AncientShadowInstance;
import net.highwayfrogs.editor.games.konami.ancientshadow.HFSFile;
import net.highwayfrogs.editor.games.konami.ancientshadow.file.AncientShadowRenderwareFile;
import net.highwayfrogs.editor.gui.MainMenuController;
import net.highwayfrogs.editor.gui.components.CollectionEditorComponent;
import net.highwayfrogs.editor.gui.components.ProgressBarComponent;
import net.highwayfrogs.editor.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manages the main menu UI for Frogger Ancient Shadow.
 * Created by Kneesnap on 8/4/2024.
 */
public class AncientShadowMainMenuUIController extends MainMenuController<AncientShadowInstance, AncientShadowGameFile> {
    public AncientShadowMainMenuUIController(AncientShadowInstance instance) {
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

            HFSFile hfsFile = getGameInstance().getMainHfs();
            File filesExportDir = new File(exportFolder, "Files [" + hfsFile.getDisplayName() + "]");
            Utils.makeDirectory(filesExportDir);

            for (int i = 0; i < hfsFile.getHfsFiles().size(); i++) {
                File groupFolder = new File(filesExportDir, "GROUP" + String.format("%02d", i));
                List<AncientShadowGameFile> groupFiles = hfsFile.getHfsFiles().get(i);
                Utils.makeDirectory(groupFolder);

                for (int j = 0; j < groupFiles.size(); j++) {
                    File outputFile = new File(groupFolder, "FILE" + String.format("%03d", j));

                    try {
                        Files.write(outputFile.toPath(), groupFiles.get(j).getRawData());
                    } catch (IOException ex) {
                        handleError(ex, false, "Failed to export file '%s'.", Utils.toLocalPath(exportFolder, outputFile, true));
                    }
                }
            }

            File imagesExportDir = new File(exportFolder, "Images [" + hfsFile.getDisplayName() + "]");
            Utils.makeDirectory(imagesExportDir);

            Map<String, AtomicInteger> nameCountMap = new HashMap<>();
            for (int i = 0; i < hfsFile.getHfsFiles().size(); i++) {
                File groupFolder = new File(filesExportDir, "GROUP" + String.format("%02d", i));
                List<AncientShadowGameFile> groupFiles = hfsFile.getHfsFiles().get(i);

                for (int j = 0; j < groupFiles.size(); j++) {
                    AncientShadowGameFile gameFile = groupFiles.get(j);
                    if (gameFile instanceof AncientShadowRenderwareFile)
                        ((AncientShadowRenderwareFile) gameFile).exportTextures(groupFolder, nameCountMap);
                }
            }

            getLogger().info("Export complete.");
        });

        /*
        TODO: Implement.
        addMenuItem(this.menuBarFile, "Import File", this::importFile); // Ctrl + I
        addMenuItem(this.menuBarFile, "Export File", this::exportFile); // Ctrl + O
         */
    }

    @Override
    protected void saveMainGameData() {
        ProgressBarComponent.openProgressBarWindow(getGameInstance(), "Saving Files",
                progressBar -> saveHfsFile(progressBar, getGameInstance().getMainHfsFile(), getMainHfs(), true));
    }

    private void saveHfsFile(ProgressBarComponent progressBar, File hfsFile, HFSFile loadedHfs, boolean overwriteOriginal) {
        File outputFile;
        if (overwriteOriginal) {
            outputFile = hfsFile;
        } else {
            outputFile = Utils.addFileNameSuffix(hfsFile, "-modified");
        }

        DataWriter writer = new DataWriter(new LargeFileReceiver(outputFile));
        loadedHfs.save(writer, progressBar);
        writer.closeReceiver();
    }

    @Override
    protected CollectionEditorComponent<AncientShadowInstance, AncientShadowGameFile> createFileListEditor() {
        return new CollectionEditorComponent<>(getGameInstance(), new AncientShadowFileBasicListViewComponent(getGameInstance()));
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
    public HFSFile getMainHfs() {
        return getGameInstance() != null ? getGameInstance().getMainHfs() : null;
    }
}