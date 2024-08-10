package net.highwayfrogs.editor.games.konami.hudson.ui;

import javafx.scene.Node;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.file.writer.FileReceiver;
import net.highwayfrogs.editor.file.writer.LargeFileReceiver;
import net.highwayfrogs.editor.games.konami.hudson.HudsonFileUserFSDefinition;
import net.highwayfrogs.editor.games.konami.hudson.HudsonGameFile;
import net.highwayfrogs.editor.games.konami.hudson.HudsonGameInstance;
import net.highwayfrogs.editor.gui.MainMenuController;
import net.highwayfrogs.editor.gui.components.CollectionEditorComponent;
import net.highwayfrogs.editor.gui.components.ProgressBarComponent;
import net.highwayfrogs.editor.utils.Utils;

import java.io.File;

/**
 * Manages the main menu UI for a Hudson game.
 * Created by Kneesnap on 8/8/2024.
 */
public class HudsonMainMenuUIController<TGameInstance extends HudsonGameInstance> extends MainMenuController<TGameInstance, HudsonGameFile> {
    public HudsonMainMenuUIController(TGameInstance instance) {
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

            try {
                for (HudsonGameFile file : getGameInstance().getFiles()) {
                    if (!(file.getFileDefinition() instanceof HudsonFileUserFSDefinition)) {
                        getLogger().severe("File '" + file.getDisplayName() + "' was defined by a(n) " + Utils.getSimpleName(file) + ", which should not have been possible.");
                        continue;
                    }

                    file.export(exportFolder);
                }

                getLogger().info("Export complete.");
            } catch (Throwable th) {
                Utils.handleError(getLogger(), th, true, "Failed to export files.");
            }
        });

        /*
        TODO: Implement.
        addMenuItem(this.menuBarFile, "Import File", this::importFile); // Ctrl + I
        addMenuItem(this.menuBarFile, "Export File", this::exportFile); // Ctrl + O
         */
    }

    @Override
    protected void saveMainGameData() {
        ProgressBarComponent.openProgressBarWindow(getGameInstance(), "Saving Files", progressBar -> {
            progressBar.update(0, getGameInstance().getFiles().size(), "");

            for (HudsonGameFile file : getGameInstance().getFiles()) {
                if (!(file.getFileDefinition() instanceof HudsonFileUserFSDefinition)) {
                    getLogger().severe("File '" + file.getDisplayName() + "' was defined by a(n) " + Utils.getSimpleName(file) + ", which should not have been possible.");
                    continue;
                }

                HudsonFileUserFSDefinition fileDefinition = (HudsonFileUserFSDefinition) file.getFileDefinition();
                progressBar.setStatusMessage("Saving '" + fileDefinition.getFileName() + "'");
                DataWriter writer = new DataWriter(fileDefinition.getFileName().equalsIgnoreCase("gamedata.bin") ? new LargeFileReceiver(fileDefinition.getFile()) : new FileReceiver(fileDefinition.getFile()));
                file.save(writer);
                writer.closeReceiver();

                progressBar.addCompletedProgress(1);
            }
        });
    }

    @Override
    protected CollectionEditorComponent<TGameInstance, HudsonGameFile> createFileListEditor() {
        return new CollectionEditorComponent<>(getGameInstance(), new HudsonFileBasicListViewComponent<>(getGameInstance()));
    }

    protected static MenuItem addMenuItem(Menu menuBar, String title, Runnable action) {
        MenuItem menuItem = new MenuItem(title);
        menuItem.setOnAction(event -> Utils.reportErrorIfFails(action));
        menuBar.getItems().add(menuItem);
        return menuItem;
    }
}